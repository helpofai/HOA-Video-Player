package com.helpofai.videoplayer.feature.workspace.transfers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TransferControlsView(
    modifier: Modifier = Modifier
) {
    // UI Transparency settings
    var uiOpacity by remember { mutableFloatStateOf(0.5f) }
    var blurIntensity by remember { mutableFloatStateOf(16f) }
    
    // Performance limiters
    var speedLimit by remember { mutableFloatStateOf(80f) }
    var maxConnections by remember { mutableFloatStateOf(3f) }
    var encryptTransfers by remember { mutableStateOf(true) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Transfer Control & Transparency",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Configure server limits, view real-time socket statistics, and adjust UI overlay opacity settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        // 1. UI Transparency Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E222B).copy(alpha = uiOpacity),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UI Transparency & Blurs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Card Background Opacity", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format("%.0f%%", uiOpacity * 100f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = uiOpacity,
                        onValueChange = { uiOpacity = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Background Blur Radius", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text("${blurIntensity.toInt()}dp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = blurIntensity,
                        onValueChange = { blurIntensity = it },
                        valueRange = 4f..32f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        
        // 2. Bandwidth & Socket limiters
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E222B).copy(alpha = uiOpacity),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bandwidth & Performance Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Maximum Bandwidth Limit", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(if (speedLimit >= 100f) "Uncapped" else "${speedLimit.toInt()} MB/s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = speedLimit,
                        onValueChange = { speedLimit = it },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Simultaneous File Connections", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text("${maxConnections.toInt()} stream(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = maxConnections,
                        onValueChange = { maxConnections = it },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Force Secure Channel", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("Encrypt transfer streams with AES-GCM 256", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(
                        checked = encryptTransfers,
                        onCheckedChange = { encryptTransfers = it }
                    )
                }
            }
        }
        
        // 3. Socket Connection & Diagnostic Transparency
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E222B).copy(alpha = uiOpacity),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Real-Time Socket Statistics", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                DiagnosticItem("wlan0 Interface IP", "192.168.1.100")
                DiagnosticItem("p2p0 (Wi-Fi Direct)", "192.168.49.1 (Active)")
                DiagnosticItem("Socket State", "LISTENING (Port 8080, 5050)")
                DiagnosticItem("Active Handshake Encryption", if (encryptTransfers) "TLS 1.3 / ECHD" else "None (Cleartext)")
                DiagnosticItem("Round-trip Time (Ping)", "4ms (Ultrafast Local Link)")
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
}
