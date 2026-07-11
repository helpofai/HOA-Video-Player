package com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession

/**
 * A compact view of the Watch Party session for the PlayerScreen tools.
 */
@Composable
fun WatchPartyPlayerMonitoringView(
    session: WatchPartySession
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Watch Party: ${session.devices.size} Users Joined", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(session.devices) { device ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(device.name, color = Color.LightGray)
                    Text("${device.status} | ${session.currentPositionMs / 1000}s", color = Color.Cyan, fontSize = 12.sp)
                }
            }
        }
    }
}
