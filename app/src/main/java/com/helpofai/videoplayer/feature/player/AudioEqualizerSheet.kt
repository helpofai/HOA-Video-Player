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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.os.Build
import com.helpofai.videoplayer.core.playback.AudioEffectManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEqualizerSheet(
    audioEffectManager: AudioEffectManager,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val view = LocalView.current
    LaunchedEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (view.parent as? DialogWindowProvider)?.window?.setBackgroundBlurRadius(60)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xF00D111A), // Frosted glass aesthetic
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = null
    ) {
        // We use derivedStateOf or just simple remember updated states.
        // Because AudioEffectManager doesn't expose StateFlow yet, we poll or use snapshot state.
        // For simplicity, we initialize state from the manager.
        
        var isEqEnabled by remember { mutableStateOf(audioEffectManager.isEqualizerEnabled) }
        var isBassEnabled by remember { mutableStateOf(audioEffectManager.isBassBoostEnabled) }
        var isVirtualizerEnabled by remember { mutableStateOf(audioEffectManager.isVirtualizerEnabled) }

        var bassStrength by remember { mutableFloatStateOf(0f) }
        var virtualizerStrength by remember { mutableFloatStateOf(0f) }

        val numBands = audioEffectManager.getNumberOfBands()
        val levelRange = audioEffectManager.getBandLevelRange()
        val minLevel = if (levelRange.isNotEmpty()) levelRange[0].toFloat() else 0f
        val maxLevel = if (levelRange.size > 1) levelRange[1].toFloat() else 100f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            Text(
                text = "Audio Equalizer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Equalizer Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable 10-Band Equalizer", modifier = Modifier.weight(1f))
                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = { 
                        isEqEnabled = it
                        audioEffectManager.isEqualizerEnabled = it 
                    }
                )
            }

            if (isEqEnabled && numBands > 0) {
                // Sliders for bands displayed horizontally
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until numBands) {
                        val band = i.toShort()
                        val freqRange = audioEffectManager.getBandFreqRange(band)
                        val freqHz = if (freqRange.isNotEmpty()) freqRange[0] / 1000 else 0
                        
                        var level by remember(isEqEnabled) { mutableFloatStateOf(audioEffectManager.getBandLevel(band).toFloat()) }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp)
                        ) {
                            Text("${freqHz}Hz", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Box(
                                modifier = Modifier
                                    .height(150.dp)
                                    .width(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = level,
                                    onValueChange = { 
                                        level = it
                                        audioEffectManager.setBandLevel(band, it.toInt().toShort()) 
                                    },
                                    valueRange = minLevel..maxLevel,
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(48.dp)
                                        .graphicsLayer {
                                            rotationZ = -90f
                                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                                        }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            val displayLevel = if (level > 0) "+${(level/100).toInt()}" else "${(level/100).toInt()}"
                            Text(displayLevel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Bass Boost
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Bass Boost", modifier = Modifier.weight(1f))
                Switch(
                    checked = isBassEnabled,
                    onCheckedChange = { 
                        isBassEnabled = it
                        audioEffectManager.isBassBoostEnabled = it 
                    }
                )
            }
            if (isBassEnabled) {
                Slider(
                    value = bassStrength,
                    onValueChange = {
                        bassStrength = it
                        audioEffectManager.setBassBoostStrength(it.toInt().toShort())
                    },
                    valueRange = 0f..1000f
                )
            }

            HorizontalDivider()

            // Virtualizer (Surround Sound)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Virtualizer (Surround)", modifier = Modifier.weight(1f))
                Switch(
                    checked = isVirtualizerEnabled,
                    onCheckedChange = { 
                        isVirtualizerEnabled = it
                        audioEffectManager.isVirtualizerEnabled = it 
                    }
                )
            }
            if (isVirtualizerEnabled) {
                Slider(
                    value = virtualizerStrength,
                    onValueChange = {
                        virtualizerStrength = it
                        audioEffectManager.setVirtualizerStrength(it.toInt().toShort())
                    },
                    valueRange = 0f..1000f
                )
            }
        }
    }
}
