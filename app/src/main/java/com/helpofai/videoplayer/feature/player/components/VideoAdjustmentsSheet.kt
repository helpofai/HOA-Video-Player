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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import com.helpofai.videoplayer.core.theme.ToolIconPalette

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
    // Adjustments signature color — the page carries one per-tool identity
    val accent = ToolIconPalette.Adjustments

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xF00D111A), // Frosted glass aesthetic
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = null
    ) {
        val view = LocalView.current
        LaunchedEffect(view) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (view.parent as? DialogWindowProvider)?.window?.setBackgroundBlurRadius(60)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            item {
                // Compact Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent)
                                    )
                                )
                        )
                        Icon(Icons.Default.Tune, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Video Adjustments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                            thumbColor = accent,
                            activeTrackColor = accent
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
                            thumbColor = accent,
                            activeTrackColor = accent
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
                            thumbColor = accent,
                            activeTrackColor = accent
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
                            color = if (mode == currentResizeMode) accent else Color.White
                        )
                        if (mode == currentResizeMode) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = accent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
