package com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

@Composable
fun DevicePermissionDialog(
    device: WatchPartyDevice,
    onDismiss: () -> Unit,
    onPermissionChange: (Boolean, Boolean, Boolean) -> Unit
) {
    var playPause by remember { mutableStateOf(device.hasPlayPausePermission) }
    var seek by remember { mutableStateOf(device.hasSeekPermission) }
    var volume by remember { mutableStateOf(device.hasVolumePermission) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions for ${device.name}") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Play/Pause", modifier = Modifier.weight(1f))
                    Switch(checked = playPause, onCheckedChange = { playPause = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Seek", modifier = Modifier.weight(1f))
                    Switch(checked = seek, onCheckedChange = { seek = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Volume", modifier = Modifier.weight(1f))
                    Switch(checked = volume, onCheckedChange = { volume = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onPermissionChange(playPause, seek, volume)
                onDismiss()
            }) {
                Text("Save")
            }
        }
    )
}
