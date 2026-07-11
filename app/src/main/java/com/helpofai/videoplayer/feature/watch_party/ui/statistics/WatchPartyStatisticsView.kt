package com.helpofai.videoplayer.feature.watch_party.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WatchPartyStatisticsView(
    averageBitrate: Float,
    jitterMs: Float,
    packetLoss: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Real-Time Stream Stats", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Average Bitrate: ${averageBitrate} Mbps")
        Text("Jitter Rate: ${jitterMs}ms")
        Text("Packet Loss: $packetLoss")
    }
}
