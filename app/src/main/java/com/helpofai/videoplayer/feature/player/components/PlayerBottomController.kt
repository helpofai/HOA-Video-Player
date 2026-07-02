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
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val rainbowColor = getAnimatedRainbowColor()
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    
    val currentProgress = if (max > 0f) (dragPosition ?: value) / max else 0f
    
    Box(
        modifier = modifier
            .height(24.dp) // Touch target size
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
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Background track (very thin)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        // Active track (rainbow, thin)
        Box(
            modifier = Modifier
                .fillMaxWidth(currentProgress.coerceIn(0f, 1f))
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(rainbowColor)
        )
        
        // Thumb (small dot, same rainbow color)
        Box(
            modifier = Modifier
                .fillMaxWidth(currentProgress.coerceIn(0f, 1f))
                .wrapContentWidth(Alignment.End)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(rainbowColor)
            )
        }
    }
}

@Composable
fun PlayerBottomController(
    isVisible: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isLandscape: Boolean,
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
