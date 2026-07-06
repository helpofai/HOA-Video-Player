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
package com.helpofai.videoplayer.feature.playlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.helpofai.videoplayer.core.model.Video
import java.util.Locale

object SmartPlaylistEngine {

    fun generatePlaylists(allVideos: List<Video>): List<SmartPlaylist> {
        val playlists = mutableListOf<SmartPlaylist>()

        // 1. Recently Added
        val recentlyAdded = allVideos.sortedByDescending { it.dateAdded }.take(20)
        if (recentlyAdded.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "recently_added",
                    title = "Recently Added",
                    description = "Latest videos you've added",
                    icon = Icons.Default.NewReleases,
                    videos = recentlyAdded,
                    accentColor = Color(0xFF00BCD4)
                )
            )
        }

        // 2. Continue Watching
        val continueWatching = allVideos
            .filter { it.lastPlayedPosition > 0 && it.duration > 0 && it.lastPlayedPosition < it.duration - 5000 }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(15)
        if (continueWatching.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "continue_watching",
                    title = "Continue Watching",
                    description = "Pick up where you left off",
                    icon = Icons.Default.PlayCircleOutline,
                    videos = continueWatching,
                    accentColor = Color(0xFFFF6B35)
                )
            )
        }

        // 3. Favorites
        val favorites = allVideos.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "favorites",
                    title = "Favorites",
                    description = "Your favorite moments",
                    icon = Icons.Default.Favorite,
                    videos = favorites,
                    accentColor = Color(0xFFE91E63)
                )
            )
        }

        // 4. Kids & Family
        val kidsKeywords = listOf("kid", "cartoon", "animation", "disney", "peppa", "cocomelon", "nursery")
        val kidsVideos = allVideos.filter { video ->
            val lowerTitle = video.title.lowercase(Locale.getDefault())
            val lowerPath = video.path.lowercase(Locale.getDefault())
            kidsKeywords.any { lowerTitle.contains(it) || lowerPath.contains(it) }
        }
        if (kidsVideos.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "kids",
                    title = "Kids & Family",
                    description = "Cartoons and kids shows",
                    icon = Icons.Default.ChildCare,
                    videos = kidsVideos,
                    accentColor = Color(0xFFFFC107)
                )
            )
        }

        // 5. Travel & Adventures
        val travelKeywords = listOf("travel", "vlog", "trip", "tour", "vacation", "holiday", "gopro", "drone")
        val travelVideos = allVideos.filter { video ->
            val lowerTitle = video.title.lowercase(Locale.getDefault())
            val lowerPath = video.path.lowercase(Locale.getDefault())
            travelKeywords.any { lowerTitle.contains(it) || lowerPath.contains(it) }
        }
        if (travelVideos.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "travel",
                    title = "Travel & Adventures",
                    description = "Your memories around the world",
                    icon = Icons.Default.FlightTakeoff,
                    videos = travelVideos,
                    accentColor = Color(0xFF4CAF50)
                )
            )
        }

        // 6. Music
        val musicKeywords = listOf("music", "song", "cover", "live", "concert", "official video", "lyric")
        val musicVideos = allVideos.filter { video ->
            val lowerTitle = video.title.lowercase(Locale.getDefault())
            val lowerPath = video.path.lowercase(Locale.getDefault())
            musicKeywords.any { lowerTitle.contains(it) || lowerPath.contains(it) } || video.category == "Music Videos"
        }
        if (musicVideos.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "music",
                    title = "Music",
                    description = "Music videos and concerts",
                    icon = Icons.Default.MusicNote,
                    videos = musicVideos,
                    accentColor = Color(0xFF9C27B0)
                )
            )
        }

        // 7. High Quality (Movies & 4K)
        val hqVideos = allVideos.filter { video ->
            val lowerTitle = video.title.lowercase(Locale.getDefault())
            val is4k = lowerTitle.contains("4k") || lowerTitle.contains("2160p") || lowerTitle.contains("uhd")
            val isHdr = lowerTitle.contains("hdr") || lowerTitle.contains("dolby") || lowerTitle.contains("dv")
            is4k || isHdr || video.category == "Movies"
        }
        if (hqVideos.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "high_quality",
                    title = "High Quality & Movies",
                    description = "4K, HDR, and feature films",
                    icon = Icons.Default.HighQuality,
                    videos = hqVideos,
                    accentColor = Color(0xFFFFD700)
                )
            )
        }

        // 8. Short Videos
        val shortVideos = allVideos.filter { it.duration in 1..60000 } // Less than 1 minute
        if (shortVideos.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "shorts",
                    title = "Short Clips",
                    description = "Quick moments and shorts",
                    icon = Icons.Default.Timer,
                    videos = shortVideos,
                    accentColor = Color(0xFF03A9F4)
                )
            )
        }

        return playlists
    }
}
