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
package com.helpofai.videoplayer.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.theme.frostedGlass
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─── 1. TARGET ABSTRACTION ───────────────────────────────────────────────────

/**
 * Encapsulates the target item(s) being operated upon by the action menu.
 */
sealed interface MediaActionTarget {
    data class SingleVideo(val video: Video) : MediaActionTarget
    data class SingleFolder(val folderName: String, val videos: List<Video>) : MediaActionTarget
    data class MultipleVideos(val videos: List<Video>) : MediaActionTarget
}

// ─── 2. SELECTION STATE HOLDER ────────────────────────────────────────────────

/**
 * Enterprise-grade multi-selection state holder that can be hoisted anywhere
 * across tabs, screens, and dialogs.
 */
@Stable
class MediaSelectionState {
    var isSelectionMode by mutableStateOf(false)
    val selectedVideos = mutableStateListOf<Video>()
    val selectedFolderNames = mutableStateListOf<String>()

    val selectedCount: Int
        get() = selectedVideos.size + selectedFolderNames.size

    val isAnySelected: Boolean
        get() = selectedCount > 0

    fun isVideoSelected(video: Video): Boolean =
        selectedVideos.any { it.id == video.id }

    fun isFolderSelected(folderName: String): Boolean =
        selectedFolderNames.contains(folderName)

    fun toggleVideo(video: Video) {
        val index = selectedVideos.indexOfFirst { it.id == video.id }
        if (index >= 0) {
            selectedVideos.removeAt(index)
            if (selectedCount == 0) isSelectionMode = false
        } else {
            selectedVideos.add(video)
            isSelectionMode = true
        }
    }

    fun toggleFolder(folderName: String, folderVideos: List<Video> = emptyList()) {
        if (selectedFolderNames.contains(folderName)) {
            selectedFolderNames.remove(folderName)
            if (selectedCount == 0) isSelectionMode = false
        } else {
            selectedFolderNames.add(folderName)
            isSelectionMode = true
        }
    }

    fun selectAll(videos: List<Video>) {
        selectedVideos.clear()
        selectedVideos.addAll(videos)
        isSelectionMode = true
    }

    fun deselectAll() {
        selectedVideos.clear()
        selectedFolderNames.clear()
        isSelectionMode = false
    }

    fun clear() = deselectAll()
}

@Composable
fun rememberMediaSelectionState(): MediaSelectionState {
    return remember { MediaSelectionState() }
}

// ─── 3. MULTI-SELECT TOP BAR ──────────────────────────────────────────────────

/**
 * Reusable animated top action bar that slides down when multi-selection mode is active.
 */
@Composable
fun MultiSelectTopBar(
    selectionState: MediaSelectionState,
    allVideos: List<Video>,
    modifier: Modifier = Modifier,
    onShareSelected: (List<Video>) -> Unit = {},
    onVaultSelected: (List<Video>) -> Unit = {},
    onDeleteSelected: (List<Video>) -> Unit = {}
) {
    AnimatedVisibility(
        visible = selectionState.isSelectionMode,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectionState.clear() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close selection",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${selectionState.selectedCount} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                // Select All / Deselect All Toggle
                val isAllSelected = selectionState.selectedVideos.size >= allVideos.size && allVideos.isNotEmpty()
                IconButton(
                    onClick = {
                        if (isAllSelected) selectionState.deselectAll()
                        else selectionState.selectAll(allVideos)
                    }
                ) {
                    Icon(
                        imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                        tint = Color(0xFF38BDF8)
                    )
                }

                // Share Batch
                IconButton(
                    onClick = { onShareSelected(selectionState.selectedVideos.toList()) },
                    enabled = selectionState.selectedVideos.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Selected",
                        tint = Color.White
                    )
                }

                // Vault Batch
                IconButton(
                    onClick = { onVaultSelected(selectionState.selectedVideos.toList()) },
                    enabled = selectionState.selectedVideos.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Move Selected to Vault",
                        tint = Color(0xFFA855F7)
                    )
                }

                // Delete Batch
                IconButton(
                    onClick = { onDeleteSelected(selectionState.selectedVideos.toList()) },
                    enabled = selectionState.selectedVideos.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Selected",
                        tint = Color(0xFFF43F5E)
                    )
                }
            }
        }
    }
}

// ─── 4. REUSABLE LONG-PRESS & CONTEXT MENU BOTTOM SHEET ───────────────────────

