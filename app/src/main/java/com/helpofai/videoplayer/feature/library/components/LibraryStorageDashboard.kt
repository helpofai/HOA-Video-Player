package com.helpofai.videoplayer.feature.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.scanner.ScannerStorageAnalyzer

/**
 * Full-screen (90% height) bottom sheet showing a storage health report.
 * Lists exact duplicates (with smart-clean action), suspected corrupted files,
 * and unused large videos — each with an individual delete button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryStorageDashboard(
    report: ScannerStorageAnalyzer.StorageReport,
    onDismissRequest: () -> Unit,
    onDeleteClick: (Video) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Storage Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val totalGb = report.totalSize / (1024.0 * 1024.0 * 1024.0)
            Text(
                "${report.totalVideos} videos • ${String.format("%.2f GB", totalGb)} total",
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── Exact Duplicates ──────────────────────────────────────────────
            if (report.exactDuplicates.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exact Duplicates (${report.exactDuplicates.size} groups)", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                report.exactDuplicates.forEach { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Exact Duplicates", fontWeight = FontWeight.Bold)
                                if (group.size > 1) {
                                    TextButton(
                                        onClick = { group.drop(1).forEach { onDeleteClick(it) } },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Smart Clean (Keep 1)") }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            group.forEach { video ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(video.uri)
                                                .crossfade(true)
                                                .size(256)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.title, maxLines = 1, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
                                        Text("${video.formattedSize} • ${video.formattedDuration}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { onDeleteClick(video) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Suspected Corrupted ───────────────────────────────────────────
            if (report.suspectedCorrupted.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suspected Corrupted (${report.suspectedCorrupted.size})", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                report.suspectedCorrupted.forEach { video ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Size: ${video.formattedSize}, Duration: 0s",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = { onDeleteClick(video) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Unused Large Videos ───────────────────────────────────────────
            if (report.unusedLargeVideos.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unused Large Videos", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                report.unusedLargeVideos.forEach { video ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Never played • ${video.formattedSize}", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { onDeleteClick(video) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ── All-clear message ─────────────────────────────────────────────
            if (report.exactDuplicates.isEmpty() && report.suspectedCorrupted.isEmpty() && report.unusedLargeVideos.isEmpty()) {
                Text(
                    "Your storage looks great! No duplicates or corrupted files found.",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
