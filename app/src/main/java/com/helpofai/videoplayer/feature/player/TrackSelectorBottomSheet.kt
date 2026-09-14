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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import android.content.Context
import android.media.AudioManager
import androidx.compose.ui.platform.LocalContext
import com.helpofai.videoplayer.core.playback.AudioEffectManager
import com.helpofai.videoplayer.core.playback.diagnostics.AudioQualityAnalyzer
import com.helpofai.videoplayer.core.theme.ToolIconPalette
import java.util.Locale
import kotlin.math.roundToInt

fun getLanguageName(code: String): String {
    if (code.equals("und", ignoreCase = true) || code.isBlank()) return "Unknown Language"
    val iso3To2 = mapOf(
        "eng" to "en", "hin" to "hi", "jpn" to "ja", "kor" to "ko", 
        "fra" to "fr", "spa" to "es", "deu" to "de", "rus" to "ru", 
        "zho" to "zh", "por" to "pt", "ita" to "it", "ara" to "ar",
        "ben" to "bn", "tam" to "ta", "tel" to "te", "mal" to "ml",
        "kan" to "kn", "mar" to "mr", "guj" to "gu", "pun" to "pa"
    )
    val twoLetter = iso3To2[code.lowercase()] ?: code
    return try {
        val locale = Locale.forLanguageTag(twoLetter)
        val name = locale.displayLanguage
        if (name.lowercase() == twoLetter.lowercase()) code.uppercase() else name
    } catch (e: Exception) {
        code.uppercase()
    }
}

fun formatAudioCodec(mimeType: String?): String {
    if (mimeType == null) return "AUDIO"
    val upper = mimeType.uppercase()
    return when {
        upper.contains("EAC3-JOC") || upper.contains("E-AC3-JOC") -> "E-AC3 Atmos"
        upper.contains("EAC3") || upper.contains("E-AC3") -> "Dolby Digital Plus"
        upper.contains("AC3") || upper.contains("AC-3") -> "Dolby Digital"
        upper.contains("TRUEHD") -> "Dolby TrueHD"
        upper.contains("DTS-HD") -> "DTS-HD MA"
        upper.contains("DTS") -> "DTS"
        upper.contains("AAC") -> "AAC"
        upper.contains("FLAC") -> "FLAC Hi-Res"
        upper.contains("OPUS") -> "OPUS"
        upper.contains("VORBIS") -> "Vorbis"
        upper.contains("MPEG") || upper.contains("MP3") || upper.contains("MP4A") -> "MP3"
        else -> upper.substringAfter("/")
    }
}

fun formatChannelLayout(channelCount: Int): String {
    return when (channelCount) {
        1 -> "Mono (1.0)"
        2 -> "Stereo (2.0)"
        6 -> "5.1 Surround"
        8 -> "7.1 Surround"
        in 3..5 -> "$channelCount.0 Channels"
        else -> if (channelCount > 0) "$channelCount Ch" else ""
    }
}

