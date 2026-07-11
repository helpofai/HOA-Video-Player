package com.helpofai.videoplayer.feature.watch_party.ui.quality

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WatchPartyQualityView(
    currentResolution: String,
    onResolutionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Stream Resolution Quality", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        listOf("1080P (Original)", "720P (HD)", "480P (SD)", "360P (LQ)").forEach { resolution ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(resolution)
                RadioButton(
                    selected = currentResolution == resolution,
                    onClick = { onResolutionSelected(resolution) }
                )
            }
        }
    }
}
