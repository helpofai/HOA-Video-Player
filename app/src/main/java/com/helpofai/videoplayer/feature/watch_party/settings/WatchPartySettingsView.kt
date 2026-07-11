package com.helpofai.videoplayer.feature.watch_party.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WatchPartySettingsView(
    maxUsers: Int,
    onMaxUsersChange: (Int) -> Unit,
    sessionPassword: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Watch Party Room Configurations", style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = sessionPassword,
            onValueChange = onPasswordChange,
            label = { Text("Session Password (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Max Connected Guests: $maxUsers")
            Row {
                TextButton(onClick = { if (maxUsers > 2) onMaxUsersChange(maxUsers - 1) }) { Text("-") }
                TextButton(onClick = { if (maxUsers < 20) onMaxUsersChange(maxUsers + 1) }) { Text("+") }
            }
        }
    }
}
