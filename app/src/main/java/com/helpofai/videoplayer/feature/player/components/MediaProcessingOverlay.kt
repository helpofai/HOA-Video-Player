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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.playback.PlaybackState
import com.helpofai.videoplayer.core.playback.diagnostics.MediaAnalyzer
import com.helpofai.videoplayer.core.theme.ToolIconPalette
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Structured media stream attributes for display in the processing media card.
 */
data class MediaStreamSpecs(
    val resolution: String? = null,
    val frameRate: String? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val audioChannels: String? = null,
    val bitrate: String? = null,
    val isHdr: Boolean = false,
    val hdrType: String? = null,
    val decoderMode: String = "HW"
)

/**
 * Helper to build clean [MediaStreamSpecs] from live playback state, media analyzer report, and exo tracks.
 */
fun buildMediaStreamSpecs(
    playbackState: PlaybackState?,
    report: MediaAnalyzer.MediaCompatibilityReport?,
    decoderMode: String
): MediaStreamSpecs {
    var videoCodec: String? = null
    var width = 0
    var height = 0
    var fps = 0f
    var bitrateBps = 0
    var audioCodec: String? = null
    var audioChannels = 0
    var isHdr = false
    var hdrType: String? = null

    // 1. From live playback state
    if (playbackState != null) {
        if (playbackState.videoCodec.isNotBlank() && playbackState.videoCodec != "Unknown") {
            videoCodec = cleanVideoCodecName(playbackState.videoCodec)
        }
        if (playbackState.videoWidth > 0) width = playbackState.videoWidth
        if (playbackState.videoHeight > 0) height = playbackState.videoHeight
        if (playbackState.videoFps > 0f) fps = playbackState.videoFps
        if (playbackState.videoBitrate > 0) bitrateBps = playbackState.videoBitrate
        if (playbackState.audioCodec.isNotBlank() && playbackState.audioCodec != "Unknown") {
            audioCodec = cleanAudioCodecName(playbackState.audioCodec)
        }
        if (playbackState.isHdr) isHdr = true
    }

    // 2. From MediaCompatibilityReport
    if (report != null) {
        if (videoCodec == null && !report.videoCodec.isNullOrBlank()) {
            videoCodec = cleanVideoCodecName(report.videoCodec)
        }
        if (width <= 0 && report.width > 0) width = report.width
        if (height <= 0 && report.height > 0) height = report.height
        if (fps <= 0f && report.fps > 0f) fps = report.fps
        if (bitrateBps <= 0 && report.totalBitrateBps > 0) bitrateBps = report.totalBitrateBps
        if (audioCodec == null && !report.audioCodec.isNullOrBlank()) {
            audioCodec = cleanAudioCodecName(report.audioCodec)
        }
        if (report.audioChannels > 0) audioChannels = report.audioChannels
        if (report.isHdr) {
            isHdr = true
            hdrType = report.hdrType
        }
    }

    val resString = if (width > 0 && height > 0) {
        val label = when {
            width >= 3800 || height >= 2100 -> "4K UHD"
            width >= 2500 || height >= 1400 -> "2K QHD"
            width >= 1900 || height >= 1000 -> "1080p FHD"
            width >= 1200 || height >= 700 -> "720p HD"
            height >= 480 -> "480p SD"
            else -> null
        }
        if (label != null) "$label (${width}×$height)" else "${width}×$height"
    } else null

    val fpsString = if (fps > 0f) {
        String.format(Locale.US, "%.0f FPS", fps)
    } else null

    val bitrateString = when {
        bitrateBps >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", bitrateBps / 1_000_000.0)
        bitrateBps > 0 -> "${bitrateBps / 1000} kbps"
        else -> null
    }

    val channelString = when (audioChannels) {
        1 -> "Mono 1.0"
        2 -> "Stereo 2.0"
        6 -> "5.1 Surround"
        8 -> "7.1 Surround"
        else -> if (audioChannels > 0) "$audioChannels Ch" else null
    }

    return MediaStreamSpecs(
        resolution = resString,
        frameRate = fpsString,
        videoCodec = videoCodec,
        audioCodec = audioCodec,
        audioChannels = channelString,
        bitrate = bitrateString,
        isHdr = isHdr,
        hdrType = hdrType,
        decoderMode = decoderMode
    )
}

