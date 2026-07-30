package com.helpofai.videoplayer.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.theme.frostedGlass

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    val isPremium: Boolean = false,
    val isComingSoon: Boolean = false
)

val availableTools = listOf(
    ToolItem(
        title = "Private Vault",
        description = "Hide & encrypt sensitive videos with PIN/Biometric lock.",
        icon = Icons.Default.Security,
        tint = Color(0xFFF39C12) // Orange
    ),
    ToolItem(
        title = "Video to MP3",
        description = "Extract high-quality audio tracks directly from videos.",
        icon = Icons.Default.AudioFile,
        tint = Color(0xFF9B59B6) // Purple
    ),
    ToolItem(
        title = "Video Trimmer",
        description = "Cut, crop, and compress large video files seamlessly.",
        icon = Icons.Default.ContentCut,
        tint = Color(0xFFE74C3C) // Red
    ),
    ToolItem(
        title = "Change Resolution",
        description = "Scale down 4K/1080p videos to save storage.",
        icon = Icons.Default.AspectRatio,
        tint = Color(0xFF2ECC71) // Green
    ),
    ToolItem(
        title = "Make GIF",
        description = "Convert any video segment into a looping GIF.",
        icon = Icons.Default.Gif,
        tint = Color(0xFF3498DB) // Blue
    ),
    ToolItem(
        title = "Network Stream",
        description = "Cast media to Smart TVs via DLNA or Chromecast.",
        icon = Icons.Default.CastConnected,
        tint = Color(0xFF1ABC9C), // Teal
        isComingSoon = true
    )
)

@Composable
fun ToolsScreen(
    paddingValues: PaddingValues,
    onToolClick: (ToolItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Section
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2C3E50),
                            Color(0xFF3498DB).copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Icon(
                    Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Advanced Tools",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unlock powerful media utilities directly within your player.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }

            items(availableTools) { tool ->
                ToolCard(tool = tool, onClick = { onToolClick(tool) })
            }
            
            // Add spacer at bottom for floating nav bar
            item { Spacer(modifier = Modifier.height(80.dp)) }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !tool.isComingSoon, onClick = onClick)
            .frostedGlass(cornerRadius = 20.dp, surfaceAlpha = 0.25f, surfaceColor = Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tool.tint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = tool.tint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (tool.isComingSoon) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SOON",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (tool.isPremium) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = "Premium",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Title & Description
            Column {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tool.isComingSoon) Color.White.copy(alpha = 0.5f) else Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 3,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.1f
                )
            }
        }
    }
}
