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

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

private val SnakeGradientColors = listOf(
    Color(0xFF00E5FF), // Electric Cyan
    Color(0xFF0072FF), // Neon Cobalt
    Color(0xFF7A00FF), // Vivid Violet
    Color(0xFFFF007F), // Hot Magenta / Pink
    Color(0xFFFF8800)  // Sunset Amber
)

@Composable
fun SnakeGradientSeekBar(
    value: Float,
    max: Float,
    bookmarks: List<Long>,
    lastPlayedPosition: Long?,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isSeekEnabled: Boolean = true,
    abRepeatA: Long? = null,
    abRepeatB: Long? = null,
    onScrub: ((Float?) -> Unit)? = null,
    isPlaying: Boolean = true
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var pendingSeekTarget by remember { mutableStateOf<Float?>(null) }

    // Running Snake Wave animation (traveling sinusoidal wave)
    val infiniteTransition = rememberInfiniteTransition(label = "snake_runner")
    val phaseAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snake_phase"
    )
    val thumbPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thumb_pulse"
    )

    // Wave travels when video is actively playing and not being scrubbed
    val phase = if (isPlaying && !isDragging) phaseAnimation else 0f

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
            .height(28.dp)
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
        val centerY = size.height / 2f
        val trackHeight = 3.dp.toPx()
        val activeTrackStroke = 3.5.dp.toPx()
        val thumbBaseRadius = if (isDragging) 6.5.dp.toPx() else 4.5.dp.toPx()
        val activeWidth = size.width * currentProgress

        // Rich horizontal static gradient across the full timeline
        val snakeGradient = Brush.horizontalGradient(
            colors = SnakeGradientColors,
            startX = 0f,
            endX = size.width.coerceAtLeast(1f)
        )

        // 1. Background unplayed track (from activeWidth to total length)
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(activeWidth.coerceAtLeast(0f), centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        // 2. Last played history region
        if (lastPlayedPosition != null && max > 0) {
            val historyFraction = (lastPlayedPosition / max).coerceIn(0f, 1f)
            val historyWidth = size.width * historyFraction
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, centerY),
                end = Offset(historyWidth, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
            // History Marker
            drawLine(
                color = Color.Yellow.copy(alpha = 0.85f),
                start = Offset(historyWidth, centerY - 6.dp.toPx()),
                end = Offset(historyWidth, centerY + 6.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 3. Bookmarks & Scenes Markers
        if (max > 0) {
            bookmarks.forEach { timeMs ->
                val fraction = (timeMs.toFloat() / max).coerceIn(0f, 1f)
                val x = size.width * fraction
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(x, centerY - 5.dp.toPx()),
                    end = Offset(x, centerY + 5.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 4. A-B Repeat Markers
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

        // 5. Active Played Track: Running Snake Wave Bar with Static Gradient
        if (activeWidth > 0f) {
            val wavelength = 22.dp.toPx()
            val amplitude = 3.5.dp.toPx()
            val transitionDist = 12.dp.toPx()
            val twoPi = (2f * PI).toFloat()

            val snakePath = Path().apply {
                moveTo(0f, centerY)
                val step = 2f
                var x = 0f
                while (x <= activeWidth) {
                    val startFade = (x / transitionDist).coerceIn(0f, 1f)
                    val endFade = ((activeWidth - x) / transitionDist).coerceIn(0f, 1f)
                    val envelope = minOf(startFade, endFade)
                    // Animated running phase reversed in direction
                    val y = centerY + amplitude * envelope * sin((x / wavelength) * twoPi + phase)
                    lineTo(x, y)
                    x += step
                }
                lineTo(activeWidth, centerY)
            }

            drawPath(
                path = snakePath,
                brush = snakeGradient,
                style = Stroke(
                    width = activeTrackStroke,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 6. Snake Head / Scrubber Thumb (with living breathing glow)
            val glowSpread = if (isPlaying && !isDragging) {
                (2.5f + 1.5f * thumbPulse).dp.toPx()
            } else {
                2.5.dp.toPx()
            }
            // Outer halo glow
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = thumbBaseRadius + glowSpread,
                center = Offset(activeWidth, centerY)
            )
            // Vibrant gradient thumb core
            drawCircle(
                brush = snakeGradient,
                radius = thumbBaseRadius,
                center = Offset(activeWidth, centerY)
            )
            // Crisp white inner dot
            drawCircle(
                color = Color.White,
                radius = (thumbBaseRadius * 0.45f).coerceAtLeast(1.5.dp.toPx()),
                center = Offset(activeWidth, centerY)
            )
        }
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
    onScrub: ((Float?) -> Unit)? = null,
    isPlaying: Boolean = true
) = SnakeGradientSeekBar(
    value = value,
    max = max,
    bookmarks = bookmarks,
    lastPlayedPosition = lastPlayedPosition,
    onSeek = onSeek,
    modifier = modifier,
    isSeekEnabled = isSeekEnabled,
    abRepeatA = abRepeatA,
    abRepeatB = abRepeatB,
    onScrub = onScrub,
    isPlaying = isPlaying
)

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

                    SnakeGradientSeekBar(
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
                        isPlaying = isPlaying,
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
