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
import android.content.SharedPreferences
import com.helpofai.videoplayer.core.playback.diagnostics.MediaAnalyzer.MediaCompatibilityReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class VideoEnhancementManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDetector: DeviceCapabilityDetector,
    private val performanceManager: AdaptivePerformanceManager
) {
    data class VideoEnhancementConfig(
        val autoEnhance: Boolean = true,
        val strength: Float = 0.45f,
        val preset: String = "auto",
        
        // Image
        val brightness: Float = 0f,         // -1f to 1f
        val contrast: Float = 0f,           // -1f to 1f
        val saturation: Float = 0f,         // -1f to 1f
        val vibrance: Float = 0f,           // -1f to 1f
        val gamma: Float = 1.0f,            // 0.5f to 2.0f
        val colorTemperature: Float = 0f,   // -1f to 1f (Cool to Warm)
        
        // Detail
        val sharpness: Float = 0f,          // 0f to 1f
        val edgeEnhancement: Float = 0f,    // 0f to 1f
        val noiseReduction: Float = 0f,     // 0f to 1f
        val textureEnhancement: Float = 0f, // 0f to 1f
        
        // Playback/HDR
        val hdrProcessing: Boolean = false,
        val toneMapping: Boolean = false,
        val frameOptimization: Boolean = false,
        val colorCorrection: Boolean = false
    )

    private val prefs: SharedPreferences = context.getSharedPreferences("video_enhancements", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config = _config.asStateFlow()

    private val _isOptimizedForPerformance = MutableStateFlow(false)
    val isOptimizedForPerformance = _isOptimizedForPerformance.asStateFlow()

    fun updateConfig(newConfig: VideoEnhancementConfig) {
        _config.value = newConfig
        saveConfig(newConfig)
    }

    fun applyPreset(presetId: String, report: MediaCompatibilityReport?) {
        val baseConfig = when (presetId) {
            "original" -> VideoEnhancementConfig(autoEnhance = false, preset = "original", strength = 0f)
            "cinema" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "cinema", strength = 0.5f,
                brightness = -0.05f, contrast = 0.15f, colorTemperature = 0.15f,
                toneMapping = true, colorCorrection = true
            )
            "natural" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "natural", strength = 0.3f,
                brightness = 0f, contrast = 0.05f, vibrance = 0.1f
            )
            "vivid" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "vivid", strength = 0.7f,
                contrast = 0.2f, saturation = 0.3f, vibrance = 0.3f,
                sharpness = 0.2f, edgeEnhancement = 0.2f, colorCorrection = true
            )
            "amoled" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "amoled", strength = 0.8f,
                contrast = 0.35f, saturation = 0.25f, gamma = 1.1f,
                toneMapping = true
            )
            "hdr" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "hdr", strength = 0.65f,
                brightness = 0.05f, contrast = 0.2f, vibrance = 0.25f,
                hdrProcessing = true, toneMapping = true
            )
            "anime" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "anime", strength = 0.6f,
                contrast = 0.1f, saturation = 0.2f, edgeEnhancement = 0.3f,
                sharpness = 0.1f
            )
            "sports" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "sports", strength = 0.7f,
                brightness = 0.05f, contrast = 0.15f, saturation = 0.25f,
                sharpness = 0.25f, frameOptimization = true
            )
            "low_light" -> VideoEnhancementConfig(
                autoEnhance = false, preset = "low_light", strength = 0.75f,
                brightness = 0.25f, contrast = 0.1f, gamma = 0.8f,
                noiseReduction = 0.35f
            )
            "custom" -> loadCustomPreset()
            else -> {
                // "auto" preset: dynamically compute base config
                computeAutoConfig(report)
            }
        }
        
        // Scale and adapt according to device thermal, RAM, and battery state
        val adaptedConfig = adaptToDeviceState(baseConfig)
        updateConfig(adaptedConfig)
    }

    fun saveCustomPreset(customConfig: VideoEnhancementConfig) {
        prefs.edit().apply {
            putFloat("c_brightness", customConfig.brightness)
            putFloat("c_contrast", customConfig.contrast)
            putFloat("c_saturation", customConfig.saturation)
            putFloat("c_vibrance", customConfig.vibrance)
            putFloat("c_gamma", customConfig.gamma)
            putFloat("c_colorTemperature", customConfig.colorTemperature)
            putFloat("c_sharpness", customConfig.sharpness)
            putFloat("c_edgeEnhancement", customConfig.edgeEnhancement)
            putFloat("c_noiseReduction", customConfig.noiseReduction)
            putFloat("c_textureEnhancement", customConfig.textureEnhancement)
            putBoolean("c_hdrProcessing", customConfig.hdrProcessing)
            putBoolean("c_toneMapping", customConfig.toneMapping)
            putBoolean("c_frameOptimization", customConfig.frameOptimization)
            putBoolean("c_colorCorrection", customConfig.colorCorrection)
            apply()
        }
    }

    private fun loadCustomPreset(): VideoEnhancementConfig {
        return VideoEnhancementConfig(
            autoEnhance = false,
            preset = "custom",
            brightness = prefs.getFloat("c_brightness", 0f),
            contrast = prefs.getFloat("c_contrast", 0f),
            saturation = prefs.getFloat("c_saturation", 0f),
            vibrance = prefs.getFloat("c_vibrance", 0f),
            gamma = prefs.getFloat("c_gamma", 1.0f),
            colorTemperature = prefs.getFloat("c_colorTemperature", 0f),
            sharpness = prefs.getFloat("c_sharpness", 0f),
            edgeEnhancement = prefs.getFloat("c_edgeEnhancement", 0f),
            noiseReduction = prefs.getFloat("c_noiseReduction", 0f),
            textureEnhancement = prefs.getFloat("c_textureEnhancement", 0f),
            hdrProcessing = prefs.getBoolean("c_hdrProcessing", false),
            toneMapping = prefs.getBoolean("c_toneMapping", false),
            frameOptimization = prefs.getBoolean("c_frameOptimization", false),
            colorCorrection = prefs.getBoolean("c_colorCorrection", false)
        )
    }

    private fun computeAutoConfig(report: MediaCompatibilityReport?): VideoEnhancementConfig {
        if (report == null) return VideoEnhancementConfig(preset = "auto")

        var brightness = 0f
        var contrast = 0f
        var saturation = 0f
        var vibrance = 0f
        var gamma = 1.0f
        var sharpness = 0f
        var noiseReduction = 0f
        var edgeEnhancement = 0f
        var textureEnhancement = 0f
        var hdrProcessing = false
        var toneMapping = false

        // 1. Resolution / Quality checks
        val maxDim = maxOf(report.width, report.height)
        if (maxDim <= 854) { // 480p or lower
            noiseReduction = 0.3f
            sharpness = 0.4f
            edgeEnhancement = 0.25f
            textureEnhancement = 0.2f
        } else if (maxDim <= 1280) { // 720p
            sharpness = 0.2f
            contrast = 0.05f
            vibrance = 0.1f
        } else if (maxDim <= 1920) { // 1080p
            contrast = 0.08f
            vibrance = 0.12f
        }

        // 2. HDR checks
        if (report.isHdr) {
            hdrProcessing = true
            toneMapping = true
            contrast = 0.15f
            vibrance = 0.2f
        }

        // 3. Anime detection from file path/name
        val pathLower = report.path.lowercase()
        if (pathLower.contains("anime") || pathLower.contains("cartoon") || pathLower.contains("animation")) {
            edgeEnhancement = 0.3f
            saturation = 0.15f
            sharpness = 0.1f
        }

        return VideoEnhancementConfig(
            autoEnhance = true,
            preset = "auto",
            strength = 0.45f,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            vibrance = vibrance,
            gamma = gamma,
            sharpness = sharpness,
            edgeEnhancement = edgeEnhancement,
            noiseReduction = noiseReduction,
            textureEnhancement = textureEnhancement,
            hdrProcessing = hdrProcessing,
            toneMapping = toneMapping
        )
    }

    fun adaptToDeviceState(config: VideoEnhancementConfig): VideoEnhancementConfig {
        val caps = capabilityDetector.getCapabilities()
        val isThermalThrottling = capabilityDetector.isThermalThrottling()
        val isBatterySaver = capabilityDetector.getBatteryLevel() < 15f
        val isLowRam = caps.lowRamDevice || capabilityDetector.getAvailableMemoryGb() < 1.0

        if (isThermalThrottling || isBatterySaver || isLowRam) {
            _isOptimizedForPerformance.value = true
            // Playback Safety Rules: Disable heavy stages, reduce strength
            return config.copy(
                strength = config.strength * 0.5f,
                sharpness = config.sharpness * 0.3f,
                edgeEnhancement = 0f,
                noiseReduction = 0f,
                textureEnhancement = 0f,
                frameOptimization = false,
                hdrProcessing = config.hdrProcessing && !isLowRam // Keep HDR if absolutely necessary but drop if low memory
            )
        } else {
            _isOptimizedForPerformance.value = false
            return config
        }
    }

    private fun saveConfig(config: VideoEnhancementConfig) {
        prefs.edit().apply {
            putBoolean("autoEnhance", config.autoEnhance)
            putFloat("strength", config.strength)
            putString("preset", config.preset)
            putFloat("brightness", config.brightness)
            putFloat("contrast", config.contrast)
            putFloat("saturation", config.saturation)
            putFloat("vibrance", config.vibrance)
            putFloat("gamma", config.gamma)
            putFloat("colorTemperature", config.colorTemperature)
            putFloat("sharpness", config.sharpness)
            putFloat("edgeEnhancement", config.edgeEnhancement)
            putFloat("noiseReduction", config.noiseReduction)
            putFloat("textureEnhancement", config.textureEnhancement)
            putBoolean("hdrProcessing", config.hdrProcessing)
            putBoolean("toneMapping", config.toneMapping)
            putBoolean("frameOptimization", config.frameOptimization)
            putBoolean("colorCorrection", config.colorCorrection)
            apply()
        }
    }

    private fun loadConfig(): VideoEnhancementConfig {
        return VideoEnhancementConfig(
            autoEnhance = prefs.getBoolean("autoEnhance", true),
            strength = prefs.getFloat("strength", 0.45f),
            preset = prefs.getString("preset", "auto") ?: "auto",
            brightness = prefs.getFloat("brightness", 0f),
            contrast = prefs.getFloat("contrast", 0f),
            saturation = prefs.getFloat("saturation", 0f),
            vibrance = prefs.getFloat("vibrance", 0f),
            gamma = prefs.getFloat("gamma", 1.0f),
            colorTemperature = prefs.getFloat("colorTemperature", 0f),
            sharpness = prefs.getFloat("sharpness", 0f),
            edgeEnhancement = prefs.getFloat("edgeEnhancement", 0f),
            noiseReduction = prefs.getFloat("noiseReduction", 0f),
            textureEnhancement = prefs.getFloat("textureEnhancement", 0f),
            hdrProcessing = prefs.getBoolean("hdrProcessing", false),
            toneMapping = prefs.getBoolean("toneMapping", false),
            frameOptimization = prefs.getBoolean("frameOptimization", false),
            colorCorrection = prefs.getBoolean("colorCorrection", false)
        )
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getMedia3Effects(config: VideoEnhancementConfig): List<androidx.media3.common.Effect> {
        if (config.preset == "original" || (!config.autoEnhance && config.strength == 0f)) {
            return emptyList()
        }
        val list = mutableListOf<androidx.media3.common.Effect>()
        val s = config.strength

        if (config.contrast != 0f) {
            list.add(androidx.media3.effect.Contrast(config.contrast * s))
        }

        val totalSat = (config.saturation + config.vibrance * 0.5f) * s
        if (totalSat != 0f) {
            list.add(
                androidx.media3.effect.HslAdjustment.Builder()
                    .adjustSaturation(totalSat * 100f)
                    .build()
            )
        }

        if (config.colorTemperature != 0f) {
            val temp = config.colorTemperature * s
            val r = if (temp > 0f) 1f + temp * 0.2f else 1f + temp * 0.1f
            val b = if (temp < 0f) 1f - temp * 0.2f else 1f - temp * 0.1f
            list.add(
                androidx.media3.effect.RgbAdjustment.Builder()
                    .setRedScale(r)
                    .setBlueScale(b)
                    .build()
            )
        }

        return list
    }
}
