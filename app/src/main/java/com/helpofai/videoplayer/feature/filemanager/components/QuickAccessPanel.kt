package com.helpofai.videoplayer.feature.filemanager.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun QuickAccessPanel(
    pinnedPaths: Set<String>,
    onFolderClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pinnedPaths.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pinnedPaths.take(4).forEach { path ->
                val file = File(path)
                val (name, icon, color) = getFolderMeta(file.name, path)
                
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(76.dp)
                        .clickable { onFolderClick(file) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun getFolderMeta(rawName: String, path: String): Triple<String, ImageVector, Color> {
    val lower = rawName.lowercase()
    val pathLower = path.lowercase()
    return when {
        lower.contains("download") -> Triple("Downloads", Icons.Default.Download, Color(0xFF00CEC9))
        lower.contains("camera") || pathLower.contains("dcim/camera") -> Triple("Camera", Icons.Default.PhotoCamera, Color(0xFF00B894))
        lower.contains("movies") || lower.contains("video") -> Triple("Movies", Icons.Default.Movie, Color(0xFF7C5CE7))
        lower.contains("music") || lower.contains("audio") -> Triple("Music", Icons.Default.MusicNote, Color(0xFFFFD200))
        else -> Triple(rawName, Icons.Default.Folder, Color(0xFF8E9CB0))
    }
}
