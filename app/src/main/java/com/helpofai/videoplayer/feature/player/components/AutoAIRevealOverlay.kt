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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * AutoAIRevealOverlay — transparent glass split reveal.
 *
 * The screen splits at the vertical center and BOTH translucent glass halves
 * slide outward (left -> off-screen left, right -> off-screen right). Because
 * the curtains are GLASS (semi-transparent + shimmer), the real enhanced video
 * is visible through them from the very first frame — the split just turns up
 * the clarity. A glowing seam + light sweep mark the parting edge, and a
 * glassmorphic "AI ENHANCED · MOVIE" badge pops in at center before the whole
 * overlay fades away, leaving clean enhanced playback.
 */
@Composable
fun AutoAIRevealOverlay(
    contentType: String,
    durationMs: Int = 2600,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val halfWidthPx = screenWidthPx / 2f

    // Curtain slide progress: 0f = curtains meet at center, 1f = fully parted
    val curtainProgress = remember { Animatable(0f) }
    // Badge pop progress
    val badgeProgress = remember { Animatable(0f) }
    // Fade-out of the whole overlay after the reveal
    val fadeProgress = remember { Animatable(1f) }

    // Continuous light sweep across the glass while the reveal plays
    val sweepTransition = rememberInfiniteTransition(label = "sweep")
    val sweepX by sweepTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepX"
    )
    // Breathing glow on the parting seam
    val seamGlow by sweepTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seamGlow"
    )

    // Badge icon glow ring geometry (precomputed in px)
    val badgeRingRadiusPx = with(density) { 29.dp.toPx() }

    var showBadge by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 1. Part the glass curtains from the center outward (950ms, cinematic ease)
        curtainProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
        // 2. Pop in the badge at the split point
        showBadge = true
        badgeProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
        )
        // 3. Hold the badge, then fade the entire overlay away
        delay((durationMs - 1500L).coerceAtLeast(400L))
        fadeProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500)
        )
    }

    val leftOffset = -halfWidthPx * curtainProgress.value
    val rightOffset = halfWidthPx * curtainProgress.value
    val fade = fadeProgress.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade },
        contentAlignment = Alignment.Center
    ) {
        // ======================= LEFT GLASS CURTAIN =======================
        // Translucent glass: the enhanced video is clearly visible through it.
        // A bright seam glow sits on the parting edge; a light sweep runs across.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .offset { IntOffset(leftOffset.roundToInt(), 0) }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x33060B16), // deep glass, still see-through
                            Color(0x1A0A1330),
                            Color(0x141A2B52)
                        )
                    )
                )
        ) {
            // Light sweep band (AI energy passing through the glass)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .offset { IntOffset((sweepX * halfWidthPx).roundToInt(), 0) }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Bright parting seam on the right edge (where the split happens)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f * seamGlow)
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * seamGlow)
                            )
                        )
                    )
            )
        }

        // ======================= RIGHT GLASS CURTAIN =======================
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .offset { IntOffset(rightOffset.roundToInt(), 0) }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x141A2B52),
                            Color(0x1A0A1330),
                            Color(0x33060B16)
                        )
                    )
                )
        ) {
            // Light sweep band (mirrored)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .offset { IntOffset((sweepX * halfWidthPx).roundToInt(), 0) }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Bright parting seam on the left edge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f * seamGlow)
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * seamGlow),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ======================= GLOWING CENTER SEAM =======================
        // The light beam that traces the split as the glass parts.
        val dividerAlpha = (curtainProgress.value * 0.9f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(16.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f * dividerAlpha * fade * seamGlow),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(
                    Color.White.copy(alpha = 0.95f * dividerAlpha * fade * seamGlow),
                    RoundedCornerShape(1.dp)
                )
        )

        // ======================= GLASS BADGE =======================
        // "AI ENHANCED · MOVIE" — frosted glass chip, not a solid card.
        if (showBadge || badgeProgress.value > 0f) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0x52060B16), // frosted, video glows through
                shadowElevation = 18.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .alpha(badgeProgress.value * fade)
                    .scale(0.7f + 0.3f * badgeProgress.value)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon inside a subtle glow ring
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                        Color.Transparent
                                    ),
                                    center = Offset(badgeRingRadiusPx, badgeRingRadiusPx),
                                    radius = badgeRingRadiusPx
                                ),
                                RoundedCornerShape(29.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // AI ENHANCED · MOVIE badge text
                    Text(
                        text = "AI ENHANCED",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "· ${contentType.replace('_', ' ').uppercase()} ·",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clarity engine active for this video",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
