package com.helpofai.videoplayer.feature.filemanager.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.filemanager.StorageStats

@Composable
fun StorageAnalyzerCard(
    stats: StorageStats,
    modifier: Modifier = Modifier
) {
    val usedBytes = stats.totalBytes - stats.freeBytes
    val usedRatio = if (stats.totalBytes > 0) usedBytes.toFloat() / stats.totalBytes else 0f
    
    val videoRatio = if (stats.totalBytes > 0) stats.videoBytes.toFloat() / stats.totalBytes else 0f
    val audioRatio = if (stats.totalBytes > 0) stats.audioBytes.toFloat() / stats.totalBytes else 0f
    val imageRatio = if (stats.totalBytes > 0) stats.imageBytes.toFloat() / stats.totalBytes else 0f
    val otherRatio = if (stats.totalBytes > 0) stats.otherBytes.toFloat() / stats.totalBytes else 0f

    val totalFormatted = formatBytes(stats.totalBytes)
    val freeFormatted = formatBytes(stats.freeBytes)
    val usedPercent = (usedRatio * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Device Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$freeFormatted free of $totalFormatted",
                        fontSize = 11.sp,
                        color = Color(0xFF8E9CB0)
                    )
                }
                Text(
                    text = "$usedPercent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00CEC9)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                if (videoRatio > 0) {
                    Box(modifier = Modifier.fillMaxHeight().weight(videoRatio.coerceAtLeast(0.001f)).background(Color(0xFF7C5CE7)))
                }
                if (audioRatio > 0) {
                    Box(modifier = Modifier.fillMaxHeight().weight(audioRatio.coerceAtLeast(0.001f)).background(Color(0xFF00CEC9)))
                }
                if (imageRatio > 0) {
                    Box(modifier = Modifier.fillMaxHeight().weight(imageRatio.coerceAtLeast(0.001f)).background(Color(0xFF00B894)))
                }
                if (otherRatio > 0) {
                    Box(modifier = Modifier.fillMaxHeight().weight(otherRatio.coerceAtLeast(0.001f)).background(Color(0xFFFFD200)))
                }
                val remainingRatio = 1f - usedRatio
                if (remainingRatio > 0) {
                    Box(modifier = Modifier.fillMaxHeight().weight(remainingRatio.coerceAtLeast(0.001f)).background(Color.Transparent))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageLegendItem(color = Color(0xFF7C5CE7), label = "Videos", size = stats.videoBytes)
                StorageLegendItem(color = Color(0xFF00CEC9), label = "Audio", size = stats.audioBytes)
                StorageLegendItem(color = Color(0xFF00B894), label = "Images", size = stats.imageBytes)
                StorageLegendItem(color = Color(0xFFFFD200), label = "Other", size = stats.otherBytes)
            }
        }
    }
}

@Composable
private fun RowScope.StorageLegendItem(color: Color, label: String, size: Long) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8E9CB0)
            )
        }
        Text(
            text = formatBytes(size),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.1f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
