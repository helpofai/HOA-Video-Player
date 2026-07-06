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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.helpofai.videoplayer.core.model.Video

/**
 * 2×2 mosaic folder card shown in the Folders tab grid.
 * Displays the first 4 video thumbnails, folder name, video count, and a "NEW ITEMS" badge
 * when the folder contains unwatched videos.
 */
@Composable
fun LibraryFolderCard(
    folderName: String,
    videos: List<Video>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 2×2 thumbnail mosaic
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    val v1 = videos.getOrNull(0)
                    val v2 = videos.getOrNull(1)
                    if (v1 != null) LibraryFolderThumbnail(v1, Modifier.weight(1f).fillMaxHeight())
                    if (v2 != null) LibraryFolderThumbnail(v2, Modifier.weight(1f).fillMaxHeight())
                }
                Row(modifier = Modifier.weight(1f)) {
                    val v3 = videos.getOrNull(2)
                    val v4 = videos.getOrNull(3)
                    if (v3 != null) LibraryFolderThumbnail(v3, Modifier.weight(1f).fillMaxHeight())
                    if (v4 != null) LibraryFolderThumbnail(v4, Modifier.weight(1f).fillMaxHeight())
                }
            }
            // Bottom gradient scrim for text readability
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF001F3F).copy(alpha = 0.9f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
            )
            // "NEW ITEMS" badge if folder has any unwatched videos
            if (videos.any { it.playCount == 0 }) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                    BadgeTag(
                        text = "NEW ITEMS",
                        gradient = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5500)))
                    )
                }
            }
            // Folder name + count at the bottom
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${videos.size} video" + if (videos.size > 1) "s" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        }
    }
}

/** Single thumbnail cell inside a [LibraryFolderCard] mosaic. */
@Composable
fun LibraryFolderThumbnail(video: Video, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(video.uri)
            .crossfade(true)
            .size(256)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

/**
 * Compact horizontal list row for the Folders tab list mode.
 *
 * Shows a small multi-thumbnail mosaic, folder name, video count, and a "NEW ITEMS"
 * badge when the folder contains unwatched videos. Full width, ~64dp height.
 */
@Composable
fun LibraryFolderListItem(
    folderName: String,
    videos: List<Video>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: 2×2 thumbnail mosaic ──────────────────────────────────────────
        Card(
            modifier = Modifier.width(52.dp).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    val v1 = videos.getOrNull(0)
                    val v2 = videos.getOrNull(1)
                    if (v1 != null) LibraryFolderThumbnail(v1, Modifier.weight(1f).fillMaxHeight())
                    if (v2 != null) LibraryFolderThumbnail(v2, Modifier.weight(1f).fillMaxHeight())
                }
                Row(modifier = Modifier.weight(1f)) {
                    val v3 = videos.getOrNull(2)
                    val v4 = videos.getOrNull(3)
                    if (v3 != null) LibraryFolderThumbnail(v3, Modifier.weight(1f).fillMaxHeight())
                    if (v4 != null) LibraryFolderThumbnail(v4, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Middle: folder name + count ─────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${videos.size} video" + if (videos.size > 1) "s" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // ── Right: "NEW ITEMS" badge if any unwatched ───────────────────────────
        if (videos.any { it.playCount == 0 }) {
            BadgeTag(
                text = "NEW",
                gradient = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5500)))
            )
        }
    }
}