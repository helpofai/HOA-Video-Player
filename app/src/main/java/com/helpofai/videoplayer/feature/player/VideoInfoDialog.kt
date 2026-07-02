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

@Composable
fun VideoInfoDialog(
    player: Player,
    videoPath: String?,
    onDismissRequest: () -> Unit
) {
    val file = videoPath?.let { File(it) }
    val sizeInMb = file?.let { it.length() / (1024.0 * 1024.0) } ?: 0.0
    val df = DecimalFormat("#.##")

    // Extract codec info from current tracks
    var videoCodec = "Unknown"
    var audioCodec = "Unknown"
    var resolution = "Unknown"
    var bitrate = "Unknown"

    player.currentTracks.groups.forEach { group ->
        if (group.length > 0) {
            val format = group.getTrackFormat(0)
            if (group.type == C.TRACK_TYPE_VIDEO) {
                videoCodec = format.sampleMimeType ?: "Unknown"
                resolution = "${format.width} x ${format.height}"
                if (format.bitrate != androidx.media3.common.Format.NO_VALUE) {
                    bitrate = "${format.bitrate / 1000} kbps"
                }
            } else if (group.type == C.TRACK_TYPE_AUDIO) {
                audioCodec = format.sampleMimeType ?: "Unknown"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Video Information", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow("File Path:", videoPath ?: "Unknown")
                InfoRow("File Size:", "${df.format(sizeInMb)} MB")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                InfoRow("Resolution:", resolution)
                InfoRow("Video Codec:", videoCodec.replace("video/", "").uppercase())
                InfoRow("Audio Codec:", audioCodec.replace("audio/", "").uppercase())
                InfoRow("Bitrate:", bitrate)
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