private fun cleanVideoCodecName(raw: String): String {
    val lower = raw.lowercase()
    return when {
        "avc" in lower || "h264" in lower -> "H.264 / AVC"
        "hevc" in lower || "h265" in lower -> "H.265 / HEVC"
        "vp9" in lower -> "VP9"
        "vp8" in lower -> "VP8"
        "av01" in lower || "av1" in lower -> "AV1"
        "mp4v" in lower || "mpeg4" in lower -> "MPEG-4"
        "mjpeg" in lower -> "Motion JPEG"
        "theora" in lower -> "Theora"
        else -> raw.substringAfter("/").uppercase()
    }
}

private fun cleanAudioCodecName(raw: String): String {
    val lower = raw.lowercase()
    return when {
        "mp4a" in lower || "aac" in lower -> "AAC"
        "eac3-joc" in lower -> "Dolby Atmos"
        "eac3" in lower -> "Dolby Digital+ (E-AC-3)"
        "ac3" in lower -> "Dolby Digital (AC-3)"
        "opus" in lower -> "Opus"
        "flac" in lower -> "FLAC Lossless"
        "mpeg" in lower || "mp3" in lower -> "MP3"
        "vorbis" in lower -> "Vorbis"
        "true-hd" in lower || "truehd" in lower -> "Dolby TrueHD"
        "dts-hd" in lower -> "DTS-HD"
        "dts" in lower -> "DTS"
        "pcm" in lower || "raw" in lower -> "PCM Lossless"
        else -> raw.substringAfter("/").uppercase()
    }
}

/**
 * Modern, cinematic buffering and stream pipeline processing overlay.
 *
 * Differentiates between:
 * - Seek / playback buffering: Lightweight, elegant glowing spinner (non-blocking).
 * - Initial stream loading: Glassmorphic media pipeline specs card with chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaBufferingOverlay(
    isBuffering: Boolean,
    isInitialLoad: Boolean,
    specs: MediaStreamSpecs?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isBuffering,
        enter = fadeIn(tween(220)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.92f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f),
        modifier = modifier
    ) {
        if (!isInitialLoad) {
            // Sleek, unobtrusive cinematic glowing spinner for seeking / mid-stream buffering
            CinematicSeekingSpinner()
        } else {
            // High-tech glassmorphic specs card for initial media preparation
            InitialMediaPipelineCard(specs = specs)
        }
    }
}

/**
 * Non-intrusive centered dual-ring glowing spinner used during active scrubbing / seek buffering.
 */
@Composable
private fun CinematicSeekingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "seeking_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xB30A0E17))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(46.dp).rotate(rotation)) {
            val strokeWidth = 3.5.dp.toPx()
            val gradientBrush = Brush.sweepGradient(
                listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF7C4DFF),
                    Color(0x0000E5FF)
                )
            )
            drawArc(
                brush = gradientBrush,
                startAngle = 0f,
                sweepAngle = 280f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Ambient glowing center beacon
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(pulseAlpha)
                .background(Color(0xFF00E5FF).copy(alpha = pulseAlpha), CircleShape)
        )
    }
}