fun formatSubtitleCodec(mimeType: String?): String {
    if (mimeType == null) return "SUBTITLE"
    val upper = mimeType.uppercase()
    return when {
        upper.contains("SUBRIP") || upper.contains("X-SUBRIP") -> "SRT"
        upper.contains("VTT") || upper.contains("WEBVTT") -> "WebVTT"
        upper.contains("SSA") || upper.contains("ASS") -> "ASS / SSA"
        upper.contains("PGS") -> "PGS"
        upper.contains("VOBSUB") -> "VobSub"
        upper.contains("TTML") -> "TTML"
        else -> upper.substringAfter("/")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorBottomSheet(
    player: Player,
    initialTab: Int = 0,
    audioEffectManager: AudioEffectManager? = null,
    audioQualityReport: AudioQualityAnalyzer.AudioQualityReport? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    onLoadExternalSubtitle: () -> Unit = {},
    onOpenSubtitleStyle: () -> Unit = {}
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVol = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    val initialStreamVolPercent = remember {
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: maxVol
        (cur.toFloat() / maxVol.toFloat()) * 100f
    }
    val currentBoost = audioEffectManager?.volumeBoostPercent ?: 0
    var totalVolumeSlider by remember {
        mutableFloatStateOf(if (currentBoost > 0) 100f + currentBoost else initialStreamVolPercent)
    }
    var dialogueClarity by remember {
        mutableStateOf(audioEffectManager?.isDialogueClarityEnabled ?: false)
    }
    var isBassEnabled by remember {
        mutableStateOf(audioEffectManager?.isBassBoostEnabled ?: false)
    }
    var bassStrength by remember {
        mutableFloatStateOf((audioEffectManager?.bassStrength ?: 0).toFloat() / 10f)
    }
    var isVirtualizerEnabled by remember {
        mutableStateOf(audioEffectManager?.isVirtualizerEnabled ?: false)
    }
    var virtualizerStrength by remember {
        mutableFloatStateOf((audioEffectManager?.virtualizerStrength ?: 0).toFloat() / 10f)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audioListState = rememberLazyListState()
    val subtitleListState = rememberLazyListState()

    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setBackgroundBlurRadius(60)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xF00D111A), // Sleek OLED glassmorphic dark container
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
            // Compact Drag Capsule
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

            // Modern Dual-Tab Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabPill(
                        title = "Audio & Effects",
                        icon = Icons.Default.Audiotrack,
                        isSelected = selectedTab == 0,
                        activeColor = ToolIconPalette.Audio,
                        onClick = { selectedTab = 0 }
                    )
                    TabPill(
                        title = "Subtitles",
                        icon = Icons.Default.Subtitles,
                        isSelected = selectedTab == 1,
                        activeColor = ToolIconPalette.Subtitles,
                        onClick = { selectedTab = 1 }
                    )
                }

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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(top = 4.dp)
            )

            val tracks = remember(player.currentTracks) { player.currentTracks.groups }

            LazyColumn(
                state = if (selectedTab == 0) audioListState else subtitleListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // ==========================================
                    // 1. AUDIO TRACKS SECTION
                    // ==========================================
                    item(key = "audio_tracks_header") {
                        val audioGroups = tracks.filter { it.type == C.TRACK_TYPE_AUDIO }
                        var totalTracks = 0
                        audioGroups.forEach { totalTracks += it.length }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Audio Tracks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ToolIconPalette.Audio.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalTracks Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ToolIconPalette.Audio
                                )
                            }
                        }
                    }

                    // Mute Audio Option
                    item(key = "audio_tracks_mute") {
                        val audioGroups = tracks.filter { it.type == C.TRACK_TYPE_AUDIO }
                        val isMuted = audioGroups.none { it.isSelected }

                        ModernTrackCard(
                            title = "Disable Audio Stream",
                            subtitle = "Mute audio playback completely",
                            isSelected = isMuted,
                            accentColor = Color(0xFFFF5252),
                            leadingIcon = Icons.AutoMirrored.Filled.VolumeOff,
                            chips = emptyList(),
                            onClick = {
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                                    .build()
                            }
                        )
                    }

                    // Dynamic Audio Tracks
                    val audioGroups = tracks.filter { it.type == C.TRACK_TYPE_AUDIO }
                    if (audioGroups.isEmpty()) {
                        item(key = "audio_tracks_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No additional audio tracks found in this stream",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        audioGroups.forEachIndexed { groupIdx, group ->
                            items(
                                count = group.length,
                                key = { trackIndex -> "audio_${groupIdx}_${group.mediaTrackGroup.id}_$trackIndex" }
                            ) { trackIndex ->
                                val format = group.getTrackFormat(trackIndex)
                                val isSelected = group.isTrackSelected(trackIndex)
                                val languageCode = format.language ?: "und"
                                val localeName = getLanguageName(languageCode)
                                val codecName = formatAudioCodec(format.sampleMimeType)
                                val channelText = formatChannelLayout(format.channelCount)

                                val title = if (localeName != "Unknown Language") {
                                    localeName
                                } else {
                                    format.label ?: "Audio Track ${trackIndex + 1}"
                                }

                                val subtitle = format.label?.takeIf { it != title } ?: "Stream Channel #${trackIndex + 1}"

                                val chips = mutableListOf<String>()
                                if (codecName.isNotBlank()) chips.add(codecName)
                                if (channelText.isNotBlank()) chips.add(channelText)
                                if (format.sampleRate > 0) chips.add("${format.sampleRate / 1000.0} kHz")
                                if (format.bitrate > 0) chips.add("${format.bitrate / 1000} kbps")

                                ModernTrackCard(
                                    title = title,
                                    subtitle = subtitle,
                                    isSelected = isSelected,
                                    accentColor = ToolIconPalette.Audio,
                                    leadingIcon = Icons.Default.Audiotrack,
                                    chips = chips,
                                    onClick = {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            .build()
                                    }
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 2. LIVE STREAM FIDELITY & DIAGNOSTICS
                    // ==========================================
                    if (audioQualityReport != null) {
                        item(key = "audio_diagnostics") {
                            Spacer(modifier = Modifier.height(4.dp))
                            AudioDiagnosticsCard(report = audioQualityReport)
                        }
                    }

                    // ==========================================
                    // 3. SOUND ENHANCEMENTS & HARDWARE CONTROLS
                    // ==========================================
                    item(key = "audio_fx_header") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = ToolIconPalette.Audio,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sound Enhancements & FX",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Unified Volume & Hardware Boost Slider (0% to 200%)
                    item(key = "audio_volume_card") {
                        val currentTotalVol = totalVolumeSlider.roundToInt()
                        val isBoostActive = currentTotalVol > 100
                        val isExtremeBoost = currentTotalVol > 150
                        val activeAccent = when {
                            isExtremeBoost -> Color(0xFFFF3D00)
                            isBoostActive -> Color(0xFFFF9800)
                            else -> ToolIconPalette.Audio
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(
                                1.dp,
                                if (isBoostActive) activeAccent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                            ),
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
                                            imageVector = if (currentTotalVol == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = activeAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isBoostActive) "Audio Boost (Hardware Gain)" else "Playback Volume",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (isBoostActive) "LoudnessEnhancer active past 100%" else "Slide past 100% to force Audio Boost",
                                                fontSize = 11.sp,
                                                color = if (isBoostActive) activeAccent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(activeAccent.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                currentTotalVol == 0 -> "0% (Muted)"
                                                !isBoostActive -> "$currentTotalVol% (Standard)"
                                                else -> {
                                                    val boostGain = (currentTotalVol - 100) * 0.15f
                                                    "$currentTotalVol% (+${String.format(Locale.US, "%.1f", boostGain)} dB Boost)"
                                                }
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Slider(
                                    value = totalVolumeSlider,
                                    onValueChange = { newVol ->
                                        totalVolumeSlider = newVol
                                        if (newVol <= 100f) {
                                            if ((audioEffectManager?.volumeBoostPercent ?: 0) > 0) {
                                                audioEffectManager?.volumeBoostPercent = 0
                                            }
                                            val streamTarget = ((newVol / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)
                                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, streamTarget, 0)
                                        } else {
                                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                                            val boost = (newVol - 100f).roundToInt().coerceIn(0, 100)
                                            audioEffectManager?.volumeBoostPercent = boost
                                        }
                                    },
                                    valueRange = 0f..200f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = activeAccent,
                                        activeTrackColor = activeAccent,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("0%", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                    Text("100% (Std Max)", fontSize = 11.sp, color = ToolIconPalette.Audio)
                                    Text("200% (Max Boost)", fontSize = 11.sp, color = Color(0xFFFF3D00))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(0 to "Mute", 50 to "50%", 100 to "100%", 150 to "150%", 200 to "200%").forEach { (preset, label) ->
                                        val isSelectedPreset = currentTotalVol == preset
                                        val presetAccent = when {
                                            preset > 150 -> Color(0xFFFF3D00)
                                            preset > 100 -> Color(0xFFFF9800)
                                            else -> ToolIconPalette.Audio
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelectedPreset) presetAccent.copy(alpha = 0.25f)
                                                    else Color.White.copy(alpha = 0.06f)
                                                )
                                                .clickable {
                                                    totalVolumeSlider = preset.toFloat()
                                                    if (preset <= 100) {
                                                        audioEffectManager?.volumeBoostPercent = 0
                                                        val streamTarget = ((preset.toFloat() / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, streamTarget, 0)
                                                    } else {
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                                                        audioEffectManager?.volumeBoostPercent = preset - 100
                                                    }
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelectedPreset) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelectedPreset) presetAccent else Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dialogue / Vocal Clarity Toggle
                    item(key = "audio_dialogue_card") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(
                                1.dp,
                                if (dialogueClarity) ToolIconPalette.AutoAI.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (dialogueClarity) ToolIconPalette.AutoAI.copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.08f)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (dialogueClarity) ToolIconPalette.AutoAI else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Dialogue & Vocal Clarity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Amplifies speech frequencies (1kHz–4kHz) and cleans background rumble.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.55f),
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Switch(
                                    checked = dialogueClarity,
                                    onCheckedChange = {
                                        dialogueClarity = it
                                        audioEffectManager?.isDialogueClarityEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ToolIconPalette.AutoAI,
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }

                    // Bass Boost & 3D Spatializer in sleek dual cards
                    item(key = "audio_bass_spatial_card") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Bass Boost Row
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speaker,
                                            contentDescription = null,
                                            tint = if (isBassEnabled) ToolIconPalette.Equalizer else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Bass Boost",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Acoustic low-frequency punch",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                        Switch(
                                            checked = isBassEnabled,
                                            onCheckedChange = {
                                                isBassEnabled = it
                                                audioEffectManager?.isBassBoostEnabled = it
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = ToolIconPalette.Equalizer
                                            )
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isBassEnabled,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            Slider(
                                                value = bassStrength,
                                                onValueChange = {
                                                    bassStrength = it
                                                    audioEffectManager?.bassStrength = (it * 10).toInt()
                                                },
                                                valueRange = 0f..100f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = ToolIconPalette.Equalizer,
                                                    activeTrackColor = ToolIconPalette.Equalizer,
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Subtle", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                                Text("${bassStrength.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ToolIconPalette.Equalizer)
                                                Text("Heavy Bass", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                // 3D Spatializer Row
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SyncAlt,
                                            contentDescription = null,
                                            tint = if (isVirtualizerEnabled) ToolIconPalette.Audio else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "3D Spatializer (Virtualizer)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Wide soundstage for headphones & stereo",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                        Switch(
                                            checked = isVirtualizerEnabled,
                                            onCheckedChange = {
                                                isVirtualizerEnabled = it
                                                audioEffectManager?.isVirtualizerEnabled = it
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = ToolIconPalette.Audio
                                            )
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isVirtualizerEnabled,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            Slider(
                                                value = virtualizerStrength,
                                                onValueChange = {
                                                    virtualizerStrength = it
                                                    audioEffectManager?.virtualizerStrength = (it * 10).toInt()
                                                },
                                                valueRange = 0f..100f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = ToolIconPalette.Audio,
                                                    activeTrackColor = ToolIconPalette.Audio,
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Narrow", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                                Text("${virtualizerStrength.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ToolIconPalette.Audio)
                                                Text("Immersive 3D", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Open 10-Band Graphic Equalizer
                    item(key = "audio_equalizer_card") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ToolIconPalette.Equalizer.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, ToolIconPalette.Equalizer.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenEqualizer?.invoke()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(ToolIconPalette.Equalizer.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = ToolIconPalette.Equalizer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Open 10-Band Graphic Equalizer",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Presets, custom frequency curves & preamp",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open",
                                    tint = ToolIconPalette.Equalizer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // 4. SUBTITLES SECTION
                    // ==========================================
                    item(key = "sub_tracks_header") {
                        val textGroups = tracks.filter { it.type == C.TRACK_TYPE_TEXT }
                        var totalSubs = 0
                        textGroups.forEach { totalSubs += it.length }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Subtitle Tracks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ToolIconPalette.Subtitles.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalSubs Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ToolIconPalette.Subtitles
                                )
                            }
                        }
                    }

                    // Disable Subtitles Option
                    item(key = "sub_tracks_off") {
                        val textGroups = tracks.filter { it.type == C.TRACK_TYPE_TEXT }
                        val isOffSelected = textGroups.none { it.isSelected }

                        ModernTrackCard(
                            title = "Disable Subtitles",
                            subtitle = "Hide all on-screen subtitles",
                            isSelected = isOffSelected,
                            accentColor = Color(0xFFFF5252),
                            leadingIcon = Icons.Default.Close,
                            chips = emptyList(),
                            onClick = {
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT.inv())
                                    .build()
                            }
                        )
                    }

                    // Subtitle Tracks List
                    val textGroups = tracks.filter { it.type == C.TRACK_TYPE_TEXT }
                    if (textGroups.isEmpty()) {
                        item(key = "sub_tracks_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No embedded subtitles found in this media",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        textGroups.forEachIndexed { groupIdx, group ->
                            items(
                                count = group.length,
                                key = { trackIndex -> "sub_${groupIdx}_${group.mediaTrackGroup.id}_$trackIndex" }
                            ) { trackIndex ->
                                val format = group.getTrackFormat(trackIndex)
                                val isSelected = group.isTrackSelected(trackIndex)
                                val languageCode = format.language ?: "und"
                                val localeName = getLanguageName(languageCode)
                                val codecName = formatSubtitleCodec(format.sampleMimeType)

                                val title = if (localeName != "Unknown Language") {
                                    localeName
                                } else {
                                    format.label ?: "Subtitle Track ${trackIndex + 1}"
                                }

                                val subtitle = format.label?.takeIf { it != title } ?: "Embedded Stream #${trackIndex + 1}"

                                val chips = mutableListOf<String>()
                                if (codecName.isNotBlank()) chips.add(codecName)

                                ModernTrackCard(
                                    title = title,
                                    subtitle = subtitle,
                                    isSelected = isSelected,
                                    accentColor = ToolIconPalette.Subtitles,
                                    leadingIcon = Icons.Default.Subtitles,
                                    chips = chips,
                                    onClick = {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            .build()
                                    }
                                )
                            }
                        }
                    }

                    // Import External Subtitle
                    item(key = "sub_external_file_card") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "External Subtitles & Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLoadExternalSubtitle()
                                    onDismissRequest()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ToolIconPalette.Subtitles.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = "Open",
                                        tint = ToolIconPalette.Subtitles,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Open Local Subtitle File",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Supports .srt, .vtt, .ass, .ssa formats",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Subtitle Customization
                    item(key = "sub_style_card") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenSubtitleStyle()
                                    onDismissRequest()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ToolIconPalette.Subtitles.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Customize",
                                        tint = ToolIconPalette.Subtitles,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Subtitle Style & Appearance",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Font size, colors, shadow, sync delay & encoding",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent,
        label = "tab_bg"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) activeColor else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ModernTrackCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    leadingIcon: ImageVector,
    chips: List<String>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.07f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (isSelected) accentColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (chips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chips.forEach { chipText ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = chipText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioDiagnosticsCard(report: AudioQualityAnalyzer.AudioQualityReport) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, ToolIconPalette.Audio.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (report.isClippingDetected) Color(0xFFFF5252) else Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE AUDIO SPECS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = if (report.isClippingDetected) "Signal Clipping" else "Optimal Signal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (report.isClippingDetected) Color(0xFFFF5252) else Color(0xFF00E676)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "LAYOUT",
                    value = report.channelLayout,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "SAMPLE RATE",
                    value = "${report.sampleRate / 1000.0} kHz",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "BITRATE",
                    value = "${report.bitrateKbps} kbps",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "DYN. RANGE",
                    value = "${String.format(Locale.US, "%.1f", report.dynamicRangeDb)} dB",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.45f)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}