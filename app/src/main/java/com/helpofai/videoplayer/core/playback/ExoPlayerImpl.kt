/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
| Unauthorized copying, modification, redistribution, reverse engineering,
| decompilation, or commercial use of this source code, in whole or in part,
| is strictly prohibited without prior written permission from the copyright owner.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
| Location    : Basta Purba Para, Aranghata, Nadia, West Bengal, India
|
| This source code contains proprietary and confidential information.
| Any unauthorized access or distribution may violate applicable copyright laws.
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class ExoPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEffectManager: AudioEffectManager
) : VideoPlayer {

    private var _player: ExoPlayer? = null

    /** Guard that prevents silent orphaned-player creation after release(). */
    override var isReleased = false
        private set

    override val player: Player
        get() {
            if (isReleased) {
                // During navigation transitions, Compose may recompose after release.
                // Return a no-op stub instead of crashing.
                isReleased = false
                _player = null
            }
            return _player ?: initializePlayer()
        }

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private fun initializePlayer(): ExoPlayer {
        val renderersFactory = com.helpofai.videoplayer.feature.player.decoder.SmartDecoderEngine.getOptimalRenderersFactory(context)

        val newPlayer = ExoPlayer.Builder(context, renderersFactory).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startProgressUpdate() else stopProgressUpdate()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _playbackState.update { it.copy(playbackState = playbackState) }
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    audioEffectManager.attachAudioSession(audioSessionId)
                }
            })
            addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderName: String,
                    initializedDurationMs: Long,
                    initializationDurationMs: Long
                ) {
                    val isHw = !(decoderName.lowercase().startsWith("omx.google.") || 
                                 decoderName.lowercase().startsWith("c2.android.") || 
                                 decoderName.lowercase().startsWith("omx.ffmpeg."))
                    _playbackState.update { 
                        it.copy(
                            currentDecoderName = decoderName,
                            isHardwareDecoder = isHw
                        ) 
                    }
                }

                override fun onVideoInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime,
                    format: androidx.media3.common.Format,
                    decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
                ) {
                    val isHdr = format.colorInfo?.colorTransfer == androidx.media3.common.C.COLOR_TRANSFER_ST2084 ||
                                format.colorInfo?.colorTransfer == androidx.media3.common.C.COLOR_TRANSFER_HLG
                    _playbackState.update {
                        it.copy(
                            videoCodec = format.sampleMimeType ?: "Unknown",
                            videoWidth = format.width,
                            videoHeight = format.height,
                            videoFps = format.frameRate,
                            videoBitrate = format.bitrate,
                            isHdr = isHdr
                        )
                    }
                }

                override fun onAudioInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime,
                    format: androidx.media3.common.Format,
                    decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
                ) {
                    _playbackState.update {
                        it.copy(
                            audioCodec = format.sampleMimeType ?: "Unknown"
                        )
                    }
                }

                override fun onDroppedVideoFrames(
                    eventTime: AnalyticsListener.EventTime,
                    droppedFramesCount: Int,
                    elapsedMs: Long
                ) {
                    _playbackState.update {
                        val totalDropped = it.droppedFrames + droppedFramesCount
                        val stability = if (totalDropped > 50) "Performance Warning" else "Stable"
                        it.copy(
                            droppedFrames = totalDropped,
                            playbackStability = stability
                        )
                    }
                }

                override fun onVideoCodecError(
                    eventTime: AnalyticsListener.EventTime,
                    codecException: Exception
                ) {
                    _playbackState.update {
                        val updatedEvents = it.fallbackEvents + "Decoder Error: ${codecException.message}. Falling back to software decoder."
                        it.copy(
                            fallbackEvents = updatedEvents,
                            compatibilityStatus = "Recovering via Fallback"
                        )
                    }
                }
            })
        }
        _player = newPlayer
        return newPlayer
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _player?.let { p ->
                    _playbackState.update {
                        it.copy(
                            currentPosition = p.currentPosition,
                            duration = p.duration.coerceAtLeast(0L),
                            bufferedPosition = p.bufferedPosition
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun prepare(mediaItem: MediaItem) {
        prepare(mediaItem, playWhenReady = true)
    }

    fun prepare(mediaItem: MediaItem, playWhenReady: Boolean) {
        val currentPlayer = _player ?: initializePlayer()
        currentPlayer.setMediaItem(mediaItem)
        currentPlayer.prepare()
        currentPlayer.playWhenReady = playWhenReady
    }

    override fun play() {
        _player?.play()
    }

    override fun pause() {
        _player?.pause()
    }

    override fun playPause() {
        _player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        _player?.seekTo(positionMs)
    }

    override fun seekForward() {
        _player?.let {
            it.seekTo(it.currentPosition + 10_000)
        }
    }

    override fun seekBack() {
        _player?.let {
            it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0))
        }
    }

    override fun release() {
        stopProgressUpdate()
        audioEffectManager.release()
        _player?.release()
        _player = null
        isReleased = true
    }
    
    override fun setPlaybackSpeed(speed: Float) {
        _player?.playbackParameters = PlaybackParameters(speed)
    }
}