/**
 * Modern glassmorphic specs card shown during initial media container decoding and pipeline configuration.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InitialMediaPipelineCard(
    specs: MediaStreamSpecs?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pipeline_glow")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    val hasAnySpecs = specs != null && (
        specs.resolution != null ||
        specs.videoCodec != null ||
        specs.audioCodec != null ||
        specs.bitrate != null
    )

    Surface(
        modifier = Modifier
            .widthIn(min = 280.dp, max = 460.dp)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xE60D1117),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.55f),
                    Color(0xFF7C4DFF).copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        ),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Rotating gradient ring with hardware decoder glyph
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .rotate(ringRotation)
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF7C4DFF),
                                    Color.Transparent
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PROCESSING MEDIA",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = "Configuring hardware decoding & stream pipelines",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specs badges or parsing placeholder
            if (hasAnySpecs) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Video Codec Badge
                    specs.videoCodec?.let { codec ->
                        SpecChip(
                            icon = Icons.Default.Movie,
                            text = codec,
                            accentColor = Color(0xFF40C4FF)
                        )
                    }

                    // Resolution & FPS Badge
                    specs.resolution?.let { res ->
                        val text = if (specs.frameRate != null) "$res • ${specs.frameRate}" else res
                        SpecChip(
                            icon = Icons.Default.HighQuality,
                            text = text,
                            accentColor = ToolIconPalette.HQ
                        )
                    }

                    // Audio Codec & Channels Badge
                    specs.audioCodec?.let { audio ->
                        val text = if (specs.audioChannels != null) "$audio • ${specs.audioChannels}" else audio
                        SpecChip(
                            icon = Icons.Default.GraphicEq,
                            text = text,
                            accentColor = ToolIconPalette.Audio
                        )
                    }

                    // Bitrate Badge
                    specs.bitrate?.let { bitrate ->
                        SpecChip(
                            icon = Icons.Default.Speed,
                            text = bitrate,
                            accentColor = Color(0xFFFF9100)
                        )
                    }

                    // Decoder Acceleration Mode
                    SpecChip(
                        icon = Icons.Default.Memory,
                        text = "${specs.decoderMode} Decoded",
                        accentColor = Color(0xFF7C4DFF)
                    )

                    // HDR Tag if active
                    if (specs.isHdr) {
                        SpecChip(
                            icon = Icons.Default.HighQuality,
                            text = specs.hdrType ?: "HDR10",
                            accentColor = Color(0xFFFF4081)
                        )
                    }
                }
            } else {
                // Parsing streams animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Analyzing container formats & codec descriptors...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated glowing gradient shimmer track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val beamWidth = w * 0.45f
                    val startX = w * shimmerOffset
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF00E5FF),
                                Color(0xFF7C4DFF),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + beamWidth, 0f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Clean pill chip for individual media stream specifications.
 */
@Composable
private fun SpecChip(
    icon: ImageVector,
    text: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161B22))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 11.sp
        )
    }
}

/**
 * Modern glassmorphic dialog shown during on-demand Auto AI video analysis.
 */
@Composable
fun AIAnalyzingOverlay(
    isAnalyzing: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isAnalyzing,
        enter = fadeIn(tween(250)) + scaleIn(tween(250, easing = FastOutSlowInEasing), initialScale = 0.90f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.90f),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "ai_scanning")
        val ringRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "aiRotation"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aiPulse"
        )

        var stepIndex by remember { mutableIntStateOf(0) }
        val steps = remember {
            listOf(
                "Sampling frame luminance & dynamic range...",
                "Balancing color saturation & contrast curves...",
                "Synthesizing optimal cinema visual profile..."
            )
        }

        LaunchedEffect(isAnalyzing) {
            if (isAnalyzing) {
                stepIndex = 0
                while (true) {
                    delay(1200)
                    stepIndex = (stepIndex + 1) % steps.size
                }
            }
        }

        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 400.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF20D1117),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        ToolIconPalette.AutoAI,
                        ToolIconPalette.VideoEnhancer,
                        Color(0xFF00E5FF)
                    )
                )
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated AI aperture / neural glyph
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .rotate(ringRotation)
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    ToolIconPalette.AutoAI,
                                    ToolIconPalette.VideoEnhancer,
                                    Color.Transparent
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ToolIconPalette.AutoAI,
                        modifier = Modifier
                            .size(26.dp)
                            .scale(pulseScale)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "AI ENHANCING VIDEO",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 1.3.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = steps[stepIndex],
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "stepAnimation"
                ) { stepText ->
                    Text(
                        text = stepText,
                        color = Color.White.copy(alpha = 0.80f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        minLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom AI neural progress wave
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    val shimmerTransition = rememberInfiniteTransition(label = "ai_wave")
                    val shimmerPos by shimmerTransition.animateFloat(
                        initialValue = -0.5f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1400, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "aiShimmerPos"
                    )

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val span = w * 0.4f
                        val sx = w * shimmerPos
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ToolIconPalette.AutoAI,
                                    ToolIconPalette.VideoEnhancer,
                                    Color.Transparent
                                ),
                                start = Offset(sx, 0f),
                                end = Offset(sx + span, 0f)
                            )
                        )
                    }
                }
            }
        }
    }
}
