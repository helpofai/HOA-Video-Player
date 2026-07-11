/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
| Unauthorized copying, modification, redistribution, reverse engineering,
| decompilation, or commercial use of this source code, in whole or in part,
| is strictly prohibited without prior written permission from the copyright owner.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
| Location    : Basta Purba Para, Aranghata, Nadia, West Bengal, India
|
| This source code contains proprietary and confidential information.
| Any unauthorized access or distribution may violate applicable copyright laws.
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.playback.PlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDiagnosticsSheet(
    state: PlaybackState,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Stream Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Diagnostic stats grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiagnosticItem("Current Decoder", state.currentDecoderName)
                DiagnosticItem("Decoder Type", if (state.isHardwareDecoder) "Hardware (Efficient)" else "Software (High CPU)")
                DiagnosticItem("Video Codec", state.videoCodec)
                DiagnosticItem("Audio Codec", state.audioCodec)
                DiagnosticItem("Resolution", "${state.videoWidth} x ${state.videoHeight}")
                DiagnosticItem("FPS", String.format("%.1f fps", state.videoFps))
                DiagnosticItem("Bitrate", if (state.videoBitrate > 0) "${state.videoBitrate / 1000} Kbps" else "Unknown")
                DiagnosticItem("HDR Type", if (state.isHdr) "Active (ST2084 / Dolby)" else "None")
                DiagnosticItem("Dropped Frames", "${state.droppedFrames}")
                DiagnosticItem("Buffer Health", String.format("%.1f sec", (state.bufferedPosition - state.currentPosition).coerceAtLeast(0L) / 1000.0))
                DiagnosticItem("Playback Stability", state.playbackStability)
                DiagnosticItem("Compatibility Status", state.compatibilityStatus)
            }

            // Fallback Events Section
            if (state.fallbackEvents.isNotEmpty()) {
                Text(
                    text = "Fallback Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x1F2196F3),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.fallbackEvents.forEach { event ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(event, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Warnings and recommendations
            val warnings = mutableListOf<String>()
            val recs = mutableListOf<String>()

            if (state.droppedFrames > 20) {
                warnings.add("Heavy frame drops detected (${state.droppedFrames} frames)")
                recs.add("Close other background processes to free up hardware rendering queues.")
            }
            if (!state.isHardwareDecoder) {
                warnings.add("Software decoder is active")
                recs.add("This consumes more battery. Ensure your device is connected to a power source.")
            }

            if (warnings.isNotEmpty()) {
                Text(
                    text = "Warnings & Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x1FFFFB300),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    warnings.forEachIndexed { index, warning ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(warning, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                if (index < recs.size) {
                                    Text(recs[index], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
