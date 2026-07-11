package com.helpofai.videoplayer.feature.watch_party.ui.invitation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WatchPartyInvitationView(
    hostName: String,
    videoTitle: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Watch Party Invitation", style = MaterialTheme.typography.titleMedium)
            Text("$hostName wants to stream $videoTitle with you.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept) { Text("Accept") }
                OutlinedButton(onClick = onReject) { Text("Reject") }
            }
        }
    }
}
