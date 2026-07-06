package com.helpofai.videoplayer.feature.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.helpofai.videoplayer.core.model.Video

// ─── Section Title ────────────────────────────────────────────────────────────

/** Standard section header used between horizontal carousels on the Home tab. */
@Composable
fun LibrarySectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─── Collection Chip ─────────────────────────────────────────────────────────

/** Compact chip card used in the Smart Playlists horizontal row. */
@Composable
fun LibraryCollectionChip(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(64.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ─── Video Info Card (Horizontal carousel card with context menu) ─────────────

/**
 * Portrait-style card used in horizontal carousels (Recently Added, Large Files, etc.).
 * Shows thumbnail, title, size, favorite button, and a "more" context menu.
 */
@Composable
fun LibraryVideoInfoCard(
    video: Video,
    onClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMergeClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        VideoThumbnailCard(video = video)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = video.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onFavoriteClick) {
                val icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                Icon(
                    icon,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xCC1E293B))
                ) {
                    DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; onShareClick() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRenameClick() })
                    DropdownMenuItem(text = { Text("Merge with...") }, onClick = { showMenu = false; onMergeClick() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDeleteClick() })
                }
            }
        }
    }
}

// ─── Favorite / Short-Clip Square Card ───────────────────────────────────────

/**
 * Small square card used in the Favorites and Short Clips horizontal carousels.
 * Fixed 90×90dp thumbnail with title and favorite button below.
 */
@Composable
fun LibraryFavoriteVideoCard(
    video: Video,
    onClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier.size(90.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .crossfade(true)
                        .size(512)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                AdvancedVideoTags(
                    video = video,
                    modifier = Modifier.align(Alignment.TopStart).padding(2.dp).scale(0.8f)
                )
                if (video.lastPlayedPosition > 0 && video.duration > 0) {
                    val progress = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF001F3F).copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = video.formattedDuration, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(90.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                val icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                Icon(
                    icon,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Compact List Item (used in folder / resume / playlist detail views) ─────

/**
 * Horizontal list row with 80dp thumbnail on the left and metadata + actions on the right.
 * Used in the Folders tab, Resume Playback section, and playlist detail views.
 */
@Composable
fun LibraryCompactVideoListItem(
    video: Video,
    onClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMergeClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.width(80.dp).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .crossfade(true)
                        .size(256)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                AdvancedVideoTags(
                    video = video,
                    modifier = Modifier.align(Alignment.TopStart).padding(2.dp).scale(0.8f)
                )
                if (video.lastPlayedPosition > 0 && video.duration > 0) {
                    val progress = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${video.formattedDuration} • ${video.formattedSize}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onFavoriteClick) {
            val icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
            Icon(
                icon,
                contentDescription = "Favorite",
                tint = if (video.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xCC1E293B))
            ) {
                DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; onShareClick() })
                DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRenameClick() })
                DropdownMenuItem(text = { Text("Merge with...") }, onClick = { showMenu = false; onMergeClick() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDeleteClick() })
            }
        }
    }
}
