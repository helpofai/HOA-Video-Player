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
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.theme.ToolIconPalette

/* =====================================================================
 *  PROFESSIONAL COLOR ICON SYSTEM
 *  Each tool has its own signature color so the toolbar reads at a
 *  glance. Inactive icons show their color at 75% alpha; ACTIVE icons
 *  light up to full color, gain a soft glow halo, a bottom indicator
 *  dot, and a subtle scale pop.
 * ===================================================================== */

/**
 * AnimatedIconButton — the colorful, professional toolbar button.
 *
 * @param color    Signature color of the tool. Shown muted when inactive,
 *                 full + glowing when active.
 * @param isActive When true the icon lights up (color, glow halo,
 *                 indicator dot, scale pop).
 */
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    color: Color = Color.White,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else if (isActive) 1.15f else 1f,
        animationSpec = tween(150),
        label = "iconScale"
    )

    val actualTint = if (isActive) color else color.copy(alpha = 0.75f)

    Box(contentAlignment = Alignment.Center) {
        // Soft glow halo behind ACTIVE icons
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.30f), Color.Transparent)
                        )
                    )
            )
        }
        IconButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier.scale(scale)
        ) {
            Icon(icon, contentDescription = contentDescription, tint = actualTint)
        }
        // Small indicator dot under ACTIVE icons
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(5.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerTopToolbar(
    isVisible: Boolean,
    title: String,
    onBackClick: () -> Unit,
    onLockClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onEqClick: () -> Unit,
    onLoopClick: () -> Unit,
    onInfoClick: () -> Unit,
    onRotateClick: () -> Unit,
    onVideoEnhancerClick: () -> Unit,
    onAutoAIEnhanceClick: () -> Unit,
    isHQMode: Boolean = false,
    onHQClick: () -> Unit = {},
    onVideoAdjustmentsClick: () -> Unit,
    abRepeatState: String,
    onABRepeatClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onMoreClick: () -> Unit,
    isToolsExpanded: Boolean,
    onToolsExpandedChange: (Boolean) -> Unit,
    onEmptyClick: () -> Unit,
    isStreaming: Boolean = false,
    isHost: Boolean = false,
    // ---- Live active states (light the icon up) ----
    isLockActive: Boolean = false,
    isSpeedActive: Boolean = false,
    isEqActive: Boolean = false,
    isLoopActive: Boolean = false,
    isRotateActive: Boolean = false,
    isEnhancerActive: Boolean = false,
    isAutoAIActive: Boolean = false,
    isAdjustmentsActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(180)
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(150)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onEmptyClick()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onEmptyClick() }
                        )
                    }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!isToolsExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = androidx.compose.foundation.MarqueeAnimationMode.Immediately
                                )
                                .padding(end = 8.dp)
                        )
                        if (isStreaming) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = (if (isHost) Color.Red else Color(0xFF00FFCC)).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, (if (isHost) Color.Red else Color(0xFF00FFCC)).copy(alpha = 0.5f)),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "LIVE STREAMING",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHost) Color.Red else Color(0xFF00FFCC),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Quick access most required icons when collapsed
                    AnimatedIconButton(
                        onClick = onAudioClick,
                        icon = Icons.Default.Audiotrack,
                        contentDescription = "Audio",
                        color = ToolIconPalette.Audio
                    )
                    AnimatedIconButton(
                        onClick = onSubtitlesClick,
                        icon = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        color = ToolIconPalette.Subtitles
                    )
                    AnimatedIconButton(
                        onClick = onVideoEnhancerClick,
                        icon = Icons.Default.AutoFixHigh,
                        contentDescription = "Video Enhancer",
                        color = ToolIconPalette.VideoEnhancer,
                        isActive = isEnhancerActive
                    )
                    AnimatedIconButton(
                        onClick = onAutoAIEnhanceClick,
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = "Auto AI Enhancement",
                        color = ToolIconPalette.AutoAI,
                        isActive = isAutoAIActive
                    )
                    AnimatedIconButton(
                        onClick = onHQClick,
                        icon = Icons.Default.HighQuality,
                        contentDescription = "HQ Mode",
                        color = ToolIconPalette.HQ,
                        isActive = isHQMode
                    )
                    AnimatedIconButton(
                        onClick = onVideoAdjustmentsClick,
                        icon = Icons.Default.AspectRatio,
                        contentDescription = "Adjustments",
                        color = ToolIconPalette.Adjustments,
                        isActive = isAdjustmentsActive
                    )

                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        AnimatedIconButton(
                            onClick = onLockClick,
                            icon = Icons.Default.Lock,
                            contentDescription = "Lock",
                            color = ToolIconPalette.Lock,
                            isActive = isLockActive
                        )
                        AnimatedIconButton(
                            onClick = onSpeedClick,
                            icon = Icons.Default.Speed,
                            contentDescription = "Speed",
                            color = ToolIconPalette.Speed,
                            isActive = isSpeedActive
                        )
                        AnimatedIconButton(
                            onClick = onEqClick,
                            icon = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            color = ToolIconPalette.Equalizer,
                            isActive = isEqActive
                        )
                        AnimatedIconButton(
                            onClick = onLoopClick,
                            icon = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            color = ToolIconPalette.Loop,
                            isActive = isLoopActive
                        )
                        AnimatedIconButton(
                            onClick = onAudioClick,
                            icon = Icons.Default.Audiotrack,
                            contentDescription = "Audio",
                            color = ToolIconPalette.Audio
                        )
                        AnimatedIconButton(
                            onClick = onSubtitlesClick,
                            icon = Icons.Default.Subtitles,
                            contentDescription = "Subtitles",
                            color = ToolIconPalette.Subtitles
                        )
                        AnimatedIconButton(
                            onClick = onScreenshotClick,
                            icon = Icons.Default.PhotoCamera,
                            contentDescription = "Screenshot",
                            color = ToolIconPalette.Screenshot
                        )

                        Box(modifier = Modifier.clickable { onABRepeatClick() }.padding(8.dp), contentAlignment = Alignment.Center) {
                            AnimatedIconButton(
                                onClick = onABRepeatClick,
                                icon = Icons.Default.SyncAlt,
                                contentDescription = "AB Repeat",
                                color = ToolIconPalette.ABRepeat,
                                isActive = abRepeatState.isNotEmpty()
                            )
                            if (abRepeatState.isNotEmpty()) {
                                Text(
                                    text = abRepeatState,
                                    color = ToolIconPalette.ABRepeat,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                        AnimatedIconButton(
                            onClick = onRotateClick,
                            icon = Icons.Default.ScreenRotation,
                            contentDescription = "Rotate",
                            color = ToolIconPalette.Rotate,
                            isActive = isRotateActive
                        )
                        AnimatedIconButton(
                            onClick = onVideoEnhancerClick,
                            icon = Icons.Default.AutoFixHigh,
                            contentDescription = "Video Enhancer",
                            color = ToolIconPalette.VideoEnhancer,
                            isActive = isEnhancerActive
                        )
                        AnimatedIconButton(
                            onClick = onAutoAIEnhanceClick,
                            icon = Icons.Default.AutoAwesome,
                            contentDescription = "Auto AI Enhancement",
                            color = ToolIconPalette.AutoAI,
                            isActive = isAutoAIActive
                        )
                        AnimatedIconButton(
                            onClick = onHQClick,
                            icon = Icons.Default.HighQuality,
                            contentDescription = "HQ Mode",
                            color = ToolIconPalette.HQ,
                            isActive = isHQMode
                        )
                        AnimatedIconButton(
                            onClick = onVideoAdjustmentsClick,
                            icon = Icons.Default.AspectRatio,
                            contentDescription = "Adjustments",
                            color = ToolIconPalette.Adjustments,
                            isActive = isAdjustmentsActive
                        )

                        val context = androidx.compose.ui.platform.LocalContext.current
                        AnimatedIconButton(
                            onClick = {
                                val activity = context as? android.app.Activity
                                val params = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(16, 9))
                                    .build()
                                activity?.enterPictureInPictureMode(params)
                            },
                            icon = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Mini Player",
                            color = ToolIconPalette.PiP
                        )
                        AnimatedIconButton(
                            onClick = onInfoClick,
                            icon = Icons.Default.Info,
                            contentDescription = "Info",
                            color = ToolIconPalette.Info
                        )
                    }
                }

                // Expand/Collapse Tools Toggle
                AnimatedIconButton(
                    onClick = { onToolsExpandedChange(!isToolsExpanded) },
                    icon = if (isToolsExpanded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = if (isToolsExpanded) "Collapse Tools" else "Expand Tools",
                    color = MaterialTheme.colorScheme.primary,
                    isActive = isToolsExpanded
                )

                Box {
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }
        }
    }
}