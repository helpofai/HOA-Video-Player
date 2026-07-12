package com.helpofai.videoplayer.feature.workspace.transfers

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TransferQueueView(
    activeQueue: List<ActiveTransfer>,
    onPauseToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalEta = remember(activeQueue) {
        val activeCount = activeQueue.count { !it.isPaused }
        if (activeCount > 0) "${activeCount * 18}s" else "0s"
    }
    
    val currentSpeed = remember(activeQueue) {
        val activeCount = activeQueue.count { !it.isPaused }
        if (activeCount > 0) "${activeCount * 54.2} MB/s" else "0.0 MB/s (Paused)"
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Active Transfer Queue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        // Speed indicator card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Ultrafast Shared Session", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = currentSpeed,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (activeQueue.isEmpty()) {
            Text("No active transfers", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        } else {
            activeQueue.forEachIndexed { index, transfer ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E222B).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(transfer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                Text(
                                    text = if (transfer.isPaused) "Paused" else "ETA: ${transfer.eta}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            
                            IconButton(onClick = { onPauseToggle(index) }) {
                                Icon(
                                    imageVector = if (transfer.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = "Toggle",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Simulated progression
                            val progress = if (transfer.isPaused) 0.45f else 0.72f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Text(String.format("%.0f%%", progress * 100f), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}
