package com.helpofai.videoplayer.feature.analysis

import com.helpofai.videoplayer.core.model.Video
import java.util.Calendar

data class HabitReport(
    val favoriteCategory: String,
    val averagePlaybackSpeed: Float,
    val subtitleUsagePercent: Int,
    val mostActiveTimeOfDay: String,
    val mostVisitedFolder: String
)

object HabitAnalyzer {
    fun analyze(videos: List<Video>): HabitReport {
        val playedVideos = videos.filter { it.playCount > 0 }
        
        if (playedVideos.isEmpty()) {
            return HabitReport("N/A", 1.0f, 0, "N/A", "N/A")
        }

        // 1. Favorite Category
        val favoriteCategory = playedVideos
            .groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.playCount } }
            ?.key ?: "General"
            
        // 2. Average Playback Speed
        val speedSum = playedVideos.sumOf { it.playbackSpeed.toDouble() }
        val avgSpeed = (speedSum / playedVideos.size).toFloat()
        
        // 3. Subtitle Usage
        val subtitleCount = playedVideos.count { it.subtitleTrackLanguage != null }
        val subtitlePercent = (subtitleCount * 100) / playedVideos.size
        
        // 4. Most Active Time Of Day (using lastPlayedTimestamp)
        val cal = Calendar.getInstance()
        val timeOfDayCounts = playedVideos.map {
            cal.timeInMillis = it.lastPlayedTimestamp
            cal.get(Calendar.HOUR_OF_DAY)
        }.groupBy { hour ->
            when (hour) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }
        }
        val mostActiveTime = timeOfDayCounts.maxByOrNull { it.value.size }?.key ?: "Evening"
        
        // 5. Most Visited Folder
        val mostVisitedFolder = playedVideos
            .groupBy { java.io.File(it.path).parentFile?.name ?: "Internal" }
            .maxByOrNull { entry -> entry.value.sumOf { it.playCount } }
            ?.key ?: "Internal"
            
        return HabitReport(
            favoriteCategory,
            avgSpeed,
            subtitlePercent,
            mostActiveTime,
            mostVisitedFolder
        )
    }
}
