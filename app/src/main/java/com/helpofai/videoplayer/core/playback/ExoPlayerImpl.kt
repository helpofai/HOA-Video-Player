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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEffectManager: AudioEffectManager,
    private val videoEnhancementManager: com.helpofai.videoplayer.core.playback.diagnostics.VideoEnhancementManager
) : VideoPlayer {

    private var _player: ExoPlayer? = null

    /** Guard that prevents silent orphaned-player creation after release(). */
    override var isReleased = false
        private set

    override val player: Player
        get() {
            val existing = _player
            if (existing != null && !isReleased) {
                return existing
            }
            return initializePlayer()
        }

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var enhancementJob: Job? = null

    private var currentDecoderMode: String = "HW"

    private fun initializePlayer(): ExoPlayer {
        val renderersFactory = com.helpofai.videoplayer.feature.player.decoder.SmartDecoderEngine.getOptimalRenderersFactory(context, currentDecoderMode)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,     // minBufferMs (15 seconds)
                50_000,     // maxBufferMs (50 seconds)
                300,        // bufferForPlaybackMs (instant 300ms start)
                1_000       // bufferForPlaybackAfterRebufferMs (1 second)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val newPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .build(),
                true
            )
            val initialEffects = videoEnhancementManager.getMedia3Effects(videoEnhancementManager.config.value)
            if (initialEffects.isNotEmpty()) {
                setVideoEffects(initialEffects)
            }
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
                    val lower = decoderName.lowercase()
                    val isHw = !(lower.startsWith("omx.google.") || 
                                 lower.startsWith("c2.android.") || 
                                 lower.startsWith("omx.ffmpeg.") ||
                                 lower.contains("ffmpeg"))
                    val isFfmpeg = lower.contains("ffmpeg") || currentDecoderMode == "SW"
                    _playbackState.update { state ->
                        state.copy(
                            currentDecoderName = decoderName,
                            isHardwareDecoder = isHw,
                            isFFmpegActive = isFfmpeg || state.audioDecoderName.lowercase().contains("ffmpeg") || state.isFFmpegActive
                        ) 
                    }
                }

                override fun onAudioDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderName: String,
                    initializedDurationMs: Long,
                    initializationDurationMs: Long
                ) {
                    val lower = decoderName.lowercase()
                    val isFfmpeg = lower.contains("ffmpeg") || currentDecoderMode == "SW"
                    _playbackState.update { state ->
                        state.copy(
                            audioDecoderName = decoderName,
                            isFFmpegActive = isFfmpeg || state.currentDecoderName.lowercase().contains("ffmpeg") || currentDecoderMode == "SW"
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
                    val mime = format.sampleMimeType?.lowercase() ?: ""
                    // AC3, EAC3, DTS, TrueHD, FLAC often use bundled FFmpeg extension on Android
                    val isFfmpegFormat = mime.contains("ac3") || 
                                         mime.contains("eac3") || 
                                         mime.contains("dts") || 
                                         mime.contains("true-hd") || 
                                         mime.contains("mlp")
                    _playbackState.update { state ->
                        val isFfmpeg = state.isFFmpegActive || 
                                       isFfmpegFormat || 
                                       state.audioDecoderName.lowercase().contains("ffmpeg") || 
                                       state.currentDecoderName.lowercase().contains("ffmpeg") || 
                                       currentDecoderMode == "SW"
                        state.copy(
                            audioCodec = format.sampleMimeType ?: "Unknown",
                            isFFmpegActive = isFfmpeg
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
        isReleased = false
        startLiveEnhancementUpdates()
        return newPlayer
    }

    /**
     * LIVE ENHANCEMENT VIEW: watches the enhancement config and re-applies
     * Media3 effects to the running player the moment any slider changes.
     * collectLatest + trailing delay throttles rapid drag updates while
     * guaranteeing the newest slider value always wins.
     */
    private fun startLiveEnhancementUpdates() {
        enhancementJob?.cancel()
        enhancementJob = scope.launch {
            videoEnhancementManager.config
                .drop(1) // initial effects already applied synchronously at creation
                .distinctUntilChanged()
                .collectLatest { config ->
                    _player?.let { p ->
                        p.setVideoEffects(videoEnhancementManager.getMedia3Effects(config))
                    }
                    delay(120) // throttle: a newer slider value cancels this and applies latest
                }
        }
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
                            bufferedPosition = p.bufferedPosition,
                            playbackSpeed = p.playbackParameters.speed
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
        val currentUri = currentPlayer.currentMediaItem?.localConfiguration?.uri
        val newUri = mediaItem.localConfiguration?.uri
        if (currentUri != null && currentUri == newUri) {
            currentPlayer.playWhenReady = playWhenReady
            return
        }
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
        enhancementJob?.cancel()
        enhancementJob = null
        audioEffectManager.release()
        _player?.release()
        _player = null
        isReleased = true
    }
    
    override fun setPlaybackSpeed(speed: Float) {
        _player?.playbackParameters = PlaybackParameters(speed)
    }

    override fun switchDecoder(mode: String) {
        if (currentDecoderMode.equals(mode, ignoreCase = true) && _player != null) return
        currentDecoderMode = mode
        val currentP = _player ?: return
        val currentMedia = currentP.currentMediaItem ?: return
        val currentPos = currentP.currentPosition
        val wasPlaying = currentP.isPlaying
        val currentSpeed = currentP.playbackParameters.speed
        val currentVol = currentP.volume

        stopProgressUpdate()
        enhancementJob?.cancel()
        enhancementJob = null
        currentP.stop()
        currentP.release()
        _player = null

        val newPlayer = initializePlayer()
        newPlayer.setMediaItem(currentMedia)
        newPlayer.prepare()
        newPlayer.seekTo(currentPos)
        newPlayer.playbackParameters = PlaybackParameters(currentSpeed)
        newPlayer.volume = currentVol
        newPlayer.playWhenReady = wasPlaying
    }
}
