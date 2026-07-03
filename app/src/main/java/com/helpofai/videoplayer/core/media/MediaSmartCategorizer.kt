package com.helpofai.videoplayer.core.media

import java.util.Locale

object MediaSmartCategorizer {

    private val cameraRegex = Regex(".*\\d{8}_\\d{6}.*")
    private val tvShowRegex = Regex(".*(s\\d{1,2}e\\d{1,2}|season\\s*\\d+|episode\\s*\\d+|\\[\\d+x\\d+\\]).*")

    fun categorizeVideo(title: String, path: String, durationMs: Long): String {
        val lowerPath = path.lowercase(Locale.getDefault())
        val lowerTitle = title.lowercase(Locale.getDefault())
        val durationMinutes = durationMs / 1000 / 60

        // 1. Social Media
        if (lowerPath.contains("whatsapp video") || lowerPath.contains("telegram") || 
            lowerPath.contains("snapchat") || lowerPath.contains("instagram") || 
            lowerPath.contains("tiktok")) {
            return "Social Media"
        }

        // 2. Camera / Family Videos
        if (lowerPath.contains("dcim") || lowerPath.contains("camera") || 
            lowerTitle.startsWith("vid_") || lowerTitle.startsWith("img_") || 
            lowerTitle.startsWith("pxl_") || lowerTitle.matches(cameraRegex)) {
            return "Camera Videos"
        }

        // 3. Screen Recordings
        if (lowerPath.contains("screenrecord") || lowerPath.contains("captures") || 
            lowerTitle.contains("screen_recording") || lowerTitle.startsWith("record_")) {
            return "Screen Recordings"
        }
        
        // 4. Gameplay
        if (lowerPath.contains("nvidia") || lowerPath.contains("geforce") || 
            lowerPath.contains("xbox game bar") || lowerTitle.contains("gameplay") || 
            lowerTitle.contains("ps5") || lowerTitle.contains("ps4")) {
            return "Gameplay"
        }

        // 5. Tutorials & Lectures
        if (lowerPath.contains("course") || lowerPath.contains("tutorial") || 
            lowerPath.contains("udemy") || lowerPath.contains("coursera") || 
            lowerTitle.contains("tutorial") || lowerTitle.contains("lesson ") || 
            lowerTitle.contains("module ") || lowerTitle.contains("lecture ")) {
            return "Tutorials & Lectures"
        }

        // 6. Anime
        if (lowerPath.contains("anime") || lowerTitle.contains("[subsplease]") || 
            lowerTitle.contains("[erai-raws]") || lowerTitle.contains("[judas]") || 
            (lowerTitle.contains("[") && lowerTitle.contains("]") && lowerTitle.contains("ep"))) {
            return "Anime"
        }

        // 7. TV Shows
        if (lowerTitle.matches(tvShowRegex) || lowerPath.contains("tv shows") || lowerPath.contains("series")) {
            return "TV Shows"
        }

        // 8. Music Videos
        if (lowerPath.contains("music") || lowerPath.contains("audio") || 
            lowerTitle.contains("official video") || lowerTitle.contains("lyric video") || 
            lowerTitle.contains("vevo")) {
            return "Music Videos"
        }

        // 9. Movies (Long duration + quality tags, or specific folder)
        if (lowerPath.contains("movies") || 
            ((durationMinutes > 60) && (lowerTitle.contains("1080p") || lowerTitle.contains("720p") || 
            lowerTitle.contains("2160p") || lowerTitle.contains("bluray") || lowerTitle.contains("web-dl") || 
            lowerTitle.contains("brrip")))) {
            return "Movies"
        }

        // 10. Short Clips
        if (durationMinutes < 3) {
            return "Short Clips"
        }

        return "General"
    }
}
