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
package com.helpofai.videoplayer.feature.watch_party.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.notification.WatchPartyNotificationManager
import kotlinx.coroutines.delay

/**
 * Global overlay composable that shows a Watch Party join request popup
 * on ANY active screen. Place this at the root level in MainActivity.
 *
 * Observes [WatchPartySessionManager.pendingRequest] globally.
 * When a device sends a join request, the popup appears as a
 * full-screen dialog overlay regardless of which page the user is on.
 */
@Composable
fun WatchPartyJoinRequestOverlay() {
    val sessionManager = remember { WatchPartySessionManager.getInstance() }
    val notificationManager = remember { WatchPartyNotificationManager.getInstance() }
    val pendingDevice by sessionManager.pendingRequest.collectAsState()
    val context = LocalContext.current

    // Auto-dismiss timer: reject after 30 seconds if no action
    LaunchedEffect(pendingDevice) {
        if (pendingDevice != null) {
            delay(30_000)
            if (sessionManager.pendingRequest.value != null) {
                val expiredName = sessionManager.pendingRequest.value?.name ?: "Device"
                notificationManager.notifyRequestExpired(expiredName)
                Toast.makeText(context, "Join request expired: $expiredName", Toast.LENGTH_SHORT).show()
                sessionManager.clearPendingRequest()
            }
        }
    }

    AnimatedVisibility(
        visible = pendingDevice != null,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f)
    ) {
        val guest = pendingDevice ?: return@AnimatedVisibility
        val activeSession by sessionManager.activeSession.collectAsState()

        // Pulsing glow animation for the icon
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        // Countdown timer
        var secondsLeft by remember(guest) { mutableIntStateOf(30) }
        LaunchedEffect(guest) {
            secondsLeft = 30
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
        }

        Dialog(
            onDismissRequest = {
                Toast.makeText(context, "Join request dismissed: ${guest.name}", Toast.LENGTH_SHORT).show()
                sessionManager.clearPendingRequest()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .shadow(24.dp, RoundedCornerShape(20.dp))
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1D2E),
                                    Color(0xFF12141F),
                                    Color(0xFF0D0F18)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    Color(0xFF6C5CE7).copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header with animated icon
                        Box(contentAlignment = Alignment.Center) {
                            // Outer pulsing glow ring
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.15f),
                                        CircleShape
                                    )
                            )
                            // Inner icon circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                Color(0xFF6C5CE7)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.GroupAdd,
                                    contentDescription = "Join Request",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Title
                        Text(
                            "Watch Party Join Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )

                        // Countdown badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (secondsLeft <= 10)
                                Color.Red.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Auto-reject in ${secondsLeft}s",
                                fontSize = 10.sp,
                                color = if (secondsLeft <= 10) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Divider(
                            color = Color.White.copy(alpha = 0.06f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // Device info card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Device name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            guest.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            "wants to join your Watch Party",
                                            fontSize = 11.sp,
                                            color = Color.LightGray.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.05f))

                                // Stats row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // IP Address
                                    DeviceStatItem(
                                        icon = Icons.Default.Wifi,
                                        label = "IP",
                                        value = guest.ipAddress
                                    )
                                    // Latency
                                    DeviceStatItem(
                                        icon = Icons.Default.Speed,
                                        label = "Ping",
                                        value = "${guest.latency}ms"
                                    )
                                    // Battery
                                    DeviceStatItem(
                                        icon = if (guest.batteryLevel > 20) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                                        label = "Battery",
                                        value = "${guest.batteryLevel}%"
                                    )
                                }

                                // Connection speed
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        tint = Color(0xFF00CEC9),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Connection: ${guest.connectionSpeed} Mbps",
                                        fontSize = 10.sp,
                                        color = Color(0xFF00CEC9)
                                    )
                                }
                            }
                        }

                        // Current room info
                        if (activeSession != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.03f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = Color(0xFF6C5CE7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            "Room: ${activeSession!!.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            "${activeSession!!.devices.size} device(s) connected",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Accept button
                            Button(
                                onClick = {
                                    sessionManager.addDevice(guest.copy(status = "Playing"))
                                    Toast.makeText(
                                        context,
                                        "✓ Accepted: ${guest.name} connected!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    sessionManager.clearPendingRequest()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                Color(0xFF6C5CE7)
                                            )
                                        ),
                                        RoundedCornerShape(12.dp)
                                    ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Accept",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Reject button
                            OutlinedButton(
                                onClick = {
                                    notificationManager.notifyDeviceRejected(guest)
                                    Toast.makeText(
                                        context,
                                        "✗ Rejected: ${guest.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    sessionManager.clearPendingRequest()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF6B6B)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B6B).copy(alpha = 0.5f),
                                            Color(0xFFFF6B6B).copy(alpha = 0.2f)
                                        )
                                    )
                                ),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Reject",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            label,
            fontSize = 9.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold
        )
    }
}