/**
 * Universal bottom sheet popup for files, folders, and multi-selected media.
 * Provides rich actions including Play, Floating Mini Player, Favorite, Vault,
 * Share, Rename, Editor shortcuts, Properties inspection, and Delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaActionBottomSheet(
    target: MediaActionTarget?,
    onDismiss: () -> Unit,
    onPlay: (Video) -> Unit = {},
    onPlayMiniPlayer: (Video) -> Unit = {},
    onPlayFolderAll: (List<Video>) -> Unit = {},
    onFavoriteToggle: (Video) -> Unit = {},
    onMoveToVault: (List<Video>) -> Unit = {},
    onShare: (List<Video>) -> Unit = {},
    onRename: (Video) -> Unit = {},
    onDelete: (List<Video>) -> Unit = {},
    onEnterSelectionMode: () -> Unit = {},
    onOpenEditor: ((mode: String, video: Video) -> Unit)? = null
) {
    if (target == null) return

    var showPropertiesDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0F1D),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Target Header ────────────────────────────────────────────────
            when (target) {
                is MediaActionTarget.SingleVideo -> {
                    val video = target.video
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .size(width = 72.dp, height = 52.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
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
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${video.formattedDuration} • ${video.formattedSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = video.resolvedFolderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                is MediaActionTarget.SingleFolder -> {
                    val folderName = target.folderName
                    val videos = target.videos
                    val totalSize = videos.sumOf { it.size }
                    val kb = totalSize / 1024.0
                    val mb = kb / 1024.0
                    val gb = mb / 1024.0
                    val formattedSize = when {
                        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
                        mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
                        else -> String.format(Locale.getDefault(), "%.2f KB", kb)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${videos.size} videos • $formattedSize",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                is MediaActionTarget.MultipleVideos -> {
                    val videos = target.videos
                    val totalSize = videos.sumOf { it.size }
                    val kb = totalSize / 1024.0
                    val mb = kb / 1024.0
                    val gb = mb / 1024.0
                    val formattedSize = when {
                        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
                        mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
                        else -> String.format(Locale.getDefault(), "%.2f KB", kb)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${videos.size} items selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Total Size: $formattedSize",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // ── Primary Quick Actions Row ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (target) {
                    is MediaActionTarget.SingleVideo -> {
                        val video = target.video
                        QuickActionButton(
                            icon = Icons.Default.PlayArrow,
                            label = "Play",
                            color = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onPlay(video) }
                        )
                        QuickActionButton(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            label = "Mini Player",
                            color = Color(0xFF6366F1),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onPlayMiniPlayer(video) }
                        )
                        QuickActionButton(
                            icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (video.isFavorite) "Favorited" else "Favorite",
                            color = if (video.isFavorite) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onFavoriteToggle(video) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onShare(listOf(video)) }
                        )
                    }

                    is MediaActionTarget.SingleFolder -> {
                        val videos = target.videos
                        QuickActionButton(
                            icon = Icons.Default.PlayArrow,
                            label = "Play All",
                            color = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onPlayFolderAll(videos) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Lock,
                            label = "Vault All",
                            color = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onMoveToVault(videos) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Share,
                            label = "Share All",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onShare(videos) }
                        )
                    }

                    is MediaActionTarget.MultipleVideos -> {
                        val videos = target.videos
                        QuickActionButton(
                            icon = Icons.Default.Share,
                            label = "Share (${videos.size})",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onShare(videos) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Lock,
                            label = "Vault (${videos.size})",
                            color = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onMoveToVault(videos) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete (${videos.size})",
                            color = Color(0xFFF43F5E),
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onDelete(videos) }
                        )
                    }
                }
            }

            // ── Detailed Categorized Options List ────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF121B2F).copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            ) {
                when (target) {
                    is MediaActionTarget.SingleVideo -> {
                        val video = target.video
                        ActionRowItem(
                            icon = Icons.Default.Lock,
                            title = "Move to Private Vault",
                            subtitle = "AES-256 GCM encrypted storage",
                            tint = Color(0xFFA855F7),
                            onClick = { onDismiss(); onMoveToVault(listOf(video)) }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.Edit,
                            title = "Rename File",
                            subtitle = "Change display & file name",
                            onClick = { onDismiss(); onRename(video) }
                        )
                        ActionDivider()
                        if (onOpenEditor != null) {
                            ActionRowItem(
                                icon = Icons.Default.ContentCut,
                                title = "Video Trimmer",
                                subtitle = "Cut, trim, or crop video clip",
                                tint = Color(0xFF38BDF8),
                                onClick = { onDismiss(); onOpenEditor("Video Trimmer", video) }
                            )
                            ActionDivider()
                            ActionRowItem(
                                icon = Icons.Default.Audiotrack,
                                title = "Extract Audio (Video to MP3)",
                                subtitle = "Save audio track locally",
                                tint = Color(0xFFF59E0B),
                                onClick = { onDismiss(); onOpenEditor("Video to MP3", video) }
                            )
                            ActionDivider()
                        }
                        ActionRowItem(
                            icon = Icons.Default.Info,
                            title = "Properties & Metadata",
                            subtitle = "Codec, dimensions, size, path",
                            onClick = { showPropertiesDialog = true }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.SelectAll,
                            title = "Select Multiple",
                            subtitle = "Enter batch selection mode",
                            onClick = { onDismiss(); onEnterSelectionMode() }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.Delete,
                            title = "Delete Permanently",
                            subtitle = "Remove file from device",
                            tint = Color(0xFFF43F5E),
                            onClick = { onDismiss(); onDelete(listOf(video)) }
                        )
                    }

                    is MediaActionTarget.SingleFolder -> {
                        val videos = target.videos
                        ActionRowItem(
                            icon = Icons.Default.Lock,
                            title = "Move Folder to Private Vault",
                            subtitle = "Encrypt all ${videos.size} videos into vault",
                            tint = Color(0xFFA855F7),
                            onClick = { onDismiss(); onMoveToVault(videos) }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.Info,
                            title = "Folder Properties",
                            subtitle = "Total size, video count, storage path",
                            onClick = { showPropertiesDialog = true }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.SelectAll,
                            title = "Select Multiple",
                            subtitle = "Enter batch selection mode",
                            onClick = { onDismiss(); onEnterSelectionMode() }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.Delete,
                            title = "Delete All Videos in Folder",
                            subtitle = "Delete ${videos.size} videos from storage",
                            tint = Color(0xFFF43F5E),
                            onClick = { onDismiss(); onDelete(videos) }
                        )
                    }

                    is MediaActionTarget.MultipleVideos -> {
                        val videos = target.videos
                        ActionRowItem(
                            icon = Icons.Default.Lock,
                            title = "Move ${videos.size} Videos to Vault",
                            subtitle = "AES-256 bit encryption",
                            tint = Color(0xFFA855F7),
                            onClick = { onDismiss(); onMoveToVault(videos) }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.Share,
                            title = "Share ${videos.size} Videos",
                            subtitle = "Send via installed apps",
                            tint = Color(0xFF10B981),
                            onClick = { onDismiss(); onShare(videos) }
                        )
                        ActionDivider()
                        ActionRowItem(
                            icon = Icons.Default.Delete,
                            title = "Delete ${videos.size} Videos",
                            subtitle = "Permanently remove from device",
                            tint = Color(0xFFF43F5E),
                            onClick = { onDismiss(); onDelete(videos) }
                        )
                    }
                }
            }
        }
    }

    if (showPropertiesDialog) {
        MediaPropertiesDialog(
            target = target,
            onDismiss = { showPropertiesDialog = false }
        )
    }
}

// ─── 5. REUSABLE METADATA PROPERTIES DIALOG ───────────────────────────────────

@Composable
fun MediaPropertiesDialog(
    target: MediaActionTarget,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (target) {
                        is MediaActionTarget.SingleVideo -> "File Properties"
                        is MediaActionTarget.SingleFolder -> "Folder Properties"
                        is MediaActionTarget.MultipleVideos -> "Batch Properties"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (target) {
                    is MediaActionTarget.SingleVideo -> {
                        val video = target.video
                        PropertyItem(label = "Title", value = video.title)
                        PropertyItem(label = "Folder", value = video.resolvedFolderName)
                        PropertyItem(label = "Duration", value = video.formattedDuration)
                        PropertyItem(label = "File Size", value = "${video.formattedSize} (${video.size} bytes)")
                        if (video.width > 0 && video.height > 0) {
                            PropertyItem(label = "Resolution", value = "${video.width} × ${video.height}")
                        }
                        if (video.dateAdded > 0) {
                            val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                                .format(Date(video.dateAdded * 1000L))
                            PropertyItem(label = "Date Added", value = dateStr)
                        }
                        PropertyItem(
                            label = "Storage Path",
                            value = video.path,
                            isCopyable = true,
                            onCopy = {
                                clipboard?.setPrimaryClip(ClipData.newPlainText("File Path", video.path))
                                Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    is MediaActionTarget.SingleFolder -> {
                        val folderName = target.folderName
                        val videos = target.videos
                        val totalSize = videos.sumOf { it.size }
                        val gb = totalSize / (1024.0 * 1024.0 * 1024.0)
                        val formattedSize = if (gb >= 1.0) String.format(Locale.getDefault(), "%.2f GB", gb)
                        else String.format(Locale.getDefault(), "%.2f MB", totalSize / (1024.0 * 1024.0))

                        PropertyItem(label = "Folder Name", value = folderName)
                        PropertyItem(label = "Total Videos", value = "${videos.size} items")
                        PropertyItem(label = "Combined Size", value = "$formattedSize ($totalSize bytes)")
                        val samplePath = videos.firstOrNull()?.path?.let { File(it).parent ?: "" } ?: ""
                        if (samplePath.isNotBlank()) {
                            PropertyItem(
                                label = "Folder Directory",
                                value = samplePath,
                                isCopyable = true,
                                onCopy = {
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Folder Path", samplePath))
                                    Toast.makeText(context, "Folder path copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    is MediaActionTarget.MultipleVideos -> {
                        val videos = target.videos
                        val totalSize = videos.sumOf { it.size }
                        val gb = totalSize / (1024.0 * 1024.0 * 1024.0)
                        val formattedSize = if (gb >= 1.0) String.format(Locale.getDefault(), "%.2f GB", gb)
                        else String.format(Locale.getDefault(), "%.2f MB", totalSize / (1024.0 * 1024.0))

                        PropertyItem(label = "Total Selected", value = "${videos.size} files")
                        PropertyItem(label = "Total Size", value = "$formattedSize ($totalSize bytes)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ─── 6. HELPER SUB-COMPONENTS ────────────────────────────────────────────────

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = tint, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun ActionDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.05f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun PropertyItem(
    label: String,
    value: String,
    isCopyable: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (isCopyable && onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
