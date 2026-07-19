package com.helpofai.videoplayer.feature.filemanager.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.helpofai.videoplayer.feature.filemanager.FileManagerNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlinePreviewSheet(
    node: FileManagerNode,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = node.file?.extension?.lowercase() ?: ""
    val isText = ext in setOf("txt", "srt", "vtt", "ass", "xml", "json", "html", "css", "log", "csv")
    val isImage = ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    val isAudio = ext in setOf("mp3", "wav", "m4a", "ogg", "flac") || node.isAudio

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161A26),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isImage -> {
                        AsyncImage(
                            model = node.file,
                            contentDescription = node.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    isText -> {
                        var textContent by remember { mutableStateOf("Loading content...") }
                        LaunchedEffect(node.file) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val content = node.file?.readText(Charsets.UTF_8) ?: "Empty File"
                                    textContent = if (content.length > 5000) {
                                        content.take(5000) + "\n\n[Content truncated for performance...]"
                                    } else {
                                        content
                                    }
                                } catch (e: Exception) {
                                    textContent = "Could not load file text content: ${e.message}"
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = textContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    isAudio && node.file != null -> {
                        AudioPlayerPreview(file = node.file)
                    }
                    else -> {
                        Text(
                            text = "No preview available for .${ext} files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AudioPlayerPreview(file: File) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }

    LaunchedEffect(file) {
        val player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
        }
        mediaPlayer = player
        duration = player.duration
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = mediaPlayer?.currentPosition ?: 0
            kotlinx.coroutines.delay(200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFF00CEC9).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00CEC9),
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(
            onClick = {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF00CEC9))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00CEC9),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = formatTime(duration),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 1000) / 60
    return String.format("%02d:%02d", min, sec)
}
