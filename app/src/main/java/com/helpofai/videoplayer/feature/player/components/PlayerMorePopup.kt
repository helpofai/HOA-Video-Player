package com.helpofai.videoplayer.feature.player.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import com.helpofai.videoplayer.core.model.Video

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.basicMarquee

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.ReorderableItem

data class ToolItem(val icon: ImageVector, val name: String, val onClick: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerMorePopup(
    videos: List<Video>,
    currentVideoPath: String?,
    onVideoSelect: (String) -> Unit,
    onReorderPlaylist: (Int, Int) -> Unit,
    onBookmarksClick: () -> Unit,
    onQualityAnalyzerClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    // We'll manage states for toggles locally to make the UI look functional
    var isDoubleTapEnabled by remember { mutableStateOf(true) }
    var isSwipeGesturesEnabled by remember { mutableStateOf(true) }
    var isAutoPlayNext by remember { mutableStateOf(true) }
    var isBackgroundPlay by remember { mutableStateOf(false) }

    var showQueue by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) videos
        else videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        // Only allow reorder if not filtered, to prevent index mismatch issues
        if (searchQuery.isBlank()) {
            onReorderPlaylist(from.index, to.index)
        }
    })

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        LaunchedEffect(view) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (view.parent as? DialogWindowProvider)?.window?.setBackgroundBlurRadius(60)
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(Color(0x66000000)) // Transparent 40% dark background
                    .clickable(enabled = false) {} // block clicks
            ) {
                if (showQueue) {
                    // Playing Queue View
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showQueue = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = "Playing Queue",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("Filter videos...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        state = state.listState,
                        modifier = Modifier
                            .weight(1f)
                            .reorderable(state)
                            .detectReorderAfterLongPress(state)
                    ) {
                        itemsIndexed(filteredVideos, key = { _, video -> video.path }) { index, video ->
                            ReorderableItem(state, key = video.path) { isDragging ->
                                val isCurrent = video.path == currentVideoPath
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                            else Color.White.copy(alpha = 0.05f)
                                        )
                                        .clickable { onVideoSelect(video.path) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Drag Handle
                                    if (searchQuery.isBlank()) {
                                        Icon(
                                            Icons.Default.DragIndicator, 
                                            contentDescription = "Drag to reorder", 
                                            tint = Color.Gray,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    
                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp, 50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = video.uri,
                                            contentDescription = "Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Playing", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Video Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = video.formattedDuration,
                                                color = Color.LightGray,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                text = video.formattedSize,
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tools & Settings View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Tools",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                        )

                        val tools = listOf(
                            ToolItem(Icons.Default.QueueMusic, "Playing Queue") { showQueue = true },
                            ToolItem(Icons.Default.AspectRatio, "Aspect Ratio") {},
                            ToolItem(Icons.Default.DisplaySettings, "Display Settings") {},
                            ToolItem(Icons.Default.Bookmarks, "Bookmarks") { onBookmarksClick() },
                            ToolItem(Icons.Default.AutoAwesomeMotion, "Smart Scenes") { onBookmarksClick() },
                            ToolItem(Icons.Default.HighQuality, "Quality Analyzer") { onQualityAnalyzerClick() },
                            ToolItem(Icons.Default.Favorite, "Favourite") {},
                            ToolItem(Icons.Default.PlaylistAdd, "Add to Playlist") {},
                            ToolItem(Icons.Default.Share, "Share") {},
                            ToolItem(Icons.Default.Cast, "Network Stream") {}
                        )

                        // Tools Grid
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            tools.forEach { tool ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .clickable { tool.onClick() }
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(tool.icon, contentDescription = tool.name, tint = Color.White, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = tool.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 24.dp))

                        Text(
                            text = "Shortcuts & Features",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ShortcutToggle("Double tap to seek", isDoubleTapEnabled) { isDoubleTapEnabled = it }
                        ShortcutToggle("Swipe for volume/brightness", isSwipeGesturesEnabled) { isSwipeGesturesEnabled = it }
                        ShortcutToggle("Auto play next video", isAutoPlayNext) { isAutoPlayNext = it }
                        ShortcutToggle("Background playback", isBackgroundPlay) { isBackgroundPlay = it }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutToggle(title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
