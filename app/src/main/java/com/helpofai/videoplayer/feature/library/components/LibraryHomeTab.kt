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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.theme.ToolIconPalette
import com.helpofai.videoplayer.core.theme.frostedGlass
import com.helpofai.videoplayer.core.ui.MediaSelectionState
import com.helpofai.videoplayer.feature.library.LibraryState
import com.helpofai.videoplayer.feature.library.ads.InlineItemAd
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import kotlin.math.abs

/**
 * High-performance virtualized Home tab powered by [LazyColumn].
 * Recycles views during scroll for buttery smooth 60/120fps responsiveness.
 */
@Composable
fun LibraryHomeTab(
    state: LibraryState,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onVideoClick: (Video) -> Unit,
    onFavoriteClick: (Video) -> Unit,
    onRenameClick: (Video) -> Unit,
    onDeleteClick: (Video) -> Unit,
    onShareClick: (Video) -> Unit,
    onVaultClick: (Video) -> Unit,
    selectionState: MediaSelectionState? = null,
    onVideoLongClick: ((Video) -> Unit)? = null
) {
    val sliderVideos = remember(state.videos) {
        val longVideos = state.videos.filter { it.duration >= 40 * 60 * 1000L }
        val finalVideos = if (longVideos.isNotEmpty()) longVideos else state.videos
        finalVideos.take(6)
    }

    val sessionManager = remember { WatchPartySessionManager.getInstance() }
    val activeSession by sessionManager.activeSession.collectAsState()
    val isFullPlayerActive by sessionManager.isFullPlayerActive.collectAsState()

    val allFolders = remember(state.videos) { state.videos.groupBy { it.resolvedFolderName } }

    val resumeData = remember(state.videos) {
        val lastPlayedVideo = state.videos.filter { it.lastPlayedPosition > 0 }.maxByOrNull { it.lastPlayedTimestamp }
        if (lastPlayedVideo != null) {
            val folder = lastPlayedVideo.resolvedFolderName
            val videos = state.videos.filter {
                it.resolvedFolderName == folder
            }.sortedByDescending { it.lastPlayedTimestamp }.take(6)
            Pair(folder, videos)
        } else null
    }

    val recommendationsData = remember(state.videos) {
        val latestVideo = state.videos.maxByOrNull { it.dateAdded }
        if (latestVideo != null) {
            val folder = latestVideo.resolvedFolderName
            val videos = state.videos.filter {
                it.resolvedFolderName == folder
            }.sortedByDescending { it.dateAdded }
            Pair(folder, videos)
        } else null
    }

    val favorites = remember(state.videos) { state.videos.filter { it.isFavorite } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Premium Slider Section
        if (sliderVideos.isNotEmpty()) {
            item(key = "home_slider") {
                PremiumVideoSlider(
                    videos = sliderVideos,
                    onVideoClick = onVideoClick
                )
            }
        }

        // 2. Active Streaming Watch Party Session Preview
        if (activeSession?.video != null) {
            item(key = "home_watch_party_preview") {
                WatchPartySessionPreviewCard(
                    session = activeSession!!,
                    isHost = !sessionManager.isClientMode,
                    isFullPlayerActive = isFullPlayerActive,
                    onVideoClick = onVideoClick
                )
            }
        }

        // 3. Smart Playlists Carousel
        item(key = "home_smart_playlists") {
            Column {
                LibrarySectionTitle("Smart Playlists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LibraryCollectionChip(
                            title = "Downloads",
                            subtitle = "${allFolders.entries.find { it.key.contains("Download", ignoreCase = true) }?.value?.size ?: 0} Videos",
                            modifier = Modifier.width(140.dp),
                            onClick = { }
                        )
                    }
                    item {
                        LibraryCollectionChip(
                            title = "Camera",
                            subtitle = "${allFolders.entries.find { it.key.contains("Camera", ignoreCase = true) }?.value?.size ?: 0} Videos",
                            modifier = Modifier.width(140.dp),
                            onClick = { }
                        )
                    }
                    item {
                        LibraryCollectionChip(
                            title = "Favorites",
                            subtitle = "${state.videos.count { it.isFavorite }} Videos",
                            modifier = Modifier.width(140.dp),
                            onClick = { }
                        )
                    }
                    item {
                        LibraryCollectionChip(
                            title = "Recent",
                            subtitle = "Last 30 Days",
                            modifier = Modifier.width(140.dp),
                            onClick = { }
                        )
                    }
                    item {
                        InlineItemAd(itemIndex = 4, adInterval = 4, nativeEvery = 4, bannerEvery = 0)
                    }
                }
            }
        }

        // 4. Resume Playback Section
        if (resumeData != null && resumeData.second.isNotEmpty()) {
            val resumeFolder = resumeData.first
            val resumeVideos = resumeData.second
            item(key = "home_resume_title") {
                Column {
                    LibrarySectionTitle("Resume Playback")
                    Text(
                        text = "From $resumeFolder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                    )
                }
            }

            val listCols = if (isTablet) 2 else 1
            val chunkedContinue = resumeVideos.chunked(listCols)
            itemsIndexed(chunkedContinue, key = { idx, _ -> "home_resume_row_$idx" }) { _, rowVideos ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowVideos.forEach { video ->
                        Box(modifier = Modifier.weight(1f)) {
                            LibraryCompactVideoListItem(
                                video = video,
                                onClick = {
                                    if (selectionState?.isSelectionMode == true) {
                                        selectionState.toggleVideo(video)
                                    } else {
                                        onVideoClick(video)
                                    }
                                },
                                onLongClick = { onVideoLongClick?.invoke(video) },
                                isSelected = selectionState?.isVideoSelected(video) == true,
                                isSelectionMode = selectionState?.isSelectionMode == true,
                                onFavoriteClick = { onFavoriteClick(video) },
                                onRenameClick = { onRenameClick(video) },
                                onDeleteClick = { onDeleteClick(video) },
                                onShareClick = { onShareClick(video) },
                                onVaultClick = { onVaultClick(video) }
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

        // 5. Recommended For You Section
        if (recommendationsData != null && recommendationsData.second.isNotEmpty()) {
            val recommendedFolder = recommendationsData.first
            val recommendations = recommendationsData.second
            item(key = "home_recommended") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = ToolIconPalette.Folders,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = recommendedFolder,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recommendations, key = { it.id }) { video ->
                            LibraryVideoInfoCard(
                                video = video,
                                onClick = {
                                    if (selectionState?.isSelectionMode == true) {
                                        selectionState.toggleVideo(video)
                                    } else {
                                        onVideoClick(video)
                                    }
                                },
                                onLongClick = { onVideoLongClick?.invoke(video) },
                                isSelected = selectionState?.isVideoSelected(video) == true,
                                isSelectionMode = selectionState?.isSelectionMode == true,
                                onFavoriteClick = { onFavoriteClick(video) },
                                onRenameClick = { onRenameClick(video) },
                                onDeleteClick = { onDeleteClick(video) },
                                onShareClick = { onShareClick(video) },
                                onVaultClick = { onVaultClick(video) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Favorites Carousel
        if (favorites.isNotEmpty()) {
            item(key = "home_favorites") {
                Column {
                    LibrarySectionTitle("Favorites")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favorites, key = { it.id }) { video ->
                            LibraryFavoriteVideoCard(
                                video = video,
                                onClick = {
                                    if (selectionState?.isSelectionMode == true) {
                                        selectionState.toggleVideo(video)
                                    } else {
                                        onVideoClick(video)
                                    }
                                },
                                onLongClick = { onVideoLongClick?.invoke(video) },
                                isSelected = selectionState?.isVideoSelected(video) == true,
                                isSelectionMode = selectionState?.isSelectionMode == true,
                                onFavoriteClick = { onFavoriteClick(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extracted standalone watch party live session preview card.
 */
@Composable
private fun WatchPartySessionPreviewCard(
    session: WatchPartySession,
    isHost: Boolean,
    isFullPlayerActive: Boolean,
    onVideoClick: (Video) -> Unit
) {
    val video = session.video ?: return
    val labelText = if (isHost) "HOSTING STREAM" else "LIVE WATCH PARTY"
    val labelColor = if (isHost) Color(0xFF7C5CE7) else Color(0xFFE74C3C)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var previewPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(video.id, isLifecycleResumed, isFullPlayerActive) {
        if (isLifecycleResumed && !isFullPlayerActive) {
            val player = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f
                playWhenReady = session.isPlaying
            }

            val videoUri = if (session.hostIp.isNotBlank() && session.hostIp != "127.0.0.1" && !isHost) {
                android.net.Uri.parse("http://${session.hostIp}:${session.port}/video?t=${System.currentTimeMillis()}")
            } else {
                video.uri
            }

            player.setMediaItem(MediaItem.fromUri(videoUri))
            player.prepare()
            if (session.currentPositionMs > 0L) {
                player.seekTo(session.currentPositionMs)
            }
            previewPlayer = player
        }
        onDispose {
            previewPlayer?.apply {
                playWhenReady = false
                stop()
                release()
            }
            previewPlayer = null
        }
    }

    LaunchedEffect(session.isPlaying, session.currentPositionMs) {
        previewPlayer?.let { player ->
            player.playWhenReady = session.isPlaying
            if (abs(player.currentPosition - session.currentPositionMs) > 2000L) {
                player.seekTo(session.currentPositionMs)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .frostedGlass(cornerRadius = 14.dp, surfaceAlpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelText,
                    color = labelColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(labelColor.copy(alpha = alpha))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (previewPlayer != null && !isFullPlayerActive) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                player = previewPlayer
                                layoutParams = android.widget.FrameLayout.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.player = previewPlayer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2C3E50), Color(0xFF3498DB))
                                )
                            )
                    )
                }
            }

            Text(
                text = video.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Room Name: ${session.name}",
                color = Color(0xFF8E9CB0),
                fontSize = 11.sp
            )

            val correctedVideo = if (!isHost) {
                val streamUri = android.net.Uri.parse("http://${session.hostIp}:${session.port}/video?t=${System.currentTimeMillis()}")
                video.copy(uri = streamUri, path = "http_stream")
            } else {
                video
            }

            Button(
                onClick = { onVideoClick(correctedVideo) },
                colors = ButtonDefaults.buttonColors(containerColor = labelColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Text(
                    text = "Join Stream in Full Player",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}