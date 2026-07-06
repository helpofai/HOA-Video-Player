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


    // 1. Hero Section (Auto & Manual Sliding)
    val heroVideos = androidx.compose.runtime.remember(state.videos) {
        val longVideos = state.videos.filter { it.duration >= 30 * 60 * 1000L }
        if (longVideos.size > 5) longVideos.shuffled().take(5) else longVideos
    }

    if (heroVideos.isNotEmpty()) {
        InteractiveCardHero(
            videos = heroVideos,
            onVideoClick = onVideoClick,
            onFavoriteClick = onFavoriteClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        AdAfterHero()
    }

    // 1.5 Continue Watching (Smart Resume)
    val continueWatching = state.videos.filter { it.lastPlayedPosition > 0 && it.duration > 0 && it.lastPlayedPosition < it.duration - 5000 }.sortedByDescending { it.lastPlayedTimestamp }.take(5)
    if (continueWatching.isNotEmpty()) {
        LibrarySectionTitle("Continue Watching")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(continueWatching) { video ->
                ContinueWatchingCard(
                    video = video,
                    onClick = { onVideoClick(video) }
                )
            }
        }
        AdAfterContinueWatching()
    }

    // 1.8 Recommended For You (Newest Folder)
    val latestVideo = state.videos.maxByOrNull { it.dateAdded }
    if (latestVideo != null) {
        val recommendedFolder = java.io.File(latestVideo.path).parentFile?.name ?: "Internal Storage"
        val recommendations = state.videos.filter { (java.io.File(it.path).parentFile?.name ?: "Internal Storage") == recommendedFolder }.sortedByDescending { it.dateAdded }
        
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
            HomeNativeAd()
        }
    }

    // 2. All Videos / Recently Added
    val recentlyAdded = state.videos.sortedByDescending { it.dateAdded }.take(10)
    if (recentlyAdded.isNotEmpty()) {
        LibrarySectionTitle("Recently Added")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recentlyAdded) { video ->
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
        HomeBannerAd()
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
        AdAfterFavorites()
    }

    // 4. Resume Playback
    val legacyContinueWatching = state.videos.filter { it.lastPlayedPosition > 0 }.sortedByDescending { it.lastPlayedPosition }
    if (legacyContinueWatching.isNotEmpty()) {
        LibrarySectionTitle("Resume Playback")
        val listCols = if (isTablet) 2 else 1
        val chunkedContinue = legacyContinueWatching.take(if (isTablet) 4 else 3).chunked(listCols)

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
        AdAfterResumePlayback()
    }

    // 5. Large Files
    val largeFiles = state.videos.sortedByDescending { it.size }.take(10)
    if (largeFiles.isNotEmpty()) {
        LibrarySectionTitle("Large Files")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(largeFiles) { video ->
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
        AdAfterLargeFiles()
    }

    // 6. Short Clips
    val shortClips = state.videos.filter { it.duration in 1..60000 }.take(15)
    if (shortClips.isNotEmpty()) {
        LibrarySectionTitle("Short Clips")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shortClips) { video ->
                LibraryFavoriteVideoCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onFavoriteClick = { onFavoriteClick(video) }
                )
            }
        }
        AdAfterShortClips()
    }

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
