package com.helpofai.videoplayer.feature.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.ui.AspectRatioFrameLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoAdjustmentsSheet(
    currentResizeMode: Int,
    onResizeModeSelected: (Int) -> Unit,
    currentBrightness: Float,
    onBrightnessChanged: (Float) -> Unit,
    isMirrored: Boolean,
    onMirrorToggled: (Boolean) -> Unit,
    isFlipped: Boolean,
    onFlipToggled: (Boolean) -> Unit,
    rotationZ: Float,
    onRotationChanged: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Placeholder states for advanced color shaders
    var contrast by remember { mutableFloatStateOf(0.5f) }
    var saturation by remember { mutableFloatStateOf(0.5f) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E),
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "Video Adjustments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Brightness Slider
            item {
                Text("Brightness", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BrightnessLow, contentDescription = null, tint = Color.Gray)
                    Slider(
                        value = currentBrightness,
                        onValueChange = onBrightnessChanged,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFEB3B),
                            activeTrackColor = Color(0xFFFFEB3B)
                        )
                    )
                    Icon(Icons.Default.BrightnessHigh, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Contrast Slider (Placeholder)
            item {
                Text("Contrast", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Contrast, contentDescription = null, tint = Color.Gray)
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF9800),
                            activeTrackColor = Color(0xFFFF9800)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Saturation Slider (Placeholder)
            item {
                Text("Saturation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color.Gray)
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE91E63),
                            activeTrackColor = Color(0xFFE91E63)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Rotation, Mirror, Flip
            item {
                Text("Transformations", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mirror Video", color = Color.White)
                    Switch(checked = isMirrored, onCheckedChange = onMirrorToggled)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Flip Video (Vertical)", color = Color.White)
                    Switch(checked = isFlipped, onCheckedChange = onFlipToggled)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rotate 90°", color = Color.White)
                    IconButton(onClick = { onRotationChanged(rotationZ + 90f) }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Aspect Ratio Options
            item {
                Text("Aspect Ratio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                
                val options = listOf(
                    AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit Screen",
                    AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch",
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop (Fill)",
                    AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH to "Fixed Width",
                    AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT to "Fixed Height"
                )
                
                options.forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResizeModeSelected(mode) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            color = if (mode == currentResizeMode) MaterialTheme.colorScheme.primary else Color.White
                        )
                        if (mode == currentResizeMode) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
