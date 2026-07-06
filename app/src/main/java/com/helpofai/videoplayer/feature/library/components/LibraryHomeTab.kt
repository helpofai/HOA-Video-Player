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
package com.helpofai.videoplayer.feature.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.library.LibraryState
import com.helpofai.videoplayer.feature.library.ads.*
import com.helpofai.videoplayer.feature.playlist.SmartPlaylistEngine

@Composable
fun LibraryHomeTab(
    state: LibraryState,
    isTablet: Boolean,
    onVideoClick: (Video) -> Unit,
    onFavoriteClick: (Video) -> Unit,
    onRenameClick: (Video) -> Unit,
    onDeleteClick: (Video) -> Unit,
    onShareClick: (Video) -> Unit,
    onNavigateToPlaylists: (String?) -> Unit
) {

    // 1. Premium Slider Section (Lightweight & High Performance)
    val sliderVideos = androidx.compose.runtime.remember(state.videos) {
        val longVideos = state.videos.filter { it.duration >= 40 * 60 * 1000L }
        val finalVideos = if (longVideos.isNotEmpty()) longVideos else state.videos
        finalVideos.shuffled()
    }

    if (sliderVideos.isNotEmpty()) {
        PremiumVideoSlider(
            videos = sliderVideos,
            onVideoClick = onVideoClick
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // 1.5 Resume Playback (Folder Context)
    val resumeData = androidx.compose.runtime.remember(state.videos) {
        val lastPlayedVideo = state.videos.filter { it.lastPlayedPosition > 0 }.maxByOrNull { it.lastPlayedTimestamp }
        if (lastPlayedVideo != null) {
            val folder = java.io.File(lastPlayedVideo.path).parentFile?.name ?: "Internal Storage"
            val videos = state.videos.filter { 
                (java.io.File(it.path).parentFile?.name ?: "Internal Storage") == folder 
            }.sortedByDescending { it.lastPlayedTimestamp }.take(5)
            Pair(folder, videos)
        } else null
    }

    if (resumeData != null && resumeData.second.isNotEmpty()) {
        val resumeFolder = resumeData.first
        val resumeVideos = resumeData.second

        LibrarySectionTitle("Resume Playback")
        androidx.compose.material3.Text(
            text = "From $resumeFolder",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
        )
        val listCols = if (isTablet) 2 else 1
        val chunkedContinue = resumeVideos.chunked(listCols)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chunkedContinue.forEach { rowVideos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowVideos.forEach { video ->
                        Box(modifier = Modifier.weight(1f)) {
                            LibraryCompactVideoListItem(
                                video = video,
                                onClick = { onVideoClick(video) },
                                onFavoriteClick = { onFavoriteClick(video) },
                                onRenameClick = { onRenameClick(video) },
                                onDeleteClick = { onDeleteClick(video) },
                                onShareClick = { onShareClick(video) }
                            )
                        }
                    }
                    val emptySlots = listCols - rowVideos.size
                    for (i in 0 until emptySlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        // Removed AdAfterResumePlayback() to save RAM and improve performance
    }

    // 1.8 Recommended For You (Newest Folder)
    val recommendationsData = androidx.compose.runtime.remember(state.videos) {
        val latestVideo = state.videos.maxByOrNull { it.dateAdded }
        if (latestVideo != null) {
            val folder = java.io.File(latestVideo.path).parentFile?.name ?: "Internal Storage"
            val videos = state.videos.filter { 
                (java.io.File(it.path).parentFile?.name ?: "Internal Storage") == folder 
            }.sortedByDescending { it.dateAdded }
            Pair(folder, videos)
        } else null
    }

    if (recommendationsData != null && recommendationsData.second.isNotEmpty()) {
        val recommendedFolder = recommendationsData.first
        val recommendations = recommendationsData.second
        
        if (recommendations.isNotEmpty()) {
            LibrarySectionTitle("Recommended For You")
            androidx.compose.material3.Text(
                text = "From $recommendedFolder",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recommendations) { video ->
                    LibraryVideoInfoCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onFavoriteClick = { onFavoriteClick(video) },
                        onRenameClick = { onRenameClick(video) },
                        onDeleteClick = { onDeleteClick(video) },
                        onShareClick = { onShareClick(video) }
                    )
                }
            }
            // Removed HomeNativeAd() to save RAM and improve performance
        }
    }



    // 3. Favorites
    val favorites = state.videos.filter { it.isFavorite }
    if (favorites.isNotEmpty()) {
        LibrarySectionTitle("Favorites")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites) { video ->
                LibraryFavoriteVideoCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onFavoriteClick = { onFavoriteClick(video) }
                )
            }
        }
        // Removed AdAfterFavorites() to save RAM and improve performance
    }

    // Sections reorganized.



    // 7. Smart Playlists
    LibrarySectionTitle("Smart Playlists")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LibraryCollectionChip("All Playlists", "Browse all", modifier = Modifier.width(140.dp), onClick = {
                onNavigateToPlaylists(null)
            })
        }
        val homePlaylists = SmartPlaylistEngine.generatePlaylists(state.videos)
        homePlaylists.forEach { playlist ->
            item {
                LibraryCollectionChip(playlist.title, "${playlist.videos.size} items", modifier = Modifier.width(160.dp), onClick = {
                    onNavigateToPlaylists(playlist.id)
                })
            }
        }
    }
    AdAfterSmartPlaylists()
    Spacer(modifier = Modifier.height(32.dp))
}
