package com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.helpofai.videoplayer.feature.watch_party.session.DevicePermissionPreset
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

private val BgDialog = Color(0xFF121620)
private val BgCardInner = Color(0xFF1A202C)
private val AccentCyan = Color(0xFF00FFCC)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF10B981)
private val WarnAmber = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)
private val BorderColor = Color.White.copy(alpha = 0.08f)

@Composable
fun DevicePermissionDialog(
    device: WatchPartyDevice,
    onDismiss: () -> Unit,
    onSavePermissions: (
        playPause: Boolean,
        seek: Boolean,
        volume: Boolean,
        gestures: Boolean,
        audioTrack: Boolean,
        subtitle: Boolean,
        reactions: Boolean
    ) -> Unit,
    onKickDevice: (() -> Unit)? = null,
    onBanDevice: (() -> Unit)? = null
) {
    var playPause by remember { mutableStateOf(device.hasPlayPausePermission) }
    var seek by remember { mutableStateOf(device.hasSeekPermission) }
    var volume by remember { mutableStateOf(device.hasVolumePermission) }
    var gestures by remember { mutableStateOf(device.hasGesturePermission) }
    var audioTrack by remember { mutableStateOf(device.hasAudioTrackPermission) }
    var subtitle by remember { mutableStateOf(device.hasSubtitlePermission) }
    var reactions by remember { mutableStateOf(device.hasReactionPermission) }

    fun applyPreset(preset: DevicePermissionPreset) {
        when (preset) {
            DevicePermissionPreset.CO_HOST -> {
                playPause = true; seek = true; volume = true; gestures = true; audioTrack = true; subtitle = true; reactions = true
            }
            DevicePermissionPreset.CONTROLLER -> {
                playPause = true; seek = true; volume = true; gestures = true; audioTrack = false; subtitle = false; reactions = true
            }
            DevicePermissionPreset.VIEWER -> {
                playPause = false; seek = false; volume = true; gestures = true; audioTrack = false; subtitle = false; reactions = true
            }
            DevicePermissionPreset.RESTRICTED -> {
                playPause = false; seek = false; volume = false; gestures = false; audioTrack = false; subtitle = false; reactions = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BgDialog,
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Header Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = device.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${device.ipAddress} • Ping ${device.latency}ms",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Role Badge
                    val currentRole = remember(playPause, seek, audioTrack, subtitle, gestures) {
                        when {
                            playPause && seek && audioTrack && subtitle -> "Co-Host"
                            playPause && seek -> "Controller"
                            !playPause && !seek && !gestures -> "Restricted"
                            else -> "Viewer"
                        }
                    }
                    val badgeColor = when (currentRole) {
                        "Co-Host" -> AccentPurple
                        "Controller" -> AccentCyan
                        "Restricted" -> DangerRed
                        else -> AccentGreen
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = currentRole,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                // Quick Presets Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUICK ROLE PRESETS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(
                            label = "Viewer",
                            isSelected = !playPause && !seek && gestures,
                            color = AccentGreen,
                            onClick = { applyPreset(DevicePermissionPreset.VIEWER) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Controller",
                            isSelected = playPause && seek && !audioTrack,
                            color = AccentCyan,
                            onClick = { applyPreset(DevicePermissionPreset.CONTROLLER) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Co-Host",
                            isSelected = playPause && seek && audioTrack && subtitle,
                            color = AccentPurple,
                            onClick = { applyPreset(DevicePermissionPreset.CO_HOST) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Restricted",
                            isSelected = !playPause && !seek && !gestures,
                            color = DangerRed,
                            onClick = { applyPreset(DevicePermissionPreset.RESTRICTED) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Granular Permissions Switches
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCardInner)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    PermissionSwitchRow(
                        icon = Icons.Default.PlayCircle,
                        title = "Play / Pause Sync",
                        subtitle = "Allow device to play or pause current media",
                        checked = playPause,
                        activeColor = AccentCyan,
                        onCheckedChange = { playPause = it }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    PermissionSwitchRow(
                        icon = Icons.Default.FastForward,
                        title = "Seek & Scrubbing",
                        subtitle = "Allow device to scrub and jump timeline",
                        checked = seek,
                        activeColor = AccentCyan,
                        onCheckedChange = { seek = it }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    PermissionSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Local Volume Adjustment",
                        subtitle = "Allow adjusting local player volume level",
                        checked = volume,
                        activeColor = AccentCyan,
                        onCheckedChange = { volume = it }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    PermissionSwitchRow(
                        icon = Icons.Default.TouchApp,
                        title = "Swipe & Touch Gestures",
                        subtitle = "Double tap seeking & vertical volume/brightness swipe",
                        checked = gestures,
                        activeColor = AccentCyan,
                        onCheckedChange = { gestures = it }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    PermissionSwitchRow(
                        icon = Icons.Default.Audiotrack,
                        title = "Audio Track Switching",
                        subtitle = "Allow switching languages / multi-track audio streams",
                        checked = audioTrack,
                        activeColor = AccentPurple,
                        onCheckedChange = { audioTrack = it }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    PermissionSwitchRow(
                        icon = Icons.Default.Subtitles,
                        title = "Subtitle Toggling",
                        subtitle = "Allow turning subtitles on/off and picking tracks",
                        checked = subtitle,
                        activeColor = AccentPurple,
                        onCheckedChange = { subtitle = it }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    PermissionSwitchRow(
                        icon = Icons.Default.EmojiEmotions,
                        title = "Chat & Floating Reactions",
                        subtitle = "Allow sending emoji floating reactions to room",
                        checked = reactions,
                        activeColor = WarnAmber,
                        onCheckedChange = { reactions = it }
                    )
                }

                // Danger Zone: Kick & Ban (if provided)
                if (onKickDevice != null || onBanDevice != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onKickDevice != null) {
                            OutlinedButton(
                                onClick = {
                                    onKickDevice()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kick Guest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (onBanDevice != null) {
                            OutlinedButton(
                                onClick = {
                                    onBanDevice()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ban Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSavePermissions(playPause, seek, volume, gestures, audioTrack, subtitle, reactions)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save & Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Backwards compatibility overload
@Composable
fun DevicePermissionDialog(
    device: WatchPartyDevice,
    onDismiss: () -> Unit,
    onPermissionChange: (Boolean, Boolean, Boolean) -> Unit
) {
    DevicePermissionDialog(
        device = device,
        onDismiss = onDismiss,
        onSavePermissions = { p, s, v, _, _, _, _ ->
            onPermissionChange(p, s, v)
        }
    )
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
        label = "presetBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else BorderColor,
        label = "presetBorder"
    )
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) color else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PermissionSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    activeColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) activeColor else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    color = if (checked) TextPrimary else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
