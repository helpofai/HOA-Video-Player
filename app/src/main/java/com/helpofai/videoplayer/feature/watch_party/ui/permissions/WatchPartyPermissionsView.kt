package com.helpofai.videoplayer.feature.watch_party.ui.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

@Composable
fun WatchPartyPermissionsView(
    device: WatchPartyDevice,
    onPermissionChange: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(device.name, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(
                checked = device.hasPlayPausePermission,
                onCheckedChange = { onPermissionChange(it, device.hasSeekPermission) }
            )
            Checkbox(
                checked = device.hasSeekPermission,
                onCheckedChange = { onPermissionChange(device.hasPlayPausePermission, it) }
            )
        }
    }
}
