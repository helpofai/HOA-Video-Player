package com.helpofai.videoplayer.feature.watch_party.ui.session_browser

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.watch_party.discovery.DiscoveredHost

@Composable
fun WatchPartySessionBrowserView(
    discoveredSessions: List<DiscoveredHost>,
    onScanQrClick: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    onJoinSession: (DiscoveredHost) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Discover Watch Parties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = onRefreshDiscovery) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
            }
        }
        
        // Scan QR Action Call
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onScanQrClick() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scan Watch Party Host QR Code", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        Text("Local Direct Rooms", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
        
        if (discoveredSessions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Searching for watch party nodes in network...", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(discoveredSessions) { host ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.clickable { onJoinSession(host) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(host.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("IP Endpoint: ${host.ipAddress}:${host.port}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Button(
                                onClick = { onJoinSession(host) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                            ) {
                                Text("Join", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
