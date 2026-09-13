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
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.delay

@Composable
private fun rememberAnimatedRainbowColor(): State<Color> {
    val transition = rememberInfiniteTransition(label = "rainbow")
    val hue = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "hue"
    )
    return remember {
        derivedStateOf { Color.hsv(hue = hue.value, saturation = 0.8f, value = 1f) }
    }
}

@Composable
fun ThinRainbowSeekBar(
    value: Float,
    max: Float,
    bookmarks: List<Long>,
    lastPlayedPosition: Long?,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isSeekEnabled: Boolean = true,
    abRepeatA: Long? = null,
    abRepeatB: Long? = null,
    onScrub: ((Float?) -> Unit)? = null
) {
    val rainbowColorState = rememberAnimatedRainbowColor()
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var pendingSeekTarget by remember { mutableStateOf<Float?>(null) }

    // Clear pendingSeekTarget once value catches up (within 1.5 seconds)
    LaunchedEffect(value) {
        pendingSeekTarget?.let { target ->
            if (kotlin.math.abs(value - target) < 1500f) {
                pendingSeekTarget = null
            }
        }
    }

    // Auto timeout for pendingSeekTarget in case seek doesn't produce an immediate update
    LaunchedEffect(pendingSeekTarget) {
        if (pendingSeekTarget != null) {
            delay(700)
            pendingSeekTarget = null
        }
    }

    val activePos = when {
        isDragging -> dragPosition
        pendingSeekTarget != null -> pendingSeekTarget!!
        else -> value
    }
    val currentProgress = if (max > 0f) (activePos / max).coerceIn(0f, 1f) else 0f
    
    Canvas(
        modifier = modifier
            .height(24.dp)
            .fillMaxWidth()
            .then(
                if (isSeekEnabled) {
                    Modifier
                        .pointerInput(max) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val target = fraction * max
                                pendingSeekTarget = target
                                onSeek(target)
                            }
                        }
                        .pointerInput(max) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    dragPosition = fraction * max
                                    onScrub?.invoke(dragPosition)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                    dragPosition = fraction * max
                                    onScrub?.invoke(dragPosition)
                                },
                                onDragEnd = {
                                    val target = dragPosition
                                    isDragging = false
                                    pendingSeekTarget = target
                                    onScrub?.invoke(null)
                                    onSeek(target)
                                },
                                onDragCancel = {
                                    isDragging = false
                                    pendingSeekTarget = null
                                    onScrub?.invoke(null)
                                }
                            )
                        }
                } else Modifier
            )
    ) {
        val rainbowColor = rainbowColorState.value
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

        // A-B Repeat Markers
        if (max > 0) {
            abRepeatA?.let { startMs ->
                val fraction = (startMs.toFloat() / max).coerceIn(0f, 1f)
                val x = size.width * fraction
                drawLine(
                    color = Color(0xFF00CEC9), // Cyan marker
                    start = Offset(x, centerY - 8.dp.toPx()),
                    end = Offset(x, centerY + 8.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
            }
            abRepeatB?.let { endMs ->
                val fraction = (endMs.toFloat() / max).coerceIn(0f, 1f)
                val x = size.width * fraction
                drawLine(
                    color = Color(0xFFFD79A8), // Magenta marker
                    start = Offset(x, centerY - 8.dp.toPx()),
                    end = Offset(x, centerY + 8.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
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
    onMinimizeClick: () -> Unit,
    isSeekEnabled: Boolean = true,
    abRepeatA: Long? = null,
    abRepeatB: Long? = null,
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Video Timer / Seekbar Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var scrubbingPosition by remember { mutableStateOf<Float?>(null) }
                    val displayPosition = scrubbingPosition?.toLong() ?: currentPosition

                    Text(
                        text = formatTime(displayPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )

                    ThinRainbowSeekBar(
                        value = currentPosition.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                        max = duration.coerceAtLeast(1).toFloat(),
                        bookmarks = bookmarks,
                        lastPlayedPosition = lastPlayedPosition,
                        onScrub = { scrubPos ->
                            scrubbingPosition = scrubPos
                        },
                        onSeek = { 
                            scrubbingPosition = null
                            onSeek(it.toLong())
                        },
                        isSeekEnabled = isSeekEnabled,
                        abRepeatA = abRepeatA,
                        abRepeatB = abRepeatB,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Playback controls below the seekbar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        AnimatedIconButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            onClick = onMinimizeClick,
                            size = 44.dp,
                            iconSize = 28.dp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        AnimatedIconButton(
                            icon = Icons.Default.SkipPrevious,
                            onClick = onPrevClick,
                            size = 44.dp,
                            iconSize = 28.dp
                        )
                        
                        // Hero Dynamic Play/Pause Button in controls row
                        DynamicPlayPauseButton(
                            isPlaying = isPlaying,
                            onClick = onPlayPauseClick,
                            size = 56.dp,
                            iconSize = 34.dp,
                            isCompact = false
                        )
                        
                        AnimatedIconButton(
                            icon = Icons.Default.SkipNext,
                            onClick = onNextClick,
                            size = 44.dp,
                            iconSize = 28.dp
                        )
                    }
                    
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        AnimatedIconButton(
                            icon = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            onClick = onFullscreenClick,
                            size = 44.dp,
                            iconSize = 28.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dynamic, fluidly animated Play/Pause button with seamless icon morphing,
 * spring press-feedback, and dynamic gradient glow styling.
 */
@Composable
fun DynamicPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    iconSize: androidx.compose.ui.unit.Dp = 30.dp,
    isCompact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dynamic_button_scale"
    )

    // Smooth gradient styling: vibrant violet-cyan/primary when playing, energetic accent when paused
    val bgBrush = if (isCompact) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                Color(0xFF7C4DFF).copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                Color(0xFF8B5CF6),
                Color(0xFF00CEC9)
            )
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isCompact) 4.dp else 12.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                width = if (isCompact) 1.dp else 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialScale = 0.6f
                )) togetherWith (fadeOut(animationSpec = tween(140)) + scaleOut(
                    animationSpec = tween(140),
                    targetScale = 0.6f
                ))
            },
            label = "dynamic_play_pause_icon"
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = if (isCompact) Color.White else Color.Black,
                modifier = Modifier.size(iconSize)
            )
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
                indication = ripple(bounded = false),
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
