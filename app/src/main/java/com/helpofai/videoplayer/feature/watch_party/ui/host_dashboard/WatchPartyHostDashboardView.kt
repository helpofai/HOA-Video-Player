package com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice
import com.helpofai.videoplayer.feature.watch_party.ui.permissions.WatchPartyPermissionsView
import com.helpofai.videoplayer.feature.watch_party.ui.quality.WatchPartyQualityView
import com.helpofai.videoplayer.feature.watch_party.ui.statistics.WatchPartyStatisticsView
import com.helpofai.videoplayer.feature.watch_party.settings.WatchPartySettingsView
import com.helpofai.videoplayer.feature.watch_party.qr_pairing.WatchPartyQrPairingManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun WatchPartyHostDashboardView(
    session: WatchPartySession,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onKickDevice: (String) -> Unit,
    onCloseSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Configurable parameters inside dialogs
    var currentResolution by remember { mutableStateOf("1080P (Original)") }
    var maxUsers by remember { mutableStateOf(session.maxUsers) }
    var sessionPassword by remember { mutableStateOf("") }
    
    val qrPairingManager = remember { WatchPartyQrPairingManager() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Session Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(session.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(session.video?.title ?: "No Media Selected", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            }
            
            Button(
                onClick = onCloseSession,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red)
            ) {
                Text("End Session", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        
        // Host Playback Commands
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.02f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Host Playback Sync Controls", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayPauseToggle,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (session.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (session.isPlaying) "Pause Party" else "Resume Party", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { onSeekTo(session.currentPositionMs + 30000L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Skip 30s", fontSize = 11.sp)
                    }
                }
            }
        }
        
        // Feature Sub-Pages Quick Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Permissions Button
            Button(
                onClick = { showPermissionsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Perms", fontSize = 9.sp)
            }
            
            // Quality Button
            Button(
                onClick = { showQualityDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Quality", fontSize = 9.sp)
            }
            
            // Diagnostics Button
            Button(
                onClick = { showStatsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stats", fontSize = 9.sp)
            }
            
            // QR Code Pairing Button
            Button(
                onClick = { showQrDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("QR", fontSize = 9.sp)
            }
            
            // Settings Button
            Button(
                onClick = { showSettingsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Setup", fontSize = 9.sp)
            }
        }
        
        // Connected Viewers List
        Text("Party Guests (${session.devices.size - 1})", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
        
        val guestList = session.devices.filterNot { it.isHost }
        if (guestList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No guests connected yet. Share QR code to invite.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(guestList, key = { it.id }) { device ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(device.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text("IP: ${device.ipAddress} • Ping: ${device.latency}ms • Battery: ${device.batteryLevel}%", fontSize = 9.sp, color = Color.Gray)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (device.status) {
                                        "Playing" -> Color.Green.copy(alpha = 0.15f)
                                        "Buffering" -> Color.Yellow.copy(alpha = 0.15f)
                                        else -> Color.LightGray.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        device.status,
                                        fontSize = 8.sp,
                                        color = when (device.status) {
                                            "Playing" -> Color.Green
                                            "Buffering" -> Color.Yellow
                                            else -> Color.LightGray
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                IconButton(onClick = { onKickDevice(device.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Kick", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 1. Permissions Dialog Sub-Page
    if (showPermissionsDialog) {
        Dialog(onDismissRequest = { showPermissionsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Client Playback Control Rights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    val guests = session.devices.filterNot { it.isHost }
                    if (guests.isEmpty()) {
                        Text("No active clients to configure", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        guests.forEach { guest ->
                            WatchPartyPermissionsView(
                                device = guest,
                                onPermissionChange = { play, seek ->
                                    // Normally updates state, triggers mock toast representation
                                    Toast.makeText(context, "Permissions updated for ${guest.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    Button(
                        onClick = { showPermissionsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    // 2. Quality Dialog Sub-Page
    if (showQualityDialog) {
        Dialog(onDismissRequest = { showQualityDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WatchPartyQualityView(
                        currentResolution = currentResolution,
                        onResolutionSelected = {
                            currentResolution = it
                            Toast.makeText(context, "Broadcasting changed to $it", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Button(
                        onClick = { showQualityDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    // 3. Statistics Dialog Sub-Page
    if (showStatsDialog) {
        Dialog(onDismissRequest = { showStatsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WatchPartyStatisticsView(
                        averageBitrate = 42.8f,
                        jitterMs = 1.4f,
                        packetLoss = "0.02%"
                    )
                    Button(
                        onClick = { showStatsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    // 4. Pairing QR Dialog Sub-Page
    if (showQrDialog) {
        val payload = qrPairingManager.generatePairingPayload(session.id, session.hostIp, session.port, session.securityToken)
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Session Dynamic QR Pairing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = Color.White, modifier = Modifier.size(150.dp))
                    Text("Pairing Payload Link:\n$payload", style = MaterialTheme.typography.bodySmall, color = Color.LightGray, textAlign = TextAlign.Center)
                    Button(
                        onClick = { showQrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss Code", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    // 5. Room Settings Configuration Dialog Sub-Page
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WatchPartySettingsView(
                        maxUsers = maxUsers,
                        onMaxUsersChange = { maxUsers = it },
                        sessionPassword = sessionPassword,
                        onPasswordChange = { sessionPassword = it }
                    )
                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Configs", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
