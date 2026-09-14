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

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Advanced, tactile Slide/Drag to Unlock slider for the Now Playing screen.
 * Prevents accidental unlocking by requiring an intentional swipe with spring physics,
 * real-time haptic confirmation, dynamic track color transitions, and traveling shimmer.
 */
@Composable
fun SlideToUnlockSlider(
    onUnlocked: () -> Unit,
    onDragInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current

    val thumbSize = 48.dp
    val trackPadding = 4.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val trackPaddingPx = with(density) { trackPadding.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val maxDragPx = (trackWidthPx - thumbSizePx - (trackPaddingPx * 2f)).coerceAtLeast(0f)

    val dragOffsetX = remember { Animatable(0f) }
    val progress = if (maxDragPx > 0f) (dragOffsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f
    val isNearUnlock = progress >= 0.85f

    // Shimmer traveling wave for track label
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmerTransition")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    // Thumb breathing glow pulse
    val thumbPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThumbPulse"
    )

    var hasFiredHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(isNearUnlock) {
        if (isNearUnlock && !hasFiredHaptic) {
            hasFiredHaptic = true
            try {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } catch (_: Exception) {}
        } else if (!isNearUnlock) {
            hasFiredHaptic = false
        }
    }

    val draggableState = rememberDraggableState { delta ->
        onDragInteraction()
        coroutineScope.launch {
            val target = (dragOffsetX.value + delta).coerceIn(0f, maxDragPx)
            dragOffsetX.snapTo(target)
        }
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(280.dp)
            .height(56.dp)
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xEE0F172A))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF7C5CE7).copy(alpha = 0.5f),
                        if (isNearUnlock) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF38BDF8).copy(alpha = 0.5f)
                    )
                ),
                RoundedCornerShape(28.dp)
            )
            .padding(trackPadding)
    ) {
        // Active dragged progress fill
        val fillWidthDp = with(density) { (dragOffsetX.value + thumbSizePx).toDp() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(fillWidthDp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x337C5CE7),
                            if (isNearUnlock) Color(0x6610B981) else Color(0x5538BDF8)
                        )
                    )
                )
        )

        // Track Shimmer Text (fades out as thumb progresses)
        val textAlpha = (1f - progress * 1.6f).coerceIn(0f, 1f)
        if (textAlpha > 0.05f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 44.dp, end = 16.dp)
            ) {
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f * textAlpha),
                        Color.White.copy(alpha = 0.95f * textAlpha),
                        Color.White.copy(alpha = 0.35f * textAlpha)
                    ),
                    start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
                    end = androidx.compose.ui.geometry.Offset(shimmerOffset + 120f, 0f)
                )

                Text(
                    text = "Slide to Unlock",
                    color = Color.White.copy(alpha = textAlpha),
                    style = androidx.compose.ui.text.TextStyle(brush = shimmerBrush),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f * textAlpha),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Draggable Circular Thumb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(x = dragOffsetX.value.roundToInt(), y = 0) }
                .size(thumbSize)
                .scale(if (isNearUnlock) 1.06f else thumbPulse)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isNearUnlock) {
                            listOf(Color(0xFF10B981), Color(0xFF059669))
                        } else {
                            listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                        }
                    )
                )
                .border(
                    1.5.dp,
                    if (isNearUnlock) Color(0xFF34D399) else Color(0xFFA78BFA),
                    CircleShape
                )
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { onDragInteraction() },
                    onDragStopped = {
                        if (progress >= 0.85f) {
                            coroutineScope.launch {
                                dragOffsetX.animateTo(
                                    targetValue = maxDragPx,
                                    animationSpec = tween(120, easing = FastOutSlowInEasing)
                                )
                                onUnlocked()
                            }
                        } else {
                            coroutineScope.launch {
                                dragOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
                )
        ) {
            Icon(
                imageVector = if (isNearUnlock) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = if (isNearUnlock) "Unlocked" else "Locked",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(if (isNearUnlock) -15f else 0f)
            )
        }
    }
}
