package com.helpofai.videoplayer.core.playback

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackState: Int = 1 // Player.STATE_IDLE
)
