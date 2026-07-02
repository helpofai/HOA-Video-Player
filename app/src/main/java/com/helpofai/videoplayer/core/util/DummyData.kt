package com.helpofai.videoplayer.core.util

data class VideoItem(
    val title: String,
    val duration: String,
    val size: String,
    val progress: Float = 0f
)

val dummyVideos = listOf(
    VideoItem("Nature Documentary 4K", "45:20", "1.2 GB", 0.6f),
    VideoItem("Cyberpunk Cinematic Movie", "02:15:30", "4.5 GB", 0.1f),
    VideoItem("Family Vacation 2026", "12:05", "300 MB", 0f),
    VideoItem("Android Compose Tutorial", "25:10", "800 MB", 1f),
    VideoItem("Gaming Montage", "08:45", "150 MB", 0f)
)
