package com.helpofai.videoplayer.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import java.util.Locale

import androidx.compose.ui.draw.blur

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
        val locale = Locale(twoLetter)
        val name = locale.displayLanguage
        if (name.lowercase() == twoLetter.lowercase()) code.uppercase() else name
    } catch (e: Exception) {
        code.uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorBottomSheet(
    player: Player,
    initialTab: Int = 0,
    onDismissRequest: () -> Unit,
    onLoadExternalSubtitle: () -> Unit = {}
) {
    // Stub states for advanced features
    var useSwDecoder by remember { mutableStateOf(false) }
    var avSyncDelay by remember { mutableStateOf(0f) }
    var stereoMode by remember { mutableStateOf("Stereo") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xCC000000), // Glassmorphism translucent background
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.7f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (initialTab == 0) Icons.Default.Audiotrack else Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (initialTab == 0) "Audio Tracks & Features" else "Subtitles & Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            val tracks = player.currentTracks.groups

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (initialTab == 0) {
                    // ================= AUDIO TAB =================
                    item {
                        SectionHeader("Audio Tracks")
                    }

                    val audioGroups = tracks.filter { it.type == C.TRACK_TYPE_AUDIO }
                    item {
                        val isOffSelected = audioGroups.none { it.isSelected }
                        TrackItemRow(
                            title = "Disable Audio",
                            subtitle = "Mute the video stream completely",
                            isSelected = isOffSelected,
                            onClick = {
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                                    .build()
                                onDismissRequest()
                            }
                        )
                    }

                    if (audioGroups.isEmpty()) {
                        item { Text("No additional audio tracks", modifier = Modifier.padding(16.dp), color = Color.Gray) }
                    } else {
                        audioGroups.forEach { group ->
                            items(group.length) { trackIndex ->
                                val format = group.getTrackFormat(trackIndex)
                                val isSelected = group.isTrackSelected(trackIndex)
                                val languageCode = format.language ?: "und"
                                val localeName = getLanguageName(languageCode)
                                val codec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: "UNKNOWN"
                                val channels = if (format.channelCount > 0) "${format.channelCount} Ch" else ""
                                
                                val title = if (localeName != "Unknown Language") localeName else format.label ?: "Track ${trackIndex + 1}"
                                val subtitle = buildString {
                                    if (!format.label.isNullOrBlank() && format.label != title) {
                                        append(format.label).append(" • ")
                                    }
                                    append("Auto Detected • $codec $channels")
                                }

                                TrackItemRow(
                                    title = title,
                                    subtitle = subtitle,
                                    isSelected = isSelected,
                                    onClick = {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            .build()
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader("Advanced Audio Settings")
                        
                        // SW Decoder Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { useSwDecoder = !useSwDecoder }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use SW Audio Decoder", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Text("Advanced level. May fix format issues.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Switch(checked = useSwDecoder, onCheckedChange = { useSwDecoder = it })
                        }
                        
                        // Stereo Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    stereoMode = when(stereoMode) {
                                        "Stereo" -> "Mono"
                                        "Mono" -> "Surround"
                                        else -> "Stereo"
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speaker, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Stereo Mode", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Text(stereoMode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // AV Sync
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("AV Sync (Audio Delay)", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("${String.format("%.1f", avSyncDelay)}s", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = avSyncDelay,
                                onValueChange = { avSyncDelay = it },
                                valueRange = -1.5f..1.5f,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    // ================= SUBTITLES TAB =================
                    item {
                        SectionHeader("Subtitle Tracks")
                    }

                    val textGroups = tracks.filter { it.type == C.TRACK_TYPE_TEXT }
                    item {
                        val isOffSelected = textGroups.none { it.isSelected }
                        TrackItemRow(
                            title = "Disable Subtitles",
                            subtitle = "Turn off subtitles",
                            isSelected = isOffSelected,
                            onClick = {
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT.inv())
                                    .build()
                                onDismissRequest()
                            }
                        )
                    }

                    if (textGroups.isNotEmpty()) {
                        textGroups.forEach { group ->
                            items(group.length) { trackIndex ->
                                val format = group.getTrackFormat(trackIndex)
                                val isSelected = group.isTrackSelected(trackIndex)
                                val languageCode = format.language ?: "und"
                                val localeName = getLanguageName(languageCode)
                                val codec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: "UNKNOWN"
                                val title = if (localeName != "Unknown Language") localeName else format.label ?: "Track ${trackIndex + 1}"
                                val subtitle = buildString {
                                    if (!format.label.isNullOrBlank() && format.label != title) {
                                        append(format.label).append(" • ")
                                    }
                                    append("Auto Detected • $codec")
                                }
                                
                                TrackItemRow(
                                    title = title,
                                    subtitle = subtitle,
                                    isSelected = isSelected,
                                    onClick = {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            .build()
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader("Import Subtitle")
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { 
                                    onLoadExternalSubtitle()
                                    onDismissRequest()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Open Local Subtitle", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(".srt, .vtt, .ass", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(48.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun TrackItemRow(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Gray
                )
            }
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
