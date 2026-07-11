package com.helpofai.videoplayer.feature.watch_party.notification

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import kotlinx.coroutines.delay

/**
 * Global Watch Party notification overlay.
 * Place at root level in MainActivity — renders notification toasts
 * on top of ANY screen, sliding in from the top.
 *
 * Features:
 * - Stacked notification queue (max 5 visible)
 * - Auto-dismiss with configurable duration per notification
 * - Slide-in/out animations with spring physics
 * - Rejected device notifications include "Accept" action button
 * - Color-coded by notification type
 * - Swipe-to-dismiss ready
 */
@Composable
fun WatchPartyNotificationOverlay() {
    val notificationManager = remember { WatchPartyNotificationManager.getInstance() }
    val sessionManager = remember { WatchPartySessionManager.getInstance() }
    val notifications by notificationManager.notifications.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifications.forEach { notification ->
                key(notification.id) {
                    WatchPartyNotificationCard(
                        notification = notification,
                        onDismiss = { notificationManager.dismiss(notification.id) },
                        onAccept = {
                            // Re-accept a previously rejected device
                            notification.device?.let { device ->
                                sessionManager.addDevice(device.copy(status = "Playing"))
                                Toast.makeText(
                                    context,
                                    "✓ ${device.name} accepted into party!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                notificationManager.dismiss(notification.id)
                                // Push a connected notification
                                val session = sessionManager.activeSession.value
                                notificationManager.notifyDeviceConnected(
                                    device,
                                    (session?.devices?.size ?: 0) + 1
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchPartyNotificationCard(
    notification: WatchPartyNotification,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    // Auto-dismiss timer
    LaunchedEffect(notification.id) {
        if (notification.autoDismissMs > 0) {
            delay(notification.autoDismissMs)
            onDismiss()
        }
    }

    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
        ) + fadeIn(tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(250)
        ) + fadeOut(tween(200))
    ) {
        val (icon, accentColor, bgGradient) = getNotificationStyle(notification.type)

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(bgGradient),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = accentColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onDismiss() }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon indicator
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Text content
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            notification.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            notification.message,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )

                        // Device stats row (if device data available)
                        if (notification.device != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NotifStatChip(
                                    icon = Icons.Default.Wifi,
                                    text = notification.device.ipAddress,
                                    color = accentColor
                                )
                                NotifStatChip(
                                    icon = Icons.Default.Speed,
                                    text = "${notification.device.latency}ms",
                                    color = accentColor
                                )
                                NotifStatChip(
                                    icon = Icons.Default.BatteryFull,
                                    text = "${notification.device.batteryLevel}%",
                                    color = accentColor
                                )
                            }
                        }

                        // Device count badge
                        if (notification.deviceCount > 0 && notification.type == WatchPartyNotificationType.CONNECTION_SUMMARY) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "${notification.deviceCount} devices connected",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }

                    // Action area
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Accept button for rejected devices
                        if (notification.hasAcceptAction && notification.device != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00B894),
                                modifier = Modifier.clickable { onAccept() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Accept",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Accept",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Close button
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .clickable { onDismiss() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifStatChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.6f),
            modifier = Modifier.size(10.dp)
        )
        Text(
            text,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * Returns (icon, accentColor, backgroundGradientColors) for each notification type.
 */
private fun getNotificationStyle(type: WatchPartyNotificationType): Triple<ImageVector, Color, List<Color>> {
    return when (type) {
        WatchPartyNotificationType.DEVICE_CONNECTED -> Triple(
            Icons.Default.PhoneAndroid,
            Color(0xFF00B894),
            listOf(Color(0xFF1A2A1F), Color(0xFF141C19), Color(0xFF101418))
        )

        WatchPartyNotificationType.DEVICE_DISCONNECTED -> Triple(
            Icons.Default.PhonelinkOff,
            Color(0xFFFF7675),
            listOf(Color(0xFF2A1A1A), Color(0xFF1C1414), Color(0xFF181010))
        )

        WatchPartyNotificationType.DEVICE_REJECTED -> Triple(
            Icons.Default.Block,
            Color(0xFFFF6B6B),
            listOf(Color(0xFF2E1515), Color(0xFF1E1010), Color(0xFF160C0C))
        )

        WatchPartyNotificationType.SESSION_CREATED -> Triple(
            Icons.Default.Wifi,
            Color(0xFF6C5CE7),
            listOf(Color(0xFF1E1A2E), Color(0xFF14121C), Color(0xFF100E18))
        )

        WatchPartyNotificationType.SESSION_ENDED -> Triple(
            Icons.Default.WifiOff,
            Color(0xFFFFA502),
            listOf(Color(0xFF2A2215), Color(0xFF1C1910), Color(0xFF18140C))
        )

        WatchPartyNotificationType.CONNECTION_SUMMARY -> Triple(
            Icons.Default.People,
            Color(0xFF00CEC9),
            listOf(Color(0xFF152A2A), Color(0xFF101C1C), Color(0xFF0C1818))
        )

        WatchPartyNotificationType.JOIN_ACCEPTED -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF00B894),
            listOf(Color(0xFF152E22), Color(0xFF101E18), Color(0xFF0C1814))
        )

        WatchPartyNotificationType.JOIN_REJECTED -> Triple(
            Icons.Default.Close,
            Color(0xFFFF6B6B),
            listOf(Color(0xFF2E1515), Color(0xFF1E1010), Color(0xFF160C0C))
        )

        WatchPartyNotificationType.REQUEST_EXPIRED -> Triple(
            Icons.Default.AccessTime,
            Color(0xFFFDCB6E),
            listOf(Color(0xFF2A2515), Color(0xFF1C1A10), Color(0xFF18150C))
        )
    }
}
