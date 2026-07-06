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
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else if (isActive) 1.15f else 1f,
        animationSpec = tween(150),
        label = "iconScale"
    )
    
    val actualTint = if (isActive) MaterialTheme.colorScheme.primary else tint

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.scale(scale)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = actualTint)
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
    onVideoAdjustmentsClick: () -> Unit,
    abRepeatState: String,
    onABRepeatClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onMoreClick: () -> Unit,
    isToolsExpanded: Boolean,
    onToolsExpandedChange: (Boolean) -> Unit,
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .padding(end = 8.dp)
                    )
                    
                    // Quick access most required icons when collapsed
                    AnimatedIconButton(onClick = onAudioClick, icon = Icons.Default.Audiotrack, contentDescription = "Audio")
                    AnimatedIconButton(onClick = onSubtitlesClick, icon = Icons.Default.Subtitles, contentDescription = "Subtitles")
                    AnimatedIconButton(onClick = onVideoEnhancerClick, icon = Icons.Default.AutoFixHigh, contentDescription = "Video Enhancer")
                    AnimatedIconButton(onClick = onVideoAdjustmentsClick, icon = Icons.Default.AspectRatio, contentDescription = "Adjustments")
                    
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        AnimatedIconButton(onClick = onLockClick, icon = Icons.Default.Lock, contentDescription = "Lock")
                        AnimatedIconButton(onClick = onSpeedClick, icon = Icons.Default.Speed, contentDescription = "Speed")
                        AnimatedIconButton(onClick = onEqClick, icon = Icons.Default.GraphicEq, contentDescription = "Equalizer")
                        AnimatedIconButton(onClick = onLoopClick, icon = Icons.Default.Repeat, contentDescription = "Loop")
                        AnimatedIconButton(onClick = onAudioClick, icon = Icons.Default.Audiotrack, contentDescription = "Audio")
                        AnimatedIconButton(onClick = onSubtitlesClick, icon = Icons.Default.Subtitles, contentDescription = "Subtitles")
                        AnimatedIconButton(onClick = onScreenshotClick, icon = Icons.Default.PhotoCamera, contentDescription = "Screenshot")
                        
                        Box(modifier = Modifier.clickable { onABRepeatClick() }.padding(8.dp), contentAlignment = Alignment.Center) {
                            AnimatedIconButton(
                                onClick = onABRepeatClick, 
                                icon = Icons.Default.SyncAlt, 
                                contentDescription = "AB Repeat", 
                                isActive = abRepeatState.isNotEmpty()
                            )
                            if (abRepeatState.isNotEmpty()) {
                                Text(
                                    text = abRepeatState,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                        AnimatedIconButton(onClick = onRotateClick, icon = Icons.Default.ScreenRotation, contentDescription = "Rotate")
                        AnimatedIconButton(onClick = onVideoEnhancerClick, icon = Icons.Default.AutoFixHigh, contentDescription = "Video Enhancer")
                        AnimatedIconButton(onClick = onVideoAdjustmentsClick, icon = Icons.Default.AspectRatio, contentDescription = "Adjustments")
                        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            AnimatedIconButton(
                                onClick = {
                                    val activity = context as? android.app.Activity
                                    val params = android.app.PictureInPictureParams.Builder()
                                        .setAspectRatio(android.util.Rational(16, 9))
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                },
                                icon = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Mini Player"
                            )
                        }
                        AnimatedIconButton(onClick = onInfoClick, icon = Icons.Default.Info, contentDescription = "Info")
                    }
                }

                // Expand/Collapse Tools Toggle
                AnimatedIconButton(
                    onClick = { onToolsExpandedChange(!isToolsExpanded) },
                    icon = if (isToolsExpanded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = if (isToolsExpanded) "Collapse Tools" else "Expand Tools"
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
