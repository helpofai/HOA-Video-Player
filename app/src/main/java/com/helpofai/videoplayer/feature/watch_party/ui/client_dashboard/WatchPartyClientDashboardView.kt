package com.helpofai.videoplayer.feature.watch_party.ui.client_dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession

@Composable
fun WatchPartyClientDashboardView(
    session: WatchPartySession,
    syncStatus: String,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var volume by remember { mutableStateOf(0.7f) }
    var brightness by remember { mutableStateOf(0.6f) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Client Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Client Stream Panel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Room: ${session.name}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            }
            
            IconButton(onClick = onDisconnect) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Leave", tint = Color.Red)
            }
        }
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        
        // Sync Status Bar
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.02f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                    Text("Sync Position:", fontSize = 11.sp, color = Color.LightGray)
                }
                Text(syncStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        // Client Specific Adjustments (Sliders only affect local device)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Local Device Playback Tuner", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                // Volume slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Text("Local Volume", fontSize = 10.sp, color = Color.LightGray)
                        }
                        Text(String.format("%.0f%%", volume * 100), fontSize = 10.sp, color = Color.White)
                    }
                    Slider(value = volume, onValueChange = { volume = it }, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                }
                
                // Brightness slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Text("Local Brightness", fontSize = 10.sp, color = Color.LightGray)
                        }
                        Text(String.format("%.0f%%", brightness * 100), fontSize = 10.sp, color = Color.White)
                    }
                    Slider(value = brightness, onValueChange = { brightness = it }, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                }
            }
        }
        
        // Host & Peer Info
        Text("Room Host & Active Peers", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.02f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Host Device Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Host Device (Master Sync)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("IP Endpoint: ${session.hostIp}:${session.port}", fontSize = 9.sp, color = Color.Gray)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                        Text("Active Streaming", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                
                // Client Device List
                session.devices.forEach { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(device.name + if (device.id == "client_id") " (My Device)" else "", fontSize = 11.sp, color = Color.White)
                            Text("Ping: ${device.latency}ms • Speed: ${device.connectionSpeed} Mbps • Battery: ${device.batteryLevel}%", fontSize = 9.sp, color = Color.Gray)
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(4.dp),
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
                    }
                }
            }
        }
        
        Text("Wait for host instructions. Playback, seeks, and subtitle overlays are synchronized with the room automatically.", fontSize = 9.sp, color = Color.Gray)
    }
}
