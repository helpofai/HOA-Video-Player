package com.helpofai.videoplayer.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AiColorProfile(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector
)

val aiColorProfiles = listOf(
    AiColorProfile("standard", "Standard", "Original colors without modification", Icons.Default.ColorLens),
    AiColorProfile("cinematic", "AI Cinematic", "Rich contrast and warm tones for movies", Icons.Default.Movie),
    AiColorProfile("hdr_boost", "AI HDR Boost", "Simulates High Dynamic Range by boosting shadows and highlights", Icons.Default.AutoFixHigh),
    AiColorProfile("vivid", "AI Anime/Vivid", "High saturation and sharp edges ideal for animation", Icons.Default.ColorLens),
    AiColorProfile("dark_detail", "AI Night Vision", "Lifts black levels to reveal details in extremely dark scenes", Icons.Default.Nightlight)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEnhancerSheet(
    videoTitle: String,
    currentProfileId: String,
    onProfileSelect: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Offline AI Logic to auto-detect best profile based on metadata/title
    val recommendedProfile = remember(videoTitle) {
        val lowerTitle = videoTitle.lowercase()
        when {
            lowerTitle.contains("anime") || lowerTitle.contains("animation") -> "vivid"
            lowerTitle.contains("dark") || lowerTitle.contains("horror") -> "dark_detail"
            lowerTitle.contains("hdr") || lowerTitle.contains("4k") -> "hdr_boost"
            lowerTitle.contains("movie") || lowerTitle.contains("film") || lowerTitle.contains("1080p") -> "cinematic"
            else -> "standard"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xCC000000), // Glassmorphism
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.7f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("AI Video Enhancer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Professional offline color profiling", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = "AI Recommendation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
                
                items(aiColorProfiles.size) { index ->
                    val profile = aiColorProfiles[index]
                    val isSelected = profile.id == currentProfileId
                    val isRecommended = profile.id == recommendedProfile
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                onProfileSelect(profile.id)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = profile.icon,
                            contentDescription = null,
                            tint = if (isSelected || isRecommended) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                )
                                if (isRecommended) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Recommended", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
