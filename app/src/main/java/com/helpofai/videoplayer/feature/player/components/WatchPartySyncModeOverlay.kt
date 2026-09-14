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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import com.helpofai.videoplayer.core.theme.HoaMiniSwitch
import kotlin.math.roundToInt
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession

/**
 * Watch Party Synchronized Mode control and status overlay for the Now Playing screen.
 * Informs the room admin that Synchronized Mode is required for streaming video to guests
 * and provides direct enable/disable controls.
 *
 * Supports long-press and drag to reposition anywhere on the player screen.
 */
@Composable
fun WatchPartySyncModeOverlay(
    session: WatchPartySession,
    isHost: Boolean,
    isSyncModeEnabled: Boolean,
    isControllerVisible: Boolean,
    onToggleSyncMode: (Boolean) -> Unit,
    onShowControls: () -> Unit,
    offset: Offset = Offset.Zero,
    onOffsetChange: (Offset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1.0f,
        label = "syncDragScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "SyncStatusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .scale(dragScale)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onOffsetChange(offset + dragAmount)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        if (isHost) {
            // Room Admin / Host view
            if (isControllerVisible) {
                // Interactive Synchronized Mode card when controls are visible
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xEE0F172A),
                    border = BorderStroke(
                        if (isDragging) 1.5.dp else 1.dp,
                        if (isDragging) Color(0xFF00FFCC)
                        else if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.55f)
                        else Color(0xFFF59E0B).copy(alpha = 0.45f)
                    ),
                    shadowElevation = if (isDragging) 16.dp else 8.dp,
                    modifier = Modifier.width(285.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // Subtle drag handle pill at the top of the card
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 6.dp)
                                .size(width = 26.dp, height = 3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isDragging) Color(0xFF00FFCC)
                                    else Color.White.copy(alpha = 0.25f)
                                )
                        )

                        // Header row: Compact Icon, Title & Sleek Mini Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.2f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isSyncModeEnabled) Color(0xFF00FFCC).copy(alpha = 0.4f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isSyncModeEnabled) Icons.Default.CellTower else Icons.Default.SyncDisabled,
                                    contentDescription = "Sync Status",
                                    tint = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Synchronized Mode",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                (if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B))
                                                    .copy(alpha = pulseAlpha)
                                             )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSyncModeEnabled) "STREAMING ACTIVE" else "NOT STREAMING",
                                        color = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Sleek Mini Switch matching HOA app UI
                            HoaMiniSwitch(
                                checked = isSyncModeEnabled,
                                onCheckedChange = onToggleSyncMode,
                                activeColor = Color(0xFF10B981)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Educational explanation banner to guide the room admin
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.12f)
                            else Color(0xFFF59E0B).copy(alpha = 0.16f),
                            border = BorderStroke(
                                0.5.dp,
                                if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.35f)
                                else Color(0xFFF59E0B).copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isSyncModeEnabled) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .size(13.dp)
                                        .padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSyncModeEnabled)
                                        "Streaming active: Currently playing video and playback controls are synced live with ${session.devices.size} member(s)."
                                    else
                                        "For streaming to guests, this needs to be enabled. Turn ON Synchronized Mode to broadcast video and sync playback.",
                                    color = if (isSyncModeEnabled) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                    fontSize = 10.sp,
                                    lineHeight = 13.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Room badge footer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Room: ${session.name}",
                                fontSize = 9.5.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ID: ${session.id}",
                                fontSize = 9.5.sp,
                                color = Color(0xFFA78BFA),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Minimal pill when controls are hidden (full screen movie watching)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(
                        if (isDragging) 1.5.dp else 1.dp,
                        if (isDragging) Color(0xFF00FFCC)
                        else if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.5f)
                        else Color(0xFFF59E0B).copy(alpha = 0.6f)
                    ),
                    shadowElevation = if (isDragging) 12.dp else 4.dp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onShowControls()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Hold to drag",
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .size(6.5.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B))
                                        .copy(alpha = pulseAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSyncModeEnabled) "● LIVE SYNC (${session.devices.size})"
                            else "⚠️ SYNC OFF (Tap to Enable)",
                            color = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFFBBF24),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Guest / Client view
            if (isControllerVisible) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF7C5CE7).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFF7C5CE7).copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Default.CellTower,
                            contentDescription = null,
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CONNECTED TO PARTY • ${session.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FFCC))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Party Guest",
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
