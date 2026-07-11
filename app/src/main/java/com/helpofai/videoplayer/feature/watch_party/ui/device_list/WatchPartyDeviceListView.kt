package com.helpofai.videoplayer.feature.watch_party.ui.device_list

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

@Composable
fun WatchPartyDeviceListView(
    devices: List<WatchPartyDevice>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        devices.forEach { device ->
            Text(
                text = "${device.name} - ${device.status} (Ping: ${device.latency}ms)",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
