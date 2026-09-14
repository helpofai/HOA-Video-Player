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
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession

/**
 * Watch Party Synchronized Mode control and status overlay for the Now Playing screen.
 * Informs the room admin that Synchronized Mode is required for streaming video to guests
 * and provides direct enable/disable controls.
 */
@Composable
fun WatchPartySyncModeOverlay(
    session: WatchPartySession,
    isHost: Boolean,
    isSyncModeEnabled: Boolean,
    isControllerVisible: Boolean,
    onToggleSyncMode: (Boolean) -> Unit,
    onShowControls: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    Box(modifier = modifier) {
        if (isHost) {
            // Room Admin / Host view
            if (isControllerVisible) {
                // Interactive Synchronized Mode card when controls are visible
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xEE0F172A),
                    border = BorderStroke(
                        1.dp,
                        if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.55f)
                        else Color(0xFFF59E0B).copy(alpha = 0.45f)
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Header row: Icon, Title & Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.2f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isSyncModeEnabled) Icons.Default.CellTower else Icons.Default.SyncDisabled,
                                    contentDescription = "Sync Status",
                                    tint = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Synchronized Mode",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                (if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B))
                                                    .copy(alpha = pulseAlpha)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isSyncModeEnabled) "STREAMING ACTIVE" else "NOT STREAMING",
                                        color = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Enable / Disable Switch
                            Switch(
                                checked = isSyncModeEnabled,
                                onCheckedChange = onToggleSyncMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF7C5CE7),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color(0x44FFFFFF)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                Text(
                                    text = if (isSyncModeEnabled) "🟢" else "⚠️",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 1.dp, end = 6.dp)
                                )
                                Text(
                                    text = if (isSyncModeEnabled)
                                        "Streaming active: Currently playing video and playback controls are synced live with ${session.devices.size} member(s)."
                                    else
                                        "For streaming to guests, this needs to be enabled. Turn ON Synchronized Mode to broadcast video and sync playback.",
                                    color = if (isSyncModeEnabled) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
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
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ID: ${session.id}",
                                fontSize = 10.sp,
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
                        1.dp,
                        if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.5f)
                        else Color(0xFFF59E0B).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onShowControls()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B))
                                        .copy(alpha = pulseAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncModeEnabled) "● LIVE SYNC (${session.devices.size})"
                            else "⚠️ SYNC OFF (Tap to Enable)",
                            color = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFFBBF24),
                            fontSize = 10.sp,
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
