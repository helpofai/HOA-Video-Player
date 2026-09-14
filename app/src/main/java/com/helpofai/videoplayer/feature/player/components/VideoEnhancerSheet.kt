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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.playback.diagnostics.MediaAnalyzer.MediaCompatibilityReport
import com.helpofai.videoplayer.core.playback.diagnostics.VideoEnhancementManager
import com.helpofai.videoplayer.core.playback.diagnostics.VideoEnhancementManager.VideoEnhancementConfig
import com.helpofai.videoplayer.core.theme.ToolIconPalette
import com.helpofai.videoplayer.feature.player.PlayerViewModel
import java.util.Locale

data class EnhancerPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

val enhancerPresets = listOf(
    EnhancerPreset("auto", "Auto AI", "Diagnostics Tuned", Icons.Default.AutoAwesome, Color(0xFF00E5FF)),
    EnhancerPreset("hq", "HD Mode", "Ultra Clarity 85%", Icons.Default.HighQuality, ToolIconPalette.HQ),
    EnhancerPreset("original", "Original", "Untouched Stream", Icons.Default.Block, Color(0xFF9E9E9E)),
    EnhancerPreset("cinema", "Cinema", "24p Warm Film", Icons.Default.Movie, Color(0xFFFFB300)),
    EnhancerPreset("natural", "Natural", "True-to-Life Tone", Icons.Default.FilterHdr, Color(0xFF4CAF50)),
    EnhancerPreset("vivid", "Vivid", "Saturated Pop", Icons.Default.ColorLens, Color(0xFFE91E63)),
    EnhancerPreset("amoled", "AMOLED", "Deep Contrast", Icons.Default.BrightnessLow, Color(0xFF9C27B0)),
    EnhancerPreset("hdr", "HDR Boost", "Expanded Range", Icons.Default.AutoFixHigh, Color(0xFFFF5722)),
    EnhancerPreset("anime", "Anime", "Crisp Edge Lines", Icons.Default.Brush, Color(0xFF00BCD4)),
    EnhancerPreset("sports", "Sports", "High Frame Clarity", Icons.Default.Speed, Color(0xFF8BC34A)),
    EnhancerPreset("low_light", "Low Light", "Night Shadow Lift", Icons.Default.Nightlight, Color(0xFF3F51B5)),
    EnhancerPreset("custom", "Custom", "Studio Tuned", Icons.Default.Settings, Color(0xFFAB47BC))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEnhancerSheet(
    enhancementManager: VideoEnhancementManager,
    report: MediaCompatibilityReport?,
    autoAIState: PlayerViewModel.AutoAIState? = null,
    onTriggerAutoAIScan: (() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val config by enhancementManager.config.collectAsState()
    val isOptimized by enhancementManager.isOptimizedForPerformance.collectAsState()

    val isActive = config.preset != "original" && config.strength > 0f
    val currentPreset = enhancerPresets.find { it.id == config.preset } ?: enhancerPresets.first()

    // Temporary Before/After compare state
    var isComparingOriginal by remember { mutableStateOf(false) }
    var savedPreCompareConfig by remember { mutableStateOf<VideoEnhancementConfig?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setBackgroundBlurRadius(60)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xF00D111A), // Sleek OLED dark glass
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Compact Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }

            // ================= HEADER =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ToolIconPalette.VideoEnhancer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = ToolIconPalette.VideoEnhancer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Video Enhancement Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Real-time Media3 OpenGL Clarity Pipeline",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isActive) ToolIconPalette.VideoEnhancer.copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (isActive) ToolIconPalette.VideoEnhancer.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isActive) currentPreset.name else "OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) ToolIconPalette.VideoEnhancer else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    // Close (X) button
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(top = 4.dp)
            )

            // ================= SCROLLABLE CONTENT =================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Adaptive Battery / Thermal Warning
                if (isOptimized) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x25FF9800)),
                        border = BorderStroke(1.dp, Color(0x66FF9800)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Thermal / Battery Saver Active: Shaders scaled down to maintain 60 FPS playback.",
                                fontSize = 12.sp,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }

                // ================= AUTO AI SMART BANNER =================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ToolIconPalette.AutoAI.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, ToolIconPalette.AutoAI.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ToolIconPalette.AutoAI,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Auto AI Scene Neural Engine",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val aiContent = autoAIState?.result?.contentType
                                    Text(
                                        text = if (aiContent != null) "Detected: ${aiContent.uppercase()}" else "Samples 12 video frames for content profile",
                                        fontSize = 11.sp,
                                        color = ToolIconPalette.AutoAI
                                    )
                                }
                            }

                            if (onTriggerAutoAIScan != null) {
                                Button(
                                    onClick = onTriggerAutoAIScan,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ToolIconPalette.AutoAI
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run AI Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        // Stream Specs Chips (from MediaCompatibilityReport)
                        if (report != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val resLabel = if (report.width > 0 && report.height > 0) "${report.width}x${report.height}" else "Auto"
                                val hdrLabel = if (report.isHdr) "HDR10" else "SDR"
                                val codecLabel = report.videoCodec?.substringAfter("/")?.uppercase() ?: "AVC"

                                listOf(resLabel, hdrLabel, codecLabel).forEach { chip ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(chip, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= PRESETS CAROUSEL =================
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Clarity & Color Presets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${enhancerPresets.size} Profiles",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(enhancerPresets) { preset ->
                            val isSelected = config.preset == preset.id

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) preset.accentColor.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) preset.accentColor.copy(alpha = 0.8f)
                                    else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .width(128.dp)
                                    .clickable {
                                        enhancementManager.applyPreset(preset.id, report)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) preset.accentColor.copy(alpha = 0.35f)
                                                    else Color.White.copy(alpha = 0.08f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = preset.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) preset.accentColor else Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(preset.accentColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            fontSize = 10.sp,
                                            color = if (isSelected) preset.accentColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.45f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= MASTER STRENGTH & LIVE COMPARE =================
                if (config.preset != "original") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "Enhancement Intensity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Global shader convolution strength",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ToolIconPalette.VideoEnhancer.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${(config.strength * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ToolIconPalette.VideoEnhancer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Slider(
                                value = config.strength,
                                onValueChange = { newValue ->
                                    enhancementManager.updateConfig(config.copy(strength = newValue))
                                },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ToolIconPalette.VideoEnhancer,
                                    activeTrackColor = ToolIconPalette.VideoEnhancer,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                                )
                            )

                            // Preset strength chips & Compare Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(0.3f to "30%", 0.5f to "50%", 0.75f to "75%", 1.0f to "100%").forEach { (valF, label) ->
                                        val isCurrent = (config.strength * 100).toInt() == (valF * 100).toInt()
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isCurrent) ToolIconPalette.VideoEnhancer.copy(alpha = 0.25f)
                                                    else Color.White.copy(alpha = 0.06f)
                                                )
                                                .clickable {
                                                    enhancementManager.updateConfig(config.copy(strength = valF))
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) ToolIconPalette.VideoEnhancer else Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }

                                // Interactive Before/After Compare Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isComparingOriginal) Color(0xFFFF9800).copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .clickable {
                                            if (!isComparingOriginal) {
                                                savedPreCompareConfig = config
                                                enhancementManager.updateConfig(
                                                    VideoEnhancementConfig(preset = "original", strength = 0f)
                                                )
                                                isComparingOriginal = true
                                            } else {
                                                savedPreCompareConfig?.let { enhancementManager.updateConfig(it) }
                                                savedPreCompareConfig = null
                                                isComparingOriginal = false
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isComparingOriginal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (isComparingOriginal) Color(0xFFFF9800) else Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isComparingOriginal) "Viewing Original" else "Compare Original",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isComparingOriginal) Color(0xFFFF9800) else Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= EXPANDABLE FINE TUNING SECTIONS =================
                // Section 1: Image & Color Tone
                ModernExpandableSection(
                    title = "Color & Tone Curve",
                    subtitle = "Brightness, contrast, vibrance, gamma & color temperature",
                    icon = Icons.Default.ColorLens,
                    accentColor = Color(0xFF00E5FF)
                ) {
                    ModernSliderItem(
                        label = "Brightness",
                        value = config.brightness,
                        valueRange = -1f..1f,
                        unit = if (config.brightness >= 0) "+%.2f" else "%.2f",
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = {
                            val updated = config.copy(preset = "custom", brightness = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Contrast",
                        value = config.contrast,
                        valueRange = -1f..1f,
                        unit = if (config.contrast >= 0) "+%.2f" else "%.2f",
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = {
                            val updated = config.copy(preset = "custom", contrast = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Saturation",
                        value = config.saturation,
                        valueRange = -1f..1f,
                        unit = if (config.saturation >= 0) "+%.2f" else "%.2f",
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = {
                            val updated = config.copy(preset = "custom", saturation = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Vibrance (Smart Muted Lift)",
                        value = config.vibrance,
                        valueRange = -1f..1f,
                        unit = if (config.vibrance >= 0) "+%.2f" else "%.2f",
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = {
                            val updated = config.copy(preset = "custom", vibrance = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Gamma Linear Scale",
                        value = config.gamma,
                        valueRange = 0.5f..2.0f,
                        unit = "%.2fx",
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = {
                            val updated = config.copy(preset = "custom", gamma = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Color Temperature (Cool <-> Warm)",
                        value = config.colorTemperature,
                        valueRange = -1f..1f,
                        unit = if (config.colorTemperature >= 0) "+%.2f K" else "%.2f K",
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = {
                            val updated = config.copy(preset = "custom", colorTemperature = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                }

                // Section 2: Detail, Sharpness & Denoise
                ModernExpandableSection(
                    title = "Detail & Unsharp Mask Engine",
                    subtitle = "1D separable convolution sharpen, edge filter & Gaussian denoise",
                    icon = Icons.Default.AutoFixHigh,
                    accentColor = ToolIconPalette.VideoEnhancer
                ) {
                    ModernSliderItem(
                        label = "Sharpness (Unsharp Mask)",
                        value = config.sharpness,
                        valueRange = 0f..1f,
                        unit = "%.2f",
                        accentColor = ToolIconPalette.VideoEnhancer,
                        onValueChange = {
                            val updated = config.copy(preset = "custom", sharpness = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Edge Definition",
                        value = config.edgeEnhancement,
                        valueRange = 0f..1f,
                        unit = "%.2f",
                        accentColor = ToolIconPalette.VideoEnhancer,
                        onValueChange = {
                            val updated = config.copy(preset = "custom", edgeEnhancement = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Noise Reduction (Gaussian Denoise)",
                        value = config.noiseReduction,
                        valueRange = 0f..1f,
                        unit = "%.2f",
                        accentColor = ToolIconPalette.VideoEnhancer,
                        onValueChange = {
                            val updated = config.copy(preset = "custom", noiseReduction = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSliderItem(
                        label = "Texture Pop",
                        value = config.textureEnhancement,
                        valueRange = 0f..1f,
                        unit = "%.2f",
                        accentColor = ToolIconPalette.VideoEnhancer,
                        onValueChange = {
                            val updated = config.copy(preset = "custom", textureEnhancement = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                }

                // Section 3: Optics & Dynamic Rendering
                ModernExpandableSection(
                    title = "Optics, Tone Mapping & HDR",
                    subtitle = "Wide color gamuts, dynamic range compression & color correction",
                    icon = Icons.Default.FilterHdr,
                    accentColor = Color(0xFFFFB300)
                ) {
                    ModernSwitchItem(
                        label = "HDR Processing",
                        description = "Hardware HDR10 wide gamut processing",
                        checked = config.hdrProcessing,
                        accentColor = Color(0xFFFFB300),
                        onCheckedChange = {
                            val updated = config.copy(preset = "custom", hdrProcessing = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSwitchItem(
                        label = "Dynamic Tone Mapping",
                        description = "Compresses highlights and shadows naturally",
                        checked = config.toneMapping,
                        accentColor = Color(0xFFFFB300),
                        onCheckedChange = {
                            val updated = config.copy(preset = "custom", toneMapping = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSwitchItem(
                        label = "Frame Optimization",
                        description = "Smoothes jitter in sports and action scenes",
                        checked = config.frameOptimization,
                        accentColor = Color(0xFFFFB300),
                        onCheckedChange = {
                            val updated = config.copy(preset = "custom", frameOptimization = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                    ModernSwitchItem(
                        label = "Studio Color Correction",
                        description = "Rec.709 skin tone accuracy correction",
                        checked = config.colorCorrection,
                        accentColor = Color(0xFFFFB300),
                        onCheckedChange = {
                            val updated = config.copy(preset = "custom", colorCorrection = it)
                            enhancementManager.updateConfig(updated)
                            enhancementManager.saveCustomPreset(updated)
                        }
                    )
                }

                // ================= RESET & SAVE CONTROLS =================
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
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset to Flat")
                    }

                    Button(
                        onClick = {
                            enhancementManager.saveCustomPreset(config)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ToolIconPalette.VideoEnhancer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Preset", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ModernExpandableSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rot")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(
            1.dp,
            if (expanded) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun ModernSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            Text(
                text = String.format(Locale.US, unit, value),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Slider(
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.10f)
            )
        )
    }
}

@Composable
fun ModernSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}
