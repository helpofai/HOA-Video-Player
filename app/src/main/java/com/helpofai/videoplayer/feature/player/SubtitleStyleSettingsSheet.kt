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
package com.helpofai.videoplayer.feature.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.core.media.SubtitleStyleManager
import kotlinx.coroutines.launch

/**
 * A comprehensive bottom sheet for customizing subtitle appearance and timing.
 *
 * Covers: font size, font color, background color, edge type, edge color,
 * vertical position, delay offset, and text encoding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleStyleSettingsSheet(
    styleManager: SubtitleStyleManager,
    onDismissRequest: () -> Unit
) {
    val config by styleManager.config.collectAsState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = ComposeColor.Transparent,
        contentColor = ComposeColor.White,
        scrimColor = ComposeColor.Black.copy(alpha = 0.4f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = ComposeColor.White.copy(alpha = 0.7f)) },
        // Real backdrop blur on API 31+ (Android 12). On API 30 blur is a no-op,
        // so we compensate with a translucent fallback scrim on the content column below.
        modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(5.dp) else Modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                // Fallback translucent backdrop for API 30 (where blur is unavailable),
                // and a subtle darkening for 31+ so white text keeps contrast over the video.
                .background(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ComposeColor.Black.copy(alpha = 0.25f)
                    else ComposeColor(0xCC000000)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Subtitle Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White
                )
            }

            HorizontalDivider(color = ComposeColor.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // ==================== FONT SIZE ====================
            SectionLabel("Font Size")
            Spacer(modifier = Modifier.height(8.dp))
            FontSizeSelector(
                currentSize = config.fontSizeKey,
                onSelect = { key -> scope.launch { styleManager.setFontSize(key) } }
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== FONT COLOR ====================
            SectionLabel("Font Color")
            Spacer(modifier = Modifier.height(8.dp))
            ColorSwatchRow(
                currentColor = config.fontColor,
                onColorSelected = { color -> scope.launch { styleManager.setFontColor(color) } },
                colors = listOf(
                    ComposeColor.White to "White",
                    ComposeColor.Yellow to "Yellow",
                    ComposeColor.Cyan to "Cyan",
                    ComposeColor.Green to "Green",
                    ComposeColor(0xFFFF6B6B) to "Red",
                    ComposeColor(0xFFFF69B4) to "Pink",
                    ComposeColor(0xFFFFA500) to "Orange",
                    ComposeColor(0xFF87CEEB) to "Sky",
                    ComposeColor(0xFF98FB98) to "Mint",
                    ComposeColor(0xFFDDA0DD) to "Plum"
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== BACKGROUND COLOR ====================
            SectionLabel("Background Color")
            Spacer(modifier = Modifier.height(8.dp))
            ColorSwatchRow(
                currentColor = config.bgColor,
                onColorSelected = { color -> scope.launch { styleManager.setBgColor(color) } },
                colors = listOf(
                    ComposeColor.Transparent to "None",
                    ComposeColor(0xFF333333) to "Semi Black",
                    ComposeColor(0xFF000000) to "Dark",
                    ComposeColor(0x99000000) to "Trans",
                    ComposeColor(0xFF1A1A2E) to "Navy",
                    ComposeColor(0xFF0D3B66) to "Blue",
                    ComposeColor(0xFF2D1B69) to "Purple",
                    ComposeColor(0xFF1B4332) to "Green",
                    ComposeColor(0xFF5C1606) to "Maroon",
                    ComposeColor(0xFF333333) to "Gray"
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== EDGE TYPE ====================
            SectionLabel("Text Edge / Outline")
            Spacer(modifier = Modifier.height(8.dp))
            EdgeTypeSelector(
                currentEdgeType = config.edgeType,
                onSelect = { edgeType -> scope.launch { styleManager.setEdgeType(edgeType) } }
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== EDGE COLOR ====================
            SectionLabel("Edge Color")
            Spacer(modifier = Modifier.height(8.dp))
            ColorSwatchRow(
                currentColor = config.edgeColor,
                onColorSelected = { color -> scope.launch { styleManager.setEdgeColor(color) } },
                colors = listOf(
                    ComposeColor.Black to "Black",
                    ComposeColor.White to "White",
                    ComposeColor(0xFF333333) to "DkGray",
                    ComposeColor(0xFF666666) to "Gray",
                    ComposeColor(0xFF0066CC) to "Blue",
                    ComposeColor(0xFFCC0000) to "Red",
                    ComposeColor(0xFF006600) to "Green",
                    ComposeColor(0xFFCC6600) to "Orange",
                    ComposeColor(0xFF660066) to "Purple",
                    ComposeColor(0xFF006666) to "Teal"
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== VERTICAL POSITION ====================
            SectionLabel("Vertical Position")
            Spacer(modifier = Modifier.height(8.dp))
            PositionSlider(
                position = config.bottomPaddingFraction,
                onPositionChange = { pos -> scope.launch { styleManager.setPosition(pos) } }
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== DELAY ====================
            SectionLabel("Subtitle Delay")
            Spacer(modifier = Modifier.height(8.dp))
            DelaySlider(
                delayMs = config.delayMs,
                onDelayChange = { delay -> scope.launch { styleManager.setDelayMs(delay) } }
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== ENCODING ====================
            SectionLabel("Text Encoding")
            Spacer(modifier = Modifier.height(8.dp))
            EncodingSelector(
                currentEncoding = config.encoding,
                onSelect = { encoding -> scope.launch { styleManager.setEncoding(encoding) } }
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ==================== SECTION LABEL ====================
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

// ==================== FONT SIZE SELECTOR ====================
@Composable
private fun FontSizeSelector(
    currentSize: String,
    onSelect: (String) -> Unit
) {
    val sizes = listOf(
        "small" to "Small",
        "medium" to "Medium",
        "large" to "Large",
        "xlarge" to "X-Large"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sizes.forEach { (key, label) ->
            val isSelected = currentSize == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else ComposeColor.White.copy(alpha = 0.08f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==================== COLOR SWATCH ROW ====================
@Composable
private fun ColorSwatchRow(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    colors: List<Pair<ComposeColor, String>>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { (color, label) ->
            val isSelected = currentColor == color.toArgb()
            val displayColor = if (color == ComposeColor.Transparent) {
                ComposeColor(0xFF333333)
            } else color

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onColorSelected(color.toArgb()) }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(displayColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else ComposeColor.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.Gray
                )
            }
        }
    }
}

// ==================== EDGE TYPE SELECTOR ====================
@Composable
private fun EdgeTypeSelector(
    currentEdgeType: String,
    onSelect: (String) -> Unit
) {
    val edgeTypes = listOf(
        "none" to "None",
        "drop_shadow" to "Shadow",
        "outline" to "Outline",
        "raised" to "Raised",
        "depressed" to "Depressed"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        edgeTypes.forEach { (key, label) ->
            val isSelected = currentEdgeType == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else ComposeColor.White.copy(alpha = 0.08f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==================== POSITION SLIDER ====================
@Composable
private fun PositionSlider(
    position: Float,
    onPositionChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Top", style = MaterialTheme.typography.labelSmall, color = ComposeColor.Gray)
            Text(
                text = "${(position * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("Bottom", style = MaterialTheme.typography.labelSmall, color = ComposeColor.Gray)
        }
        Slider(
            value = position,
            onValueChange = onPositionChange,
            valueRange = 0.1f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ==================== DELAY SLIDER ====================
@Composable
private fun DelaySlider(
    delayMs: Int,
    onDelayChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Earlier", style = MaterialTheme.typography.labelSmall, color = ComposeColor.Gray)
            Text(
                text = if (delayMs == 0) "Synced" else "${if (delayMs > 0) "+" else ""}${delayMs}ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("Later", style = MaterialTheme.typography.labelSmall, color = ComposeColor.Gray)
        }
        Slider(
            value = delayMs.toFloat(),
            onValueChange = { onDelayChange(it.toInt()) },
            valueRange = -5000f..5000f,
            steps = 19,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        // Quick preset buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(-1000, -500, -100, 0, 100, 500, 1000).forEach { ms ->
                val isSelected = delayMs == ms
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else ComposeColor.White.copy(alpha = 0.06f)
                        )
                        .clickable { onDelayChange(ms) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (ms == 0) "0" else "${if (ms > 0) "+" else ""}${ms / 1000.0}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ==================== ENCODING SELECTOR ====================
@Composable
private fun EncodingSelector(
    currentEncoding: String,
    onSelect: (String) -> Unit
) {
    val encodings = listOf(
        "auto" to "Auto Detect",
        "UTF-8" to "UTF-8",
        "ISO-8859-1" to "ISO-8859-1",
        "windows-1252" to "Windows-1252",
        "Shift_JIS" to "Shift JIS",
        "EUC-KR" to "EUC-KR",
        "GB2312" to "GB2312",
        "BIG5" to "Big5"
    )

    Column {
        encodings.forEach { (key, label) ->
            val isSelected = currentEncoding == key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else ComposeColor.Transparent
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
