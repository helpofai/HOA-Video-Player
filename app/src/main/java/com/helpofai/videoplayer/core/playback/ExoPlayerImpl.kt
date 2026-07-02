package com.helpofai.videoplayer.core.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    
    override val player: Player
        get() = _player ?: initializePlayer()

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private fun initializePlayer(): ExoPlayer {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true) // Enables software decoding fallback if hardware fails

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
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun prepare(mediaItem: MediaItem) {
        val currentPlayer = _player ?: initializePlayer()
        currentPlayer.setMediaItem(mediaItem)
        currentPlayer.prepare()
        currentPlayer.playWhenReady = true
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
    }
    
    override fun setPlaybackSpeed(speed: Float) {
        _player?.playbackParameters = PlaybackParameters(speed)
    }
}
