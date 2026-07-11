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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.helpofai.videoplayer.core.playback.diagnostics.VideoEnhancementManager
import com.helpofai.videoplayer.core.playback.diagnostics.VideoEnhancementManager.VideoEnhancementConfig
import com.helpofai.videoplayer.core.playback.diagnostics.MediaAnalyzer.MediaCompatibilityReport

data class EnhancerPreset(
    val id: String,
    val name: String,
    val icon: ImageVector
)

val enhancerPresets = listOf(
    EnhancerPreset("auto", "Auto", Icons.Default.AutoAwesome),
    EnhancerPreset("original", "Original", Icons.Default.Block),
    EnhancerPreset("cinema", "Cinema", Icons.Default.Movie),
    EnhancerPreset("natural", "Natural", Icons.Default.FilterHdr),
    EnhancerPreset("vivid", "Vivid", Icons.Default.ColorLens),
    EnhancerPreset("amoled", "AMOLED", Icons.Default.BrightnessLow),
    EnhancerPreset("hdr", "HDR Boost", Icons.Default.AutoFixHigh),
    EnhancerPreset("anime", "Anime", Icons.Default.Brush),
    EnhancerPreset("sports", "Sports", Icons.Default.DirectionsRun),
    EnhancerPreset("low_light", "Low Light", Icons.Default.Nightlight),
    EnhancerPreset("custom", "Custom", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEnhancerSheet(
    enhancementManager: VideoEnhancementManager,
    report: MediaCompatibilityReport?,
    onDismissRequest: () -> Unit
) {
    val config by enhancementManager.config.collectAsState()
    val isOptimized by enhancementManager.isOptimizedForPerformance.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xF20F1216), // Sleek, modern premium dark background
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Video Enhancement Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Device-adaptive real-time clarity engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
                
                // Active status
                Surface(
                    color = if (config.preset != "original") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.DarkGray,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (config.preset != "original") "Active" else "Off",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (config.preset != "original") MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
            ) {
                // Adaptive Warning / Message
                if (isOptimized) {
                    Surface(
                        color = Color(0x33FF9800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, Color(0x66FF9800), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Playback Optimization: Enhancement has been reduced to maintain smooth playback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }

                // Auto Enhance Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto Enhance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Automatically configure stages based on media diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = config.autoEnhance,
                        onCheckedChange = { checked ->
                            if (checked) {
                                enhancementManager.applyPreset("auto", report)
                            } else {
                                enhancementManager.updateConfig(config.copy(autoEnhance = false))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enhancement Strength Slider (only if enabled)
                if (config.preset != "original") {
                    Text(
                        "Enhancement Strength",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Slider(
                            value = config.strength,
                            onValueChange = { newValue ->
                                enhancementManager.updateConfig(config.copy(strength = newValue))
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "${(config.strength * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Presets Title
                Text(
                    "Quick Presets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Presets Horizonal Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(enhancerPresets.size) { index ->
                        val preset = enhancerPresets[index]
                        val isSelected = config.preset == preset.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    enhancementManager.applyPreset(preset.id, report)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    preset.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom sliders block (Enabled only for CUSTOM, otherwise show readonly stats)
                val isCustom = config.preset == "custom"

                ExpandableSection(
                    title = "Image Adjustments",
                    isCustom = isCustom,
                    config = config
                ) {
                    SliderItem(
                        label = "Brightness",
                        value = config.brightness,
                        valueRange = -1f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(brightness = it)) }
                    )
                    SliderItem(
                        label = "Contrast",
                        value = config.contrast,
                        valueRange = -1f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(contrast = it)) }
                    )
                    SliderItem(
                        label = "Saturation",
                        value = config.saturation,
                        valueRange = -1f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(saturation = it)) }
                    )
                    SliderItem(
                        label = "Vibrance",
                        value = config.vibrance,
                        valueRange = -1f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(vibrance = it)) }
                    )
                    SliderItem(
                        label = "Gamma",
                        value = config.gamma,
                        valueRange = 0.5f..2.0f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(gamma = it)) }
                    )
                    SliderItem(
                        label = "Color Temperature",
                        value = config.colorTemperature,
                        valueRange = -1f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(colorTemperature = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExpandableSection(
                    title = "Detail Adjustments",
                    isCustom = isCustom,
                    config = config
                ) {
                    SliderItem(
                        label = "Sharpness",
                        value = config.sharpness,
                        valueRange = 0f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(sharpness = it)) }
                    )
                    SliderItem(
                        label = "Edge Enhancement",
                        value = config.edgeEnhancement,
                        valueRange = 0f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(edgeEnhancement = it)) }
                    )
                    SliderItem(
                        label = "Noise Reduction",
                        value = config.noiseReduction,
                        valueRange = 0f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(noiseReduction = it)) }
                    )
                    SliderItem(
                        label = "Texture Enhancement",
                        value = config.textureEnhancement,
                        valueRange = 0f..1f,
                        enabled = isCustom,
                        onValueChange = { enhancementManager.updateConfig(config.copy(textureEnhancement = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExpandableSection(
                    title = "Playback & Color Rendering",
                    isCustom = isCustom,
                    config = config
                ) {
                    ToggleItem(
                        label = "HDR Processing",
                        checked = config.hdrProcessing,
                        enabled = isCustom,
                        onCheckedChange = { enhancementManager.updateConfig(config.copy(hdrProcessing = it)) }
                    )
                    ToggleItem(
                        label = "Tone Mapping",
                        checked = config.toneMapping,
                        enabled = isCustom,
                        onCheckedChange = { enhancementManager.updateConfig(config.copy(toneMapping = it)) }
                    )
                    ToggleItem(
                        label = "Frame Optimization",
                        checked = config.frameOptimization,
                        enabled = isCustom,
                        onCheckedChange = { enhancementManager.updateConfig(config.copy(frameOptimization = it)) }
                    )
                    ToggleItem(
                        label = "Color Correction",
                        checked = config.colorCorrection,
                        enabled = isCustom,
                        onCheckedChange = { enhancementManager.updateConfig(config.copy(colorCorrection = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom controls (Save, Reset)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            enhancementManager.applyPreset("original", report)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Reset")
                    }

                    if (isCustom) {
                        Button(
                            onClick = {
                                enhancementManager.saveCustomPreset(config)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Custom Preset", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    isCustom: Boolean,
    config: VideoEnhancementConfig,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!isCustom) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(Locked to Preset)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(content = content)
            }
        }
    }
}

@Composable
fun SliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = if (enabled) Color.White else Color.Gray)
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
        Slider(
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) MaterialTheme.colorScheme.primary else Color.DarkGray,
                activeTrackColor = if (enabled) MaterialTheme.colorScheme.primary else Color.DarkGray,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun ToggleItem(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (enabled) Color.White else Color.Gray)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = Color.Black
            )
        )
    }
}
