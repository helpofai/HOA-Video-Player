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
package com.helpofai.videoplayer.core.playback.diagnostics

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AutoAIEnhancementEngine
 *
 * Advanced, per-video enhancement analyzer that runs ONLY when the user taps the
 * "Auto AI" button for the CURRENT video. It samples actual frames from the video
 * (luminance, contrast, colorfulness, darkness ratio), merges those with the
 * MediaAnalyzer compatibility report, and produces a tailored enhancement config
 * for that single video. Settings are persisted per-video (keyed by path hash)
 * so the same file gets the same treatment on next playback.
 */
@Singleton
class AutoAIEnhancementEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Result of one per-video analysis run. */
    data class AnalysisResult(
        val videoPath: String,
        val contentType: String,          // "movie", "anime", "sports", "low_light", "documentary", "music_video", "generic"
        val avgLuminance: Float,          // 0..1 average frame brightness
        val luminanceStdDev: Float,       // spread of brightness across sampled frames
        val darkFrameRatio: Float,        // 0..1 fraction of very dark frames
        val contrastScore: Float,         // 0..1 estimated global contrast
        val colorfulness: Float,          // 0..1 estimated color saturation level
        val frameCountAnalyzed: Int,
        val config: VideoEnhancementManager.VideoEnhancementConfig
    )

    private val prefs = context.getSharedPreferences("auto_ai_enhancements", Context.MODE_PRIVATE)

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Runs the full analysis for the given video. Should be called from a
     * coroutine on Dispatchers.IO (it blocks on frame extraction).
     */
    suspend fun analyzeVideo(
        path: String,
        report: MediaAnalyzer.MediaCompatibilityReport?
    ): AnalysisResult = withContext(Dispatchers.IO) {
        // 1. Sample frames and compute content metrics
        val frameStats = sampleFrameMetrics(path)

        // 2. Merge frame metrics + media report into a config
        val contentType = classifyContent(path, report, frameStats)
        val config = buildConfig(contentType, report, frameStats)

        // 3. Persist per-video so it survives across sessions
        saveForVideo(path, config, contentType)

        AnalysisResult(
            videoPath = path,
            contentType = contentType,
            avgLuminance = frameStats.avgLuminance,
            luminanceStdDev = frameStats.luminanceStdDev,
            darkFrameRatio = frameStats.darkFrameRatio,
            contrastScore = frameStats.contrastScore,
            colorfulness = frameStats.colorfulness,
            frameCountAnalyzed = frameStats.frameCount,
            config = config
        )
    }

    /** Restore the last AI config saved for this video, if any. */
    fun loadForVideo(path: String): VideoEnhancementManager.VideoEnhancementConfig? {
        val key = hashKey(path)
        if (!prefs.contains("${key}_preset")) return null
        return VideoEnhancementManager.VideoEnhancementConfig(
            autoEnhance = false,
            preset = "ai_${prefs.getString("${key}_content", "generic")}",
            strength = prefs.getFloat("${key}_strength", 0.45f),
            brightness = prefs.getFloat("${key}_brightness", 0f),
            contrast = prefs.getFloat("${key}_contrast", 0f),
            saturation = prefs.getFloat("${key}_saturation", 0f),
            vibrance = prefs.getFloat("${key}_vibrance", 0f),
            gamma = prefs.getFloat("${key}_gamma", 1.0f),
            colorTemperature = prefs.getFloat("${key}_colorTemp", 0f),
            sharpness = prefs.getFloat("${key}_sharpness", 0f),
            edgeEnhancement = prefs.getFloat("${key}_edge", 0f),
            noiseReduction = prefs.getFloat("${key}_noise", 0f),
            textureEnhancement = prefs.getFloat("${key}_texture", 0f),
            hdrProcessing = prefs.getBoolean("${key}_hdr", false),
            toneMapping = prefs.getBoolean("${key}_tone", false),
            frameOptimization = prefs.getBoolean("${key}_frameOpt", false),
            colorCorrection = prefs.getBoolean("${key}_colorCorr", false)
        )
    }

    /** Whether this video already has a saved AI enhancement. */
    fun hasSavedForVideo(path: String): Boolean {
        val key = hashKey(path)
        return prefs.contains("${key}_preset")
    }

    /** Forget the AI enhancement for this video (restore original). */
    fun clearForVideo(path: String) {
        val key = hashKey(path)
        val editor = prefs.edit()
        listOf(
            "preset", "content", "strength", "brightness", "contrast", "saturation",
            "vibrance", "gamma", "colorTemp", "sharpness", "edge", "noise",
            "texture", "hdr", "tone", "frameOpt", "colorCorr"
        ).forEach { editor.remove("${key}_$it") }
        editor.apply()
    }

    // ---------------------------------------------------------------------
    // Frame sampling & metrics
    // ---------------------------------------------------------------------

    private data class FrameMetrics(
        val avgLuminance: Float,
        val luminanceStdDev: Float,
        val darkFrameRatio: Float,
        val contrastScore: Float,
        val colorfulness: Float,
        val frameCount: Int
    )

    private fun sampleFrameMetrics(path: String): FrameMetrics {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            val uri = android.net.Uri.fromFile(java.io.File(path))
            retriever.setDataSource(context, uri)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) {
                return FrameMetrics(0.5f, 0f, 0f, 0.5f, 0.5f, 0)
            }

            // Sample up to 12 evenly spaced frames across the video.
            val samples = 12
            val luminances = mutableListOf<Float>()
            var darkFrames = 0
            var colorSum = 0f

            for (i in 0 until samples) {
                val timeUs = (durationMs * i / samples) * 1000L
                val frame = try {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    null
                } ?: continue

                val (luma, color) = analyzeFrame(frame)
                luminances.add(luma)
                colorSum += color
                if (luma < 0.12f) darkFrames++
                frame.recycle()
            }

            if (luminances.isEmpty()) {
                return FrameMetrics(0.5f, 0f, 0f, 0.5f, 0.5f, 0)
            }

            val avg = luminances.average().toFloat()
            val variance = luminances.map { (it - avg) * (it - avg) }.average().toFloat()
            val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()

            // Contrast estimate: normalized spread of luminance.
            val contrast = (stdDev * 2.2f).coerceIn(0f, 1f)

            return FrameMetrics(
                avgLuminance = avg.coerceIn(0f, 1f),
                luminanceStdDev = stdDev.coerceIn(0f, 1f),
                darkFrameRatio = (darkFrames.toFloat() / luminances.size).coerceIn(0f, 1f),
                contrastScore = contrast,
                colorfulness = (colorSum / luminances.size).coerceIn(0f, 1f),
                frameCount = luminances.size
            )
        } catch (e: Exception) {
            // Retriever may fail on exotic containers; fall back to neutral stats.
            return FrameMetrics(0.5f, 0f, 0f, 0.5f, 0.5f, 0)
        } finally {
            try { retriever?.release() } catch (e: Exception) {}
        }
    }

    /**
     * Downscale a decoded frame and compute average luminance + colorfulness.
     * Uses luma coefficients (0.2126 R + 0.7152 G + 0.0722 B) to weight
     * brightness the way the human eye perceives it.
     */
    private fun analyzeFrame(frame: Bitmap): Pair<Float, Float> {
        val scale = 64f / maxOf(frame.width, frame.height).coerceAtLeast(1)
        val w = (frame.width * scale).toInt().coerceAtLeast(1)
        val h = (frame.height * scale).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(frame, w, h, true)

        var lumaSum = 0.0
        var colorSum = 0.0
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)

        for (p in pixels) {
            val r = (p shr 16 and 0xFF) / 255f
            val g = (p shr 8 and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            lumaSum += 0.2126 * r + 0.7152 * g + 0.0722 * b
            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            colorSum += (maxC - minC)  // chroma spread = colorfulness
        }

        small.recycle()
        val n = (w * h).toDouble()
        return Pair((lumaSum / n).toFloat(), (colorSum / n).toFloat())
    }

    // ---------------------------------------------------------------------
    // Content classification & config generation
    // ---------------------------------------------------------------------

    private fun classifyContent(
        path: String,
        report: MediaAnalyzer.MediaCompatibilityReport?,
        stats: FrameMetrics
    ): String {
        val lower = path.lowercase()

        // Name-based hints first
        if (lower.contains("anime") || lower.contains("cartoon") || lower.contains("animation")) return "anime"
        if (lower.contains("sport") || lower.contains("match") || lower.contains("game") || lower.contains("hls") || lower.contains("live")) return "sports"
        if (lower.contains("concert") || lower.contains("music") || lower.contains("song") || lower.contains("mv_") || lower.contains("video")) return "music_video"
        if (lower.contains("docu") || lower.contains("nature") || lower.contains("travel") || lower.contains("tutorial")) return "documentary"

        // Content-based detection from sampled frames
        val darkRatio = stats.darkFrameRatio
        val avg = stats.avgLuminance
        if (darkRatio > 0.45f && avg < 0.28f) return "low_light"

        // Movie vs generic: long duration + typical contrast suggests movie
        val durationSec = report?.durationMs?.div(1000f) ?: 0f
        if (durationSec > 45 * 60 && stats.contrastScore > 0.35f) return "movie"

        return "generic"
    }

    private fun buildConfig(
        contentType: String,
        report: MediaAnalyzer.MediaCompatibilityReport?,
        stats: FrameMetrics
    ): VideoEnhancementManager.VideoEnhancementConfig {
        val maxDim = maxOf(report?.width ?: 0, report?.height ?: 0)
        val isHdr = report?.isHdr == true

        // Base values
        var brightness = 0f
        var contrast = 0f
        var saturation = 0f
        var vibrance = 0f
        var gamma = 1.0f
        var colorTemp = 0f
        var sharpness = 0f
        var edge = 0f
        var noise = 0f
        var texture = 0f
        var frameOpt = false
        var colorCorr = false
        var toneMapping = false
        var strength = 0.45f

        // ---- Resolution tier ----
        when {
            maxDim <= 854 -> { noise = 0.3f; sharpness = 0.4f; edge = 0.25f; texture = 0.2f; strength = 0.6f }
            maxDim <= 1280 -> { sharpness = 0.2f; contrast = 0.05f; vibrance = 0.1f; strength = 0.5f }
            maxDim <= 1920 -> { contrast = 0.08f; vibrance = 0.12f; strength = 0.45f }
            else -> { contrast = 0.05f; strength = 0.35f } // 4K+: keep it subtle
        }

        // ---- Frame-driven corrections (the "AI" part) ----
        val avg = stats.avgLuminance
        val darkRatio = stats.darkFrameRatio

        // Too dark → brighten and recover shadows
        if (avg < 0.30f || darkRatio > 0.35f) {
            brightness = (0.28f - avg).coerceAtLeast(0.05f)
            gamma = 0.85f
            contrast = (contrast + 0.08f).coerceAtMost(0.3f)
            noise = maxOf(noise, 0.25f)
            strength = maxOf(strength, 0.6f)
        }
        // Too bright / washed out → pull contrast down slightly, deepen blacks
        else if (avg > 0.72f) {
            gamma = 1.08f
            contrast = (contrast + 0.12f).coerceAtMost(0.35f)
            strength = maxOf(strength, 0.55f)
        }
        // Flat / low contrast → boost contrast and texture
        if (stats.contrastScore < 0.28f) {
            contrast = (contrast + 0.15f).coerceAtMost(0.4f)
            texture = maxOf(texture, 0.15f)
            colorCorr = true
        }
        // Dull colors → saturate
        if (stats.colorfulness < 0.18f) {
            saturation = (saturation + 0.15f).coerceAtMost(0.35f)
            vibrance = (vibrance + 0.1f).coerceAtMost(0.3f)
        }

        // ---- Content-type presets ----
        when (contentType) {
            "anime" -> {
                edge = maxOf(edge, 0.3f)
                saturation = maxOf(saturation, 0.15f)
                sharpness = maxOf(sharpness, 0.1f)
            }
            "sports" -> {
                sharpness = maxOf(sharpness, 0.25f)
                saturation = maxOf(saturation, 0.2f)
                contrast = maxOf(contrast, 0.15f)
                frameOpt = true
            }
            "music_video" -> {
                vibrance = maxOf(vibrance, 0.25f)
                saturation = maxOf(saturation, 0.2f)
                colorCorr = true
            }
            "documentary" -> {
                contrast = maxOf(contrast, 0.1f)
                sharpness = maxOf(sharpness, 0.15f)
                colorTemp = 0.1f // slightly warm for natural skin tones
            }
            "low_light" -> {
                brightness = maxOf(brightness, 0.2f)
                contrast = maxOf(contrast, 0.1f)
                gamma = minOf(gamma, 0.85f)
                noise = maxOf(noise, 0.35f)
                strength = maxOf(strength, 0.7f)
            }
            "movie" -> {
                colorTemp = maxOf(colorTemp, 0.12f)
                contrast = maxOf(contrast, 0.12f)
                toneMapping = true
                colorCorr = true
            }
        }

        // ---- HDR / report integration ----
        if (isHdr) {
            toneMapping = true
            vibrance = maxOf(vibrance, 0.2f)
            contrast = maxOf(contrast, 0.12f)
        }

        return VideoEnhancementManager.VideoEnhancementConfig(
            autoEnhance = false,
            preset = "ai_$contentType",
            strength = strength.coerceIn(0.1f, 1f),
            brightness = brightness.coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            vibrance = vibrance.coerceIn(-1f, 1f),
            gamma = gamma.coerceIn(0.5f, 2f),
            colorTemperature = colorTemp.coerceIn(-1f, 1f),
            sharpness = sharpness.coerceIn(0f, 1f),
            edgeEnhancement = edge.coerceIn(0f, 1f),
            noiseReduction = noise.coerceIn(0f, 1f),
            textureEnhancement = texture.coerceIn(0f, 1f),
            hdrProcessing = isHdr,
            toneMapping = toneMapping,
            frameOptimization = frameOpt,
            colorCorrection = colorCorr
        )
    }

    // ---------------------------------------------------------------------
    // Per-video persistence
    // ---------------------------------------------------------------------

    private fun saveForVideo(
        path: String,
        config: VideoEnhancementManager.VideoEnhancementConfig,
        contentType: String
    ) {
        val key = hashKey(path)
        prefs.edit().apply {
            putString("${key}_preset", config.preset)
            putString("${key}_content", contentType)
            putFloat("${key}_strength", config.strength)
            putFloat("${key}_brightness", config.brightness)
            putFloat("${key}_contrast", config.contrast)
            putFloat("${key}_saturation", config.saturation)
            putFloat("${key}_vibrance", config.vibrance)
            putFloat("${key}_gamma", config.gamma)
            putFloat("${key}_colorTemp", config.colorTemperature)
            putFloat("${key}_sharpness", config.sharpness)
            putFloat("${key}_edge", config.edgeEnhancement)
            putFloat("${key}_noise", config.noiseReduction)
            putFloat("${key}_texture", config.textureEnhancement)
            putBoolean("${key}_hdr", config.hdrProcessing)
            putBoolean("${key}_tone", config.toneMapping)
            putBoolean("${key}_frameOpt", config.frameOptimization)
            putBoolean("${key}_colorCorr", config.colorCorrection)
            apply()
        }
    }

    private fun hashKey(path: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(path.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
}
