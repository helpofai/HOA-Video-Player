package com.helpofai.videoplayer.feature.player.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FeedbackType {
    PLAY_PAUSE, SEEK, BRIGHTNESS, VOLUME, SPEED, ZOOM, LOCK, INFO
}

data class FeedbackEvent(
    val type: FeedbackType,
    val icon: ImageVector,
    val text: String,
    val value: Float? = null,
    val color: Color = Color.White,
    val id: Long = System.currentTimeMillis()
)
