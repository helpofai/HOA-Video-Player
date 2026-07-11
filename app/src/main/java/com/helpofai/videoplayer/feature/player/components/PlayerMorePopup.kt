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
package com.helpofai.videoplayer.feature.player.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice
import com.helpofai.videoplayer.feature.watch_party.qr.generator.WatchPartyQrGenerator
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.basicMarquee

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyItemScope

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
    onDiagnosticsClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val watchPartySessionManager = remember { WatchPartySessionManager.getInstance() }
    val activeSession by watchPartySessionManager.activeSession.collectAsState()
    
    // We'll manage states for toggles locally to make the UI look functional
    var isDoubleTapEnabled by remember { mutableStateOf(true) }
    var isSwipeGesturesEnabled by remember { mutableStateOf(true) }
    var isAutoPlayNext by remember { mutableStateOf(true) }
    var isBackgroundPlay by remember { mutableStateOf(false) }

    var showQueue by remember { mutableStateOf(false) }
    var showWatchPartyConfig by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) videos
        else videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    
    val state = rememberReorderableLazyListState(onMove = { from, to ->
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
                    .background(Color(0xCC0C0E12)) // Transparent 80% dark background
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
                            CustomReorderableItem(state, key = video.path) { isDragging ->
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
                                    if (searchQuery.isBlank()) {
                                        Icon(
                                            Icons.Default.DragIndicator, 
                                            contentDescription = "Drag to reorder", 
                                            tint = Color.Gray,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    
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
                } else if (showWatchPartyConfig) {
                    // Watch Party Config Sub-Page
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showWatchPartyConfig = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = "Watch Party",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (activeSession != null) {
                            val session = activeSession!!
                            Text("Room Status: Active", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Host Device IP: ${session.hostIp}", color = Color.White, fontSize = 11.sp)
                            Text("Video: ${session.video?.title ?: "Local Stream Source"}", color = Color.LightGray, fontSize = 11.sp)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Connected Guests (${session.devices.size - 1})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            
                            val guests = session.devices.filterNot { it.isHost }
                            if (guests.isEmpty()) {
                                Text("No guests connected yet.", color = Color.Gray, fontSize = 10.sp)
                            } else {
                                guests.forEach { guest ->
                                    Surface(
                                        color = Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(guest.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(guest.status, color = Color.Green, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                            
                            var showQrDialog by remember { mutableStateOf(false) }
                            if (showQrDialog) {
                                val videoParam = session.video?.let {
                                    "&videoTitle=${java.net.URLEncoder.encode(it.title, "UTF-8")}&videoDuration=${it.duration}&videoPath=${java.net.URLEncoder.encode(it.path, "UTF-8")}&videoSize=${it.size}"
                                } ?: ""
                                val joinLink = "vidplay://join?roomId=${session.id}&hostIp=${session.hostIp}&port=${session.port}&token=${session.securityToken}&roomName=${java.net.URLEncoder.encode(session.name, "UTF-8")}$videoParam"
                                val qrBitmap = remember(joinLink) { WatchPartyQrGenerator.generateQrBitmap(joinLink, 400) }
                                Dialog(onDismissRequest = { showQrDialog = false }) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF111520),
                                        border = BorderStroke(1.dp, Color(0xFF1E2535)),
                                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text("Scan to Join Room", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            if (qrBitmap != null) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "QR Code",
                                                    modifier = Modifier
                                                        .size(200.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color.White)
                                                        .padding(8.dp)
                                                )
                                            }
                                            Text("Room ID: ${session.id}", color = Color.LightGray, fontSize = 12.sp)
                                            Button(
                                                onClick = { showQrDialog = false },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CE7))
                                            ) {
                                                Text("Close", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { showQrDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CE7)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Show Room QR Code", fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    watchPartySessionManager.endSession()
                                    Toast.makeText(context, "Watch Party closed", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("End Watch Party Room", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                "No active watch party session.\nEnable \"Watch Party\" in Shortcuts below, then go to the Watch Party tab to create a room.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            // Show which video will be streamed
                            val streamingVideo by watchPartySessionManager.currentStreamingVideo.collectAsState()
                            if (streamingVideo != null) {
                                Surface(
                                    color = Color(0xFF0D2218),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF00B894).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayCircle, null, tint = Color(0xFF00B894), modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Streaming video set:", color = Color.Gray, fontSize = 9.sp)
                                            Text(streamingVideo!!.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "\u2705 Watch Party tick is ON. Go to Watch Party tab \u2192 Host Room to create the room.",
                                    color = Color(0xFF00B894),
                                    fontSize = 10.sp
                                )
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
                            ToolItem(Icons.Default.Group, "Watch Party") { showWatchPartyConfig = true },
                            ToolItem(Icons.Default.Info, "Diagnostics") { onDiagnosticsClick() },
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
                                Box(
                                    modifier = Modifier
                                        .size(96.dp, 80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable { tool.onClick() }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
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
                        
                        // Watch Party Tick — enables streaming of currently playing video
                        // When ticked ON: sets the current video as the streaming source (no picker dialog)
                        // When ticked OFF: clears the streaming source and ends any active session
                        val isSyncModeEnabled by watchPartySessionManager.isSyncModeEnabled.collectAsState()
                        ShortcutToggle("Watch Party Synchronized Mode", isSyncModeEnabled) { isChecked ->
                            watchPartySessionManager.setSyncMode(isChecked)
                            if (isChecked) {
                                val currentVideo = videos.firstOrNull { it.path == currentVideoPath } ?: run {
                                    currentVideoPath?.let { path ->
                                        com.helpofai.videoplayer.core.model.Video(
                                            id = path.hashCode().toLong(),
                                            uri = android.net.Uri.fromFile(java.io.File(path)),
                                            title = java.io.File(path).name,
                                            duration = 0L,
                                            size = 0L,
                                            dateAdded = 0L,
                                            path = path
                                        )
                                    }
                                }
                                if (currentVideo != null) {
                                    watchPartySessionManager.setStreamingVideo(currentVideo)
                                    Toast.makeText(context, "Watch Party Sync Mode: ON", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Watch Party Sync Mode: ON (No video loaded)", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                watchPartySessionManager.setStreamingVideo(null)
                                Toast.makeText(context, "Watch Party Sync Mode: OFF", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
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

@Composable
fun LazyItemScope.CustomReorderableItem(
    state: org.burnoutcrew.reorderable.ReorderableState<*>,
    key: Any?,
    modifier: Modifier = Modifier,
    content: @Composable LazyItemScope.(isDragging: Boolean) -> Unit
) {
    val isDragging = state.draggingItemKey == key
    val translation = if (isDragging) {
        try {
            val method = state::class.java.methods.firstOrNull { 
                it.name == "getDraggingItemOffset" || 
                it.name == "getDragOffset" || 
                it.name.contains("offset", ignoreCase = true) 
            }
            method?.let { 
                val value = it.invoke(state)
                if (value is Float) value else (value as? Number)?.toFloat() ?: 0f
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    } else {
        0f
    }
    
    val dragModifier = if (isDragging) {
        Modifier
            .graphicsLayer {
                translationY = translation
            }
            .zIndex(1f)
    } else {
        Modifier.animateItem()
    }
    
    Box(
        modifier = modifier.then(dragModifier)
    ) {
        content(isDragging)
    }
}
