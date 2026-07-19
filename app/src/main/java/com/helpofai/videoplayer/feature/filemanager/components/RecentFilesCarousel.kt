package com.helpofai.videoplayer.feature.filemanager.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.helpofai.videoplayer.feature.filemanager.FileManagerNode

@Composable
fun RecentFilesCarousel(
    recentFiles: List<FileManagerNode>,
    onFileClick: (FileManagerNode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentFiles.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Recent Files",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recentFiles) { node ->
                Card(
                    modifier = Modifier
                        .width(110.dp)
                        .height(120.dp)
                        .clickable { onFileClick(node) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (node.isVideo && node.file != null) {
                                AsyncImage(
                                    model = node.file,
                                    contentDescription = node.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                val (icon, color) = getRecentFileIconMeta(node)
                                Icon(
                                    imageVector = icon,
                                    contentDescription = node.name,
                                    tint = color,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = node.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = node.formattedSize,
                                fontSize = 9.sp,
                                color = Color(0xFF8E9CB0)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getRecentFileIconMeta(node: FileManagerNode): Pair<ImageVector, Color> {
    val ext = node.file?.extension?.lowercase() ?: ""
    return when {
        node.isAudio || ext in setOf("mp3", "wav", "m4a", "ogg", "flac") -> Pair(Icons.Default.MusicNote, Color(0xFF00CEC9))
        ext in setOf("jpg", "jpeg", "png", "webp", "gif") -> Pair(Icons.Default.Image, Color(0xFF00B894))
        node.isSubtitle || ext in setOf("srt", "vtt", "ass") -> Pair(Icons.Default.Subtitles, Color(0xFFFFD200))
        ext in setOf("zip", "rar", "7z", "tar") -> Pair(Icons.Default.Folder, Color(0xFF7C5CE7))
        else -> Pair(Icons.Default.Description, Color(0xFF8E9CB0))
    }
}
