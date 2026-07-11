package com.helpofai.videoplayer.feature.workspace.tools

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.feature.workspace.shared.TaskProgressItem
import com.helpofai.videoplayer.feature.workspace.shared.ToolButton

@Composable
fun MediaToolboxView(
    activeExtractions: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xF20F1216),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Engine Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolButton("Trim", Icons.Default.ContentCut, Modifier.weight(1f)) {
                        Toast.makeText(context, "Select a video to trim", Toast.LENGTH_SHORT).show()
                    }
                    ToolButton("Convert", Icons.Default.CompareArrows, Modifier.weight(1f)) {
                        Toast.makeText(context, "FFmpeg converting in background...", Toast.LENGTH_SHORT).show()
                    }
                    ToolButton("Extract Audio", Icons.Default.Audiotrack, Modifier.weight(1f)) {
                        Toast.makeText(context, "Extracting audio track...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xF20F1216),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Background Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                
                activeExtractions.forEach { (name, progress) ->
                    if (progress < 1.0f) {
                        TaskProgressItem("Extracting $name", progress, "${((1.0f - progress) * 10).toInt()}s")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                TaskProgressItem("Transcoding H.265 stream", 0.65f, "8s")
                Spacer(modifier = Modifier.height(8.dp))
                TaskProgressItem("Generating layout thumbnails", 0.42f, "18s")
            }
        }
    }
}
