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

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.helpofai.videoplayer.core.model.Video

/**
 * Custom Folder Shape drawing a folder silhouette.
 */
class FolderShape(private val tabHeightPx: Float = 35f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val r = 24f
            val tabWidth = width * 0.45f
            val slope = 15f
            
            reset()
            moveTo(0f, tabHeightPx + r)
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(tabWidth - slope, 0f)
            lineTo(tabWidth, tabHeightPx)
            lineTo(width - r, tabHeightPx)
            quadraticTo(width, tabHeightPx, width, tabHeightPx + r)
            lineTo(width, height - r)
            quadraticTo(width, height, width - r, height)
            lineTo(r, height)
            quadraticTo(0f, height, 0f, height - r)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun LibraryFolderCard(
    folderName: String,
    videos: List<Video>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick)
    ) {
        // 1. Back flap of the folder
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = FolderShape(35f),
            color = Color(0xFF1E2746),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {}

        // 2. Video thumbnails sticking out of the folder (clipped to body)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 22.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f))
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
            )
        }

        // 3. Front flap of the folder (covers the bottom 62%)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            color = Color(0xFF283566),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // "NEW ITEMS" badge if folder has any unwatched videos
                if (videos.any { it.playCount == 0 }) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        BadgeTag(
                            text = "NEW",
                            gradient = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5500)))
                        )
                    }
                }

                // Folder details at the bottom of the front flap
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00CEC9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = "Folder",
                                tint = Color(0xFF090B10),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${videos.size} video" + if (videos.size > 1) "s" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
            }
        }
    }
}

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
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111520).copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Mini 3D Folder Icon with thumbnails peaking out!
        Box(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
        ) {
            // Folder Back Flap
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = FolderShape(15f),
                color = Color(0xFF1E2746),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {}

            // Thumbnails peaking out
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
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

            // Folder Front Flap (covers bottom 60%)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                color = Color(0xFF283566),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {}
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Center: Folder name & count
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${videos.size} video" + if (videos.size > 1) "s" else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Right side: New items badge or folder icon indicator
        if (videos.any { it.playCount == 0 }) {
            BadgeTag(
                text = "NEW",
                gradient = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5500)))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = Color(0xFF00CEC9).copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}