package com.helpofai.videoplayer.core.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

interface VideoPlayer {
    val player: Player
    val playbackState: StateFlow<PlaybackState>

    fun prepare(mediaItem: MediaItem)
    fun play()
    fun pause()
    fun playPause()
    fun seekTo(positionMs: Long)
    fun seekForward()
    fun seekBack()
    fun release()
    fun setPlaybackSpeed(speed: Float)
}
