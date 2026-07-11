package com.helpofai.videoplayer.feature.watch_party.ui.join_room

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice
import com.helpofai.videoplayer.feature.watch_party.ui.qr_scanner.WatchPartyQrScannerScreen
import com.helpofai.videoplayer.feature.watch_party.ui.qr_preview.WatchPartyRoomPreviewScreen
import com.helpofai.videoplayer.feature.watch_party.discovery.WatchPartyDeviceDiscoveryService
import com.helpofai.videoplayer.feature.watch_party.discovery.DiscoveredHost
import com.helpofai.videoplayer.feature.watch_party.notification.WatchPartyNotificationManager

// Colors
private val BgDeep       = Color(0xFF090B10)
private val BgCard       = Color(0xFF111520)
private val AccentPurple = Color(0xFF7C5CE7)
private val AccentCyan   = Color(0xFF00CEC9)
private val AccentGreen  = Color(0xFF00B894)
private val TextPrimary  = Color(0xFFECF0F1)
private val TextSub      = Color(0xFF8E9CB0)
private val DivColor     = Color(0xFF1E2535)
private val WarnAmber    = Color(0xFFFDCB6E)

@Composable
fun WatchPartyJoinRoomScreen(
    videos: List<com.helpofai.videoplayer.core.model.Video>,
    onBack: () -> Unit,
    onJoinSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { WatchPartySessionManager.getInstance() }
    val activeSession by sessionManager.activeSession.collectAsState()

    val streamingVideo = remember(videos) {
        videos.firstOrNull() ?: com.helpofai.videoplayer.core.model.Video(
            id = 9999L,
            title = "Spectacular Big Buck Bunny",
            uri = android.net.Uri.EMPTY,
            path = "",
            duration = 600000,
            size = 104857600,
            dateAdded = System.currentTimeMillis()
        )
    }

    var showScanner by remember { mutableStateOf(false) }
    var scannedData by remember { mutableStateOf<String?>(null) }

    // Manual Form parameters
    var manualRoomId by remember { mutableStateOf("") }
    var manualPassword by remember { mutableStateOf("") }

    val discoveryService = remember { WatchPartyDeviceDiscoveryService() }
    val discoveredHosts by discoveryService.discoveredHosts.collectAsState()

    LaunchedEffect(Unit) {
        discoveryService.startDiscovery()
    }

    val pendingDeepLink by sessionManager.pendingDeepLink.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        if (pendingDeepLink != null) {
            scannedData = pendingDeepLink
            sessionManager.pendingDeepLink.value = null
        }
    }

    // Dynamic Title representation based on active layout
    val pageTitle = when {
        showScanner -> "Scan QR Code"
        scannedData != null -> "Room Preview Details"
        else -> "Join Watch Party Room"
    }

    // Intercept hardware back button to pop views sequentially
    BackHandler {
        when {
            showScanner -> showScanner = false
            scannedData != null -> scannedData = null
            else -> onBack()
        }
    }

    if (showScanner) {
        WatchPartyQrScannerScreen(
            onBack = { showScanner = false },
            onQrDetected = { data ->
                scannedData = data
                showScanner = false
            }
        )
        return
    }

    if (scannedData != null) {
        WatchPartyRoomPreviewScreen(
            roomData = scannedData!!,
            onJoin = { roomId, roomName, hostIp, port, token, videoTitle, videoDuration, videoPath, videoSize ->
                Toast.makeText(context, "Joining room...", Toast.LENGTH_SHORT).show()
                val streamPort = com.helpofai.videoplayer.feature.watch_party.streaming.WatchPartyVideoStreamServer.VIDEO_STREAM_PORT
                val matchedVideo = com.helpofai.videoplayer.core.model.Video(
                    id = 9999L,
                    title = videoTitle ?: "Watch Party Stream",
                    uri = android.net.Uri.parse("http://$hostIp:$streamPort/video"),
                    path = "http_stream",
                    duration = videoDuration ?: 0L,
                    size = videoSize ?: 0L,
                    dateAdded = System.currentTimeMillis()
                )
                sessionManager.isClientMode = true
                sessionManager.createSession(
                    name = roomName,
                    hostIp = hostIp,
                    hostDeviceName = "Host $hostIp",
                    video = matchedVideo,
                    securityToken = token,
                    id = roomId
                )
                WatchPartyNotificationManager.getInstance().notifyJoinAccepted(roomName)
                Toast.makeText(context, "Joined! Connecting to stream...", Toast.LENGTH_LONG).show()
                onJoinSuccess()
            },
            onClose = { scannedData = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toolbar with Back Button & Dynamic Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                pageTitle,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        // Section 1: Connected Room Data
        if (activeSession != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, DivColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Connected Active Room", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Room Name:", color = TextSub, fontSize = 12.sp)
                        Text(activeSession!!.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                    DetailDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Room ID:", color = TextSub, fontSize = 12.sp)
                        Text(activeSession!!.id, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                    DetailDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Host Address:", color = TextSub, fontSize = 12.sp)
                        Text("${activeSession!!.hostIp}:${activeSession!!.port}", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                    DetailDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Session Time:", color = TextSub, fontSize = 12.sp)
                        Text("Created 5m ago", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                    DetailDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Currently Playing Video:", color = TextSub, fontSize = 12.sp)
                        Text(activeSession!!.video?.title ?: "Idle (Waiting for Stream)", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 2: Scan QR to Join option
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, DivColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showScanner = true }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR",
                    tint = AccentCyan,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Scan Host QR Code to Join",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Tapping here opens camera scanner to connect instantly",
                    color = TextSub,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Section 3: Manual Room ID entry
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, DivColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Connect Manually via Room ID",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = manualRoomId,
                    onValueChange = { manualRoomId = it },
                    label = { Text("Room ID", color = TextSub) },
                    placeholder = { Text("e.g. wp_1720894", color = TextSub) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DivColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = manualPassword,
                    onValueChange = { manualPassword = it },
                    label = { Text("Password (Optional)", color = TextSub) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DivColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (manualRoomId.isBlank()) {
                            Toast.makeText(context, "Please enter a valid Room ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        Toast.makeText(context, "Manual join requires scanning the host's QR code or using Network Discovery.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Request to Join Host", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 4: Available & Previously Joined Rooms List
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, DivColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Network Discovered Rooms",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                val (joinedRooms, nonJoinedRooms) = discoveredHosts.partition { host -> 
                    host.name.contains("Home") || host.name.contains("Office")
                }

                if (discoveredHosts.isEmpty()) {
                    Text(
                        "Scanning network for active watch party hosts...",
                        color = TextSub,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }

                if (joinedRooms.isNotEmpty()) {
                    Text("Already Joined Rooms", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    joinedRooms.forEach { room ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(room.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                Text("${room.ipAddress}:${room.port}", color = TextSub, fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    val streamPort = room.streamPort
                                    val streamVideo = com.helpofai.videoplayer.core.model.Video(
                                        id = 9999L,
                                        title = "Watch Party Stream",
                                        uri = android.net.Uri.parse("http://${room.ipAddress}:$streamPort/video"),
                                        path = "http_stream",
                                        duration = 0L,
                                        size = 0L,
                                        dateAdded = System.currentTimeMillis()
                                    )
                                    sessionManager.isClientMode = true
                                    sessionManager.createSession(
                                        name = room.name,
                                        hostIp = room.ipAddress,
                                        hostDeviceName = room.name,
                                        video = streamVideo,
                                        securityToken = "reconnect",
                                        id = room.name,
                                        tunnelPort = room.port
                                    )
                                    WatchPartyNotificationManager.getInstance().notifyJoinAccepted(room.name)
                                    Toast.makeText(context, "Reconnected to ${room.name}!", Toast.LENGTH_SHORT).show()
                                    onJoinSuccess()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.2f), contentColor = AccentGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Rejoin", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    DetailDivider()
                }

                if (nonJoinedRooms.isNotEmpty()) {
                    Text("Available Rooms (New)", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    nonJoinedRooms.forEach { room ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(room.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                Text("${room.ipAddress}:${room.port}", color = TextSub, fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    val streamPort = room.streamPort
                                    val streamVideo = com.helpofai.videoplayer.core.model.Video(
                                        id = 9999L,
                                        title = "Watch Party Stream",
                                        uri = android.net.Uri.parse("http://${room.ipAddress}:$streamPort/video"),
                                        path = "http_stream",
                                        duration = 0L,
                                        size = 0L,
                                        dateAdded = System.currentTimeMillis()
                                    )
                                    sessionManager.isClientMode = true
                                    sessionManager.createSession(
                                        name = room.name,
                                        hostIp = room.ipAddress,
                                        hostDeviceName = room.name,
                                        video = streamVideo,
                                        securityToken = "request",
                                        id = room.name,
                                        tunnelPort = room.port
                                    )
                                    WatchPartyNotificationManager.getInstance().notifyJoinAccepted(room.name)
                                    Toast.makeText(context, "Joined ${room.name}!", Toast.LENGTH_SHORT).show()
                                    onJoinSuccess()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Request Join", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(color = DivColor, thickness = 0.5.dp)
}
