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
package com.helpofai.videoplayer.feature.library.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helpofai.videoplayer.core.theme.frostedGlass
import com.helpofai.videoplayer.feature.library.MediaScanProgressState
import kotlin.math.roundToInt

/**
 * Advanced, beautiful media scan progress dialog showing real-time scan data.
 * Displays videos found, current file, progress percentage, elapsed/estimated time, and storage info.
 */
@Composable
fun MediaScanProgressDialog(
    scanProgress: MediaScanProgressState,
    onDismiss: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !scanProgress.isScanning,
            dismissOnClickOutside = !scanProgress.isScanning,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .frostedGlass(
                    cornerRadius = 24.dp,
                    surfaceAlpha = 0.85f,
                    surfaceColor = Color.Black
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── Header with Title & Close Button ───────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (scanProgress.isScanning) "Scanning Media Library" else "Scan Complete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    if (!scanProgress.isScanning) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // ─── Status Message ────────────────────────────────────────────────
                Text(
                    text = scanProgress.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                if (scanProgress.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFE53935).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ ${scanProgress.errorMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF5350)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ─── Progress Bar with Percentage ──────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${scanProgress.displayProgress.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00CEC9)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = scanProgress.displayProgress / 100f,
                        animationSpec = tween(300),
                        label = "Progress"
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00CEC9),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Statistics Grid ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Videos Found
                    StatRow(
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00CEC9)) },
                        label = "Videos Found",
                        value = scanProgress.videosFound.toString(),
                        subValue = "Scanned: ${scanProgress.videosScanned}"
                    )

                    // Current Directory
                    if (scanProgress.currentDirectory.isNotEmpty()) {
                        StatRow(
                            icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFA500)) },
                            label = "Current Directory",
                            value = scanProgress.currentDirectory.split("/").lastOrNull() ?: "Storage",
                            subValue = scanProgress.currentDirectory.take(50) + if (scanProgress.currentDirectory.length > 50) "..." else ""
                        )
                    }

                    // Current File
                    if (scanProgress.currentFile.isNotEmpty()) {
                        StatRow(
                            icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF81C784)) },
                            label = "Current File",
                            value = scanProgress.currentFile.substringAfterLast("/").take(40),
                            subValue = if (scanProgress.currentSizeBytes > 0) formatBytes(scanProgress.currentSizeBytes) else "..."
                        )
                    }

                    // Total Size
                    if (scanProgress.totalSizeBytes > 0) {
                        StatRow(
                            icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF64B5F6)) },
                            label = "Total Size Scanned",
                            value = formatBytes(scanProgress.totalSizeBytes),
                            subValue = ""
                        )
                    }

                    // Time Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatSmall(
                            icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF9C27B0)) },
                            label = "Elapsed",
                            value = formatTime(scanProgress.elapsedTimeMs)
                        )

                        StatSmall(
                            icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFFB74D)) },
                            label = "Remaining",
                            value = if (scanProgress.isScanning) formatTime(scanProgress.estimatedRemainingMs) else "Done"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Completion Status ─────────────────────────────────────────────
                if (!scanProgress.isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Scan completed successfully!\nFound ${scanProgress.videosFound} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81C784),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Show loading indicator while scanning
                if (scanProgress.isScanning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(24.dp)) {
                        CircularLoadingIndicator()
                    }
                }
            }
        }
    }
}

/**
 * Stat row displaying icon, label, value, and sub-value.
 */
@Composable
private fun StatRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    subValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subValue.isNotEmpty()) {
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Small stat box for compact display.
 */
@Composable
private fun StatSmall(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(20.dp)) {
                icon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Circular loading spinner animation.
 */
@Composable
private fun CircularLoadingIndicator() {
    val rotation by rememberInfiniteTransition(label = "ScanSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "Loading"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00CEC9),
                        Color(0xFF00CEC9).copy(alpha = 0.3f),
                        Color(0xFF00CEC9)
                    )
                ),
                shape = CircleShape
            )
            .clip(CircleShape)
            .graphicsLayer { rotationZ = rotation }
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Utility Functions
// ─────────────────────────────────────────────────────────────────────────

/**
 * Format bytes to human-readable size (B, KB, MB, GB).
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes <= 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

/**
 * Format milliseconds to HH:MM:SS format.
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
        else -> String.format("%02ds", seconds)
    }
}
