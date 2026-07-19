package com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import com.helpofai.videoplayer.feature.watch_party.qr.generator.WatchPartyQrGenerator
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice
import com.helpofai.videoplayer.feature.watch_party.notification.WatchPartyNotificationManager
import com.helpofai.videoplayer.feature.watch_party.notification.WatchPartyNotification
import com.helpofai.videoplayer.feature.watch_party.notification.WatchPartyNotificationType

// Color Scheme
private val BgDeep       = Color.Transparent
private val BgCard       = Color(0xFF111520)
private val AccentPurple = Color(0xFF7C5CE7)
private val AccentCyan   = Color(0xFF00CEC9)
private val AccentGreen  = Color(0xFF00B894)
private val TextPrimary  = Color(0xFFECF0F1)
private val TextSub      = Color(0xFF8E9CB0)
private val DivColor     = Color(0xFF1E2535)
private val WarnAmber    = Color(0xFFFDCB6E)

@Composable
fun WatchPartyHostDashboard(
    session: WatchPartySession,
    discoveredHosts: List<com.helpofai.videoplayer.feature.watch_party.discovery.DiscoveredHost>,
    onKickDevice: (String) -> Unit,
    onBanDevice: (String) -> Unit,
    onUnbanDevice: (String) -> Unit,
    onUpdatePermissions: (String, Boolean, Boolean, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { WatchPartySessionManager.getInstance() }
    
    val accepted = session.devices.filter { !it.isBanned }
    val banned = session.devices.filter { it.isBanned }

    var showEditDialog by remember { mutableStateOf(false) }

    val videoParam = session.video?.let {
        "&videoTitle=${java.net.URLEncoder.encode(it.title, "UTF-8")}&videoDuration=${it.duration}&videoPath=${java.net.URLEncoder.encode(it.path, "UTF-8")}&videoSize=${it.size}"
    } ?: ""
    val joinLink = "vidplay://join?roomId=${session.id}&hostIp=${session.hostIp}&port=${session.port}&token=${session.securityToken}&roomName=${java.net.URLEncoder.encode(session.name, "UTF-8")}$videoParam"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Room Identity, Back Navigation & Edit Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Setup",
                        tint = TextPrimary
                    )
                }
                Column {
                    val dynamicSub = if (session.isPlaying) "Streaming: ${session.video?.title ?: "Active media"}" else "Room Idle"
                    Text(
                        dynamicSub,
                        color = if (session.isPlaying) AccentGreen else WarnAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        session.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Button(
                onClick = { showEditDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Edit, "Edit Room", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit Room", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Room Data Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, DivColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Room Configuration Data", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Room ID:", color = TextSub, fontSize = 12.sp)
                    Text(session.id, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
                HorizontalDivider(color = DivColor, thickness = 0.5.dp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Security Mode:", color = TextSub, fontSize = 12.sp)
                    Text(
                        if (session.usePassword) "Password Protected (${session.password})" else "Open Room (No Password)",
                        color = if (session.usePassword) WarnAmber else AccentGreen,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
                HorizontalDivider(color = DivColor, thickness = 0.5.dp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max Connections Limit:", color = TextSub, fontSize = 12.sp)
                    Text("${session.maxUsers} guests", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
                HorizontalDivider(color = DivColor, thickness = 0.5.dp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allowed Client Controls:", color = TextSub, fontSize = 12.sp)
                    Column(horizontalAlignment = Alignment.End) {
                        if (session.allowPlayPause) Text("Play/Pause", color = AccentGreen, fontSize = 10.sp)
                        if (session.allowSeek) Text("Scrubbing/Seek", color = AccentGreen, fontSize = 10.sp)
                        if (session.allowVolume) Text("Volume Control", color = AccentGreen, fontSize = 10.sp)
                        if (session.allowNextPrev) Text("Next/Prev Video", color = AccentGreen, fontSize = 10.sp)
                        if (session.allowGestures) Text("Gesture Controls", color = AccentGreen, fontSize = 10.sp)
                        if (session.allowReactions) Text("Emojis reactions", color = AccentGreen, fontSize = 10.sp)
                    }
                }
            }
        }

        // QR Code Preview Section
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, DivColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Scan to Join • Dynamic QR Code",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                
                val qrBitmap = remember(joinLink) { WatchPartyQrGenerator.generateQrBitmap(joinLink, 400) }
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Room QR Code",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )
                }

                Text(
                    "Point client camera to this code for automatic setup",
                    color = TextSub,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy Link Button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Watch Party Link", joinLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Join link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Join Link", fontSize = 11.sp)
                    }

                    // Share Link Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Join my Watch Party")
                                putExtra(Intent.EXTRA_TEXT, "Hey, join my watch party in VidPlay using this link: $joinLink")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Join Link"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Join Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Send Join Request to Nearby Subnet Devices
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, DivColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Send Join Invitation (Same Network / Hotspot)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                
                if (discoveredHosts.isEmpty()) {
                    Text(
                        "No other active devices discovered on your network subnet.",
                        color = TextSub,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    discoveredHosts.forEach { host ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(host.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                Text("${host.ipAddress}:${host.port}", color = TextSub, fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    sendJoinInvitationRequest(context, host.name, joinLink)
                                    Toast.makeText(context, "Invitation request sent to ${host.name}", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2535), contentColor = AccentCyan),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Send Link", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Active Connected Guests list
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, DivColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Connected Guests (${accepted.size - 1})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                
                val guestList = accepted.filterNot { it.isHost }
                if (guestList.isEmpty()) {
                    Text(
                        "No guests connected yet. Invite using QR code or links.",
                        color = TextSub,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    guestList.forEach { guest ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(guest.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("IP: ${guest.ipAddress} • Ping: ${guest.latency}ms", color = TextSub, fontSize = 10.sp)
                            }
                            
                            IconButton(onClick = { onKickDevice(guest.id) }) {
                                Icon(Icons.Default.Delete, "Kick", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }

        // Banned Users List
        if (banned.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, DivColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Banned Users (${banned.size})", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    banned.forEach { guest ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(guest.name, color = TextPrimary, fontSize = 12.sp)
                            IconButton(onClick = { onUnbanDevice(guest.id) }) {
                                Icon(Icons.Default.Check, "Unban", tint = AccentGreen)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Room Details Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(session.name) }
        var editUsePassword by remember { mutableStateOf(session.usePassword) }
        var editPassword by remember { mutableStateOf(session.password) }
        var editMaxUsers by remember { mutableStateOf(session.maxUsers.toFloat()) }
        
        var editPlayPause by remember { mutableStateOf(session.allowPlayPause) }
        var editSeek by remember { mutableStateOf(session.allowSeek) }
        var editVolume by remember { mutableStateOf(session.allowVolume) }
        var editNextPrev by remember { mutableStateOf(session.allowNextPrev) }
        var editGestures by remember { mutableStateOf(session.allowGestures) }
        var editReactions by remember { mutableStateOf(session.allowReactions) }

        Dialog(onDismissRequest = { showEditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BgCard,
                border = BorderStroke(1.dp, DivColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Edit Room Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Room Name", color = TextSub) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DivColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require Password", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = editUsePassword,
                            onCheckedChange = { editUsePassword = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentPurple)
                        )
                    }

                    if (editUsePassword) {
                        OutlinedTextField(
                            value = editPassword,
                            onValueChange = { editPassword = it },
                            label = { Text("Password", color = TextSub) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = DivColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Max connections slider
                    Column {
                        Text("Maximum Guests: ${editMaxUsers.toInt()}", color = TextPrimary, fontSize = 13.sp)
                        Slider(
                            value = editMaxUsers,
                            onValueChange = { editMaxUsers = it },
                            valueRange = 1f..30f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentPurple,
                                activeTrackColor = AccentPurple,
                                inactiveTrackColor = DivColor
                            )
                        )
                    }

                    Text("Client Control Options", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    // Control toggles
                    PermissionRow("Play / Pause Controls", editPlayPause) { editPlayPause = it }
                    PermissionRow("Seek / Scrubbing Controls", editSeek) { editSeek = it }
                    PermissionRow("Local Volume Adjustment", editVolume) { editVolume = it }
                    PermissionRow("Next / Previous Controls", editNextPrev) { editNextPrev = it }
                    PermissionRow("Gesture Controls", editGestures) { editGestures = it }
                    PermissionRow("Emoji Reactions Support", editReactions) { editReactions = it }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, DivColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                sessionManager.updateSession(
                                    name = editName,
                                    usePassword = editUsePassword,
                                    password = editPassword,
                                    maxUsers = editMaxUsers.toInt(),
                                    allowPlayPause = editPlayPause,
                                    allowSeek = editSeek,
                                    allowVolume = editVolume,
                                    allowNextPrev = editNextPrev,
                                    allowGestures = editGestures,
                                    allowReactions = editReactions,
                                    allowSubtitleToggle = session.allowSubtitleToggle,
                                    allowAudioTrack = session.allowAudioTrack,
                                    allowFolderQueue = session.allowFolderQueue
                                )
                                showEditDialog = false
                                Toast.makeText(context, "Room settings updated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextSub, fontSize = 12.sp)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
        )
    }
}

// Global In-app + OS Notification Mock Dispatcher
private fun sendJoinInvitationRequest(context: Context, deviceName: String, link: String) {
    // 1. Send in-app global notification via WatchPartyNotificationManager
    val globalNotification = WatchPartyNotification(
        type = WatchPartyNotificationType.DEVICE_CONNECTED,
        title = "Invitation Shared",
        message = "Watch Party invite code shared with $deviceName.",
        autoDismissMs = 4000L
    )
    WatchPartyNotificationManager.getInstance().push(globalNotification)

    // 2. Dispatch a device system notification representing incoming invitation
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "watch_party_invitations"
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Watch Party Invitations",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming join invitation requests"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    
    val pendingIntent = android.app.PendingIntent.getActivity(
        context,
        0,
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle("Join Watch Party Invite")
        .setContentText("You received an invitation from host to join the room!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}
