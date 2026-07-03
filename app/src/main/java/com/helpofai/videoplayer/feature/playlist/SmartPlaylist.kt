package com.helpofai.videoplayer.feature.playlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.helpofai.videoplayer.core.model.Video

data class SmartPlaylist(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val videos: List<Video>,
    val accentColor: Color = Color(0xFF6C63FF)
)
