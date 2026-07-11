package com.helpofai.videoplayer.feature.watch_party.ui.client_dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession

/**
 * Client Dashboard
 * 
 * Shows joined room info, playback status, and lists/filters local files in the active folder.
 */
@Composable
fun WatchPartyClientDashboard(
    session: WatchPartySession,
    videos: List<com.helpofai.videoplayer.core.model.Video>,
    syncStatus: String,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    val activeVideoFolder = remember(session.video) {
        session.video?.let { java.io.File(it.path).parentFile?.name ?: "Internal Storage" }
    }
    
    // Filter videos by the folder of the currently playing host video
    val filteredVideos = remember(videos, activeVideoFolder) {
        if (activeVideoFolder != null) {
            videos.filter { (java.io.File(it.path).parentFile?.name ?: "Internal Storage") == activeVideoFolder }
        } else {
            videos
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = session.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White
                )
                Text(
                    text = syncStatus, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color(0xFF00CEC9)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Session Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111520)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Room Connection", color = Color(0xFF7C5CE7), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Room ID: ${session.id}", color = Color.White, fontSize = 12.sp)
                Text("Host Address: ${session.hostIp}", color = Color.LightGray, fontSize = 12.sp)
                if (activeVideoFolder != null) {
                    Text("Host Folder Filter: $activeVideoFolder", color = Color(0xFF00CEC9), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Video Header
        val sectionTitle = if (activeVideoFolder != null) "Folder: $activeVideoFolder" else "Available Videos"
        Text(
            text = sectionTitle, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Filtered Video List with actual thumbnails
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredVideos) { video ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF111520),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Video Thumbnail Card
                        Box(
                            modifier = Modifier
                                .size(80.dp, 50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E2535)),
                            contentAlignment = Alignment.Center
                        ) {
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
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Video Title and Size
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${video.formattedSize}  |  ${video.formattedDuration}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Disconnect button
        Button(
            onClick = onDisconnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
        ) {
            Text("Disconnect Watch Party", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
