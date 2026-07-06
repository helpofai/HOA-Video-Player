package com.helpofai.videoplayer.feature.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.helpofai.videoplayer.core.model.Video

@Composable
fun VideoThumbnailCard(
    video: Video,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(101.dp), // 16:9 ratio
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Coil AsyncImage
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.uri)
                    .crossfade(true)
                    .size(512) // Cap resolution to save RAM
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Video Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Advanced Tags
            AdvancedVideoTags(
                video = video,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )

            // Duration Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF001F3F).copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            // View Memory Progress Bar
            if (video.lastPlayedPosition > 0 && video.duration > 0) {
                val progress = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
