package com.helpofai.videoplayer.feature.watch_party.ui

import com.helpofai.videoplayer.core.theme.ToolIconPalette
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.theme.frostedGlass
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice
import com.helpofai.videoplayer.feature.watch_party.host.WatchPartyHostManager
import com.helpofai.videoplayer.feature.watch_party.client.WatchPartyClientManager
import com.helpofai.videoplayer.feature.watch_party.qr_pairing.WatchPartyQrPairingManager
import com.helpofai.videoplayer.feature.watch_party.discovery.WatchPartyDeviceDiscoveryService
import com.helpofai.videoplayer.feature.watch_party.discovery.DiscoveredHost
import com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard.WatchPartyHostDashboard
import com.helpofai.videoplayer.feature.watch_party.ui.client_dashboard.WatchPartyClientDashboard
import com.helpofai.videoplayer.feature.watch_party.ui.session_browser.WatchPartySessionBrowserView
import com.helpofai.videoplayer.feature.watch_party.ui.join_room.WatchPartyJoinRoomScreen
import com.helpofai.videoplayer.feature.watch_party.ui.host_setup.WatchPartyHostRoomSetupScreen
import com.helpofai.videoplayer.feature.watch_party.settings.WatchPartySettingsView
import com.helpofai.videoplayer.feature.watch_party.ui.connection_status.WatchPartyConnectionStatusSection
import com.helpofai.videoplayer.feature.watch_party.background.WatchPartyBackgroundService
import com.helpofai.videoplayer.feature.watch_party.networking.WatchPartyConnectionPreferences
import com.helpofai.videoplayer.feature.watch_party.ui.guide.WatchPartyGuideDialog
import java.net.NetworkInterface
import java.util.Collections

@Composable
fun WatchPartyMainTab(
    videos: List<Video>,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onVideoClick: (Video) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionManager = remember { WatchPartySessionManager.getInstance() }
    val prefs = remember { WatchPartyConnectionPreferences.getInstance(context) }
    val hostManager = remember { WatchPartyHostManager(sessionManager) }
    val clientManager = remember { WatchPartyClientManager(sessionManager) }
    val qrPairingManager = remember { WatchPartyQrPairingManager() }
    val discoveryService = remember { WatchPartyDeviceDiscoveryService() }
    
    val activeSession by sessionManager.activeSession.collectAsState()

    // Background service management
    LaunchedEffect(activeSession, prefs.backgroundKeepAlive) {
        if (activeSession != null && prefs.backgroundKeepAlive) {
            WatchPartyBackgroundService.start(context)
        } else {
            WatchPartyBackgroundService.stop(context)
        }
    }
    
    val discoveredHosts by discoveryService.discoveredHosts.collectAsState()
    
    // Navigation state — which sub-page to show
    var showHostSetup by remember { mutableStateOf(false) }
    var showJoinRoom by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    val isClientMode by sessionManager.isClientModeFlow.collectAsState()
    var showActiveRoom by remember { mutableStateOf(true) }

    val pendingDeepLink by sessionManager.pendingDeepLink.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        if (pendingDeepLink != null) {
            sessionManager.isClientMode = true
            showJoinRoom = true
            showActiveRoom = true
        }
    }
    
    // Auto discovery trigger
    LaunchedEffect(isClientMode) {
        if (isClientMode) {
            discoveryService.startDiscovery()
        } else {
            discoveryService.stopDiscovery()
        }
    }
    
    if (showHostSetup) {
        WatchPartyHostRoomSetupScreen(
            paddingValues = paddingValues,
            videos = videos,
            onBack = { showHostSetup = false },
            onRoomCreated = {
                showHostSetup = false
                showActiveRoom = true
            }
        )
        return
    }

    if (showJoinRoom) {
        WatchPartyJoinRoomScreen(
            paddingValues = paddingValues,
            videos = videos,
            onBack = { showJoinRoom = false },
            onJoinSuccess = {
                showJoinRoom = false
                sessionManager.isClientMode = true
                showActiveRoom = true
            }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        if (activeSession != null && showActiveRoom) {
            val session = activeSession!!
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isClientMode) {
                    // Show Host Dashboard
                    WatchPartyHostDashboard(
                        session = session,
                        discoveredHosts = discoveredHosts,
                        paddingValues = paddingValues,
                        onKickDevice = { id -> 
                            hostManager.kickDevice(id)
                            Toast.makeText(context, "Kicked device", Toast.LENGTH_SHORT).show()
                        },
                        onBanDevice = { id -> 
                            hostManager.banDevice(id)
                            Toast.makeText(context, "Banned device", Toast.LENGTH_SHORT).show()
                        },
                        onUnbanDevice = { id -> 
                            hostManager.unbanDevice(id)
                            Toast.makeText(context, "Unbanned device", Toast.LENGTH_SHORT).show()
                        },
                        onUpdatePermissions = { id, p, s, v ->
                            sessionManager.setDevicePermission(id, p, s, v)
                            Toast.makeText(context, "Permissions updated", Toast.LENGTH_SHORT).show()
                        },
                        onOpenPlayer = onVideoClick,
                        onEndRoom = {
                            sessionManager.endSession()
                            Toast.makeText(context, "Watch Party room ended", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { showActiveRoom = false }
                    )
                } else {
                    // Show Client Dashboard
                    WatchPartyClientDashboard(
                        session = session,
                        videos = videos,
                        syncStatus = if (session.isPlaying) "Synchronized (Playing)" else "Paused",
                        paddingValues = paddingValues,
                        onDisconnect = {
                            clientManager.disconnect()
                            sessionManager.endSession()
                            sessionManager.isClientMode = false
                            Toast.makeText(context, "Disconnected from watch party room", Toast.LENGTH_SHORT).show()
                        },
                        onOpenPlayer = onVideoClick,
                        onBack = { showActiveRoom = false }
                    )
                }
            }
        } else {
            // Setup Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding() + 8.dp))
                WatchPartyConnectionStatusSection()

                if (activeSession != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(cornerRadius = 16.dp, surfaceAlpha = 0.2f, surfaceColor = Color.Black)
                            .clickable { showActiveRoom = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CellTower, null, tint = ToolIconPalette.WatchParty)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Active Room: ${activeSession!!.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("Tap to return to active dashboard", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = ToolIconPalette.WatchParty)
                        }
                    }
                }

                // Mode Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .frostedGlass(cornerRadius = 16.dp, surfaceAlpha = 0.2f, surfaceColor = Color.Black)
                            .clickable {
                                sessionManager.isClientMode = false
                                showHostSetup = true
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = ToolIconPalette.WatchParty, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Host Room", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    
                    // Join Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .frostedGlass(cornerRadius = 16.dp, surfaceAlpha = 0.2f, surfaceColor = Color.Black)
                            .clickable {
                                showJoinRoom = true
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = ToolIconPalette.Network, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Join Room", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Visual Guide Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 16.dp, surfaceAlpha = 0.25f, surfaceColor = Color.Black)
                        .clickable { showGuideDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(Color(0xFF7C5CE7), Color(0xFF00CEC9))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Watch Party Visual Guide",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "How to host, join, stream & offline sync",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Visual Guide",
                            tint = Color(0xFF00CEC9)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
            }
        }

        if (showGuideDialog) {
            WatchPartyGuideDialog(onDismiss = { showGuideDialog = false })
        }
    }
}

data class WatchPartyInvitationDetails(val hostName: String, val videoTitle: String, val resolution: String, val codec: String, val duration: String)