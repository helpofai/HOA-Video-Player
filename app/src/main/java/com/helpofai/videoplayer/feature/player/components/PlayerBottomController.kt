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
package com.helpofai.videoplayer.feature.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap

@Composable
private fun getAnimatedRainbowColor(): Color {
    val transition = rememberInfiniteTransition(label = "rainbow")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "hue"
    )
    return Color.hsv(hue = hue, saturation = 0.8f, value = 1f)
}

@Composable
fun ThinRainbowSeekBar(
    value: Float,
    max: Float,
    bookmarks: List<Long>,
    lastPlayedPosition: Long?,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val rainbowColor = getAnimatedRainbowColor()
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    
    val currentProgress = if (max > 0f) (dragPosition ?: value) / max else 0f
    
    Canvas(
        modifier = modifier
            .height(24.dp)
            .fillMaxWidth()
            .pointerInput(max) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction * max)
                }
            }
            .pointerInput(max) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        dragPosition = fraction * max
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragPosition = fraction * max
                    },
                    onDragEnd = {
                        dragPosition?.let { onSeek(it) }
                        dragPosition = null
                    },
                    onDragCancel = {
                        dragPosition = null
                    }
                )
            }
    ) {
        val trackHeight = 2.dp.toPx()
        val thumbRadius = 4.dp.toPx()
        val centerY = size.height / 2f
        
        // Background track
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )
        
        // Last played history region
        if (lastPlayedPosition != null && max > 0) {
            val historyFraction = (lastPlayedPosition / max).coerceIn(0f, 1f)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(size.width * historyFraction, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
            // History Marker
            drawLine(
                color = Color.Yellow.copy(alpha = 0.8f),
                start = Offset(size.width * historyFraction, centerY - 6.dp.toPx()),
                end = Offset(size.width * historyFraction, centerY + 6.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
        
        // Bookmarks & Scenes Marker
        if (max > 0) {
            bookmarks.forEach { timeMs ->
                val fraction = (timeMs.toFloat() / max).coerceIn(0f, 1f)
                val x = size.width * fraction
                drawLine(
                    color = Color.White,
                    start = Offset(x, centerY - 4.dp.toPx()),
                    end = Offset(x, centerY + 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        
        // Active track
        val activeWidth = size.width * currentProgress
        if (activeWidth > 0f) {
            drawLine(
                color = rainbowColor,
                start = Offset(0f, centerY),
                end = Offset(activeWidth, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }
        
        // Thumb
        drawCircle(
            color = rainbowColor,
            radius = thumbRadius,
            center = Offset(activeWidth, centerY)
        )
    }
}

@Composable
fun PlayerBottomController(
    isVisible: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isLandscape: Boolean,
    bookmarks: List<Long> = emptyList(),
    lastPlayedPosition: Long? = null,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(250)
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = if (isLandscape) 48.dp else 16.dp)
                .padding(bottom = 24.dp, top = 32.dp)
        ) {
            // Single Row Layout for maximum space saving
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedIconButton(
                    icon = Icons.Default.SkipPrevious,
                    onClick = onPrevClick,
                    size = 36.dp,
                    iconSize = 24.dp
                )
                
                AnimatedIconButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = onPlayPauseClick,
                    size = 36.dp,
                    iconSize = 24.dp,
                    tint = Color.White
                )
                
                AnimatedIconButton(
                    icon = Icons.Default.SkipNext,
                    onClick = onNextClick,
                    size = 36.dp,
                    iconSize = 24.dp
                )
                
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
                
                var sliderValue by remember(currentPosition, duration) { 
                    mutableFloatStateOf(currentPosition.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat())) 
                }
                
                ThinRainbowSeekBar(
                    value = sliderValue,
                    max = duration.coerceAtLeast(1).toFloat(),
                    bookmarks = bookmarks,
                    lastPlayedPosition = lastPlayedPosition,
                    onSeek = { 
                        sliderValue = it
                        onSeek(it.toLong())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
                
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                AnimatedIconButton(
                    icon = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    onClick = onFullscreenClick,
                    size = 36.dp,
                    iconSize = 24.dp
                )
            }
        }
    }
}

@Composable
fun AnimatedIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    backgroundColor: Color = Color.Transparent,
    tint: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(bounded = false),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
