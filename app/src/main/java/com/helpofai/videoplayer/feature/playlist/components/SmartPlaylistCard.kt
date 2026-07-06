package com.helpofai.videoplayer.feature.playlist.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.helpofai.videoplayer.feature.playlist.SmartPlaylist

@Composable
fun SmartPlaylistCard(
    playlist: SmartPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )
    
    val accentColor = playlist.accentColor
    val totalDurationMs = playlist.videos.sumOf { it.duration }
    val totalDurationFormatted = formatTotalDuration(totalDurationMs)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        // Multi-thumbnail mosaic (up to 4 thumbnails)
        val thumbnailVideos = playlist.videos.take(4)
        when {
            thumbnailVideos.size >= 4 -> {
                // 2x2 grid
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = thumbnailVideos[0].uri,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = thumbnailVideos[1].uri,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = thumbnailVideos[2].uri,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = thumbnailVideos[3].uri,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            thumbnailVideos.size >= 2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    thumbnailVideos.take(2).forEach { video ->
                        AsyncImage(
                            model = video.uri,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            thumbnailVideos.isNotEmpty() -> {
                AsyncImage(
                    model = thumbnailVideos[0].uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accentColor.copy(alpha = 0.8f), accentColor.copy(alpha = 0.3f))
                            )
                        )
                )
            }
        }
        
        // Dark gradient overlay (bottom-heavy)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f
                    )
                )
        )
        
        // Accent color strip at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Icon badge (top-left)
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.85f))
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = playlist.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Video count badge (top-right)
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .align(Alignment.TopEnd)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${playlist.videos.size} videos",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
        
        // Bottom content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            Text(
                text = playlist.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.2.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playlist.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (totalDurationMs > 0) {
                    Text(
                        text = totalDurationFormatted,
                        color = accentColor.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        
        // Play all FAB (center-right)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                    )
                )
                .graphicsLayer { shadowElevation = 8f },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play All",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

private fun formatTotalDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    val totalSecs = durationMs / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "${totalSecs}s"
    }
}
