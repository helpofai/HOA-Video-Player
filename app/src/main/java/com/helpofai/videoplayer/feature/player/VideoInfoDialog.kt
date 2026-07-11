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
package com.helpofai.videoplayer.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import java.io.File
import java.text.DecimalFormat

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun VideoInfoDialog(
    report: com.helpofai.videoplayer.core.playback.diagnostics.MediaAnalyzer.MediaCompatibilityReport?,
    audioReport: com.helpofai.videoplayer.core.playback.diagnostics.AudioQualityAnalyzer.AudioQualityReport?,
    videoPath: String?,
    onDismissRequest: () -> Unit
) {
    val file = videoPath?.let { File(it) }
    val sizeInMb = file?.let { it.length() / (1024.0 * 1024.0) } ?: 0.0
    val df = DecimalFormat("#.##")

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Media Compatibility Report", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow("File Path:", videoPath ?: "Unknown")
                InfoRow("File Size:", "${df.format(sizeInMb)} MB")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                if (report != null) {
                    InfoRow("Container Format:", report.container)
                    InfoRow("Resolution:", "${report.width} x ${report.height} (${report.rotation}° Rotation)")
                    InfoRow("Frame Rate:", String.format("%.1f fps", report.fps))
                    InfoRow("Video Codec:", (report.videoCodec ?: "None").replace("video/", "").uppercase())
                    if (report.videoProfile > 0) {
                        InfoRow("Video Codec Profile / Level:", "Profile ${report.videoProfile} / Level ${report.videoLevel}")
                    }
                    InfoRow("HDR Format:", if (report.isHdr) report.hdrType ?: "Yes" else "None")
                    InfoRow("Audio Track Count:", "${report.audioTrackCount}")
                    InfoRow("Subtitle Track Count:", "${report.subtitleTrackCount}")
                    if (report.languageTracks.isNotEmpty()) {
                        InfoRow("Languages Detected:", report.languageTracks.distinct().joinToString(", ").uppercase())
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    if (audioReport != null) {
                        InfoRow("Audio Codec:", audioReport.codec.replace("audio/", "").uppercase())
                        InfoRow("Audio Layout:", "${audioReport.channelLayout} @ ${audioReport.sampleRate} Hz")
                        InfoRow("Dynamic Range Estimate:", "${audioReport.dynamicRangeDb} dB")
                        InfoRow("Peak Amplitude Estimate:", "${audioReport.peakLevelDbfs} dBFS")
                    }
                    
                    if (report.issues.isNotEmpty() || report.recommendations.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Issues & Suggestions:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        report.issues.forEach { issue ->
                            Text("• $issue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        report.recommendations.forEach { rec ->
                            Text("• $rec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("Loading metadata analyzer...")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
