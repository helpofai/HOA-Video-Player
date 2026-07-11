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

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class DeviceCapabilities(
        val sdkVersion: Int,
        val cpuCores: Int,
        val totalRamGb: Double,
        val lowRamDevice: Boolean,
        val supportedCodecs: List<CodecInfo>,
        val maxDisplayRefreshRate: Float,
        val displayWidth: Int,
        val displayHeight: Int,
        val supportsHdr: Boolean
    )

    data class CodecInfo(
        val name: String,
        val mimeType: String,
        val isHardwareAccelerated: Boolean,
        val maxInstances: Int,
        val supportedProfiles: List<Int>,
        val colorFormats: List<Int>
    )

    private var cachedCapabilities: DeviceCapabilities? = null

    @Synchronized
    fun getCapabilities(): DeviceCapabilities {
        cachedCapabilities?.let { return it }

        val sdk = Build.VERSION.SDK_INT
        val cpu = Runtime.getRuntime().availableProcessors()
        
        // RAM details
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val ramGb = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
        val lowRam = memInfo.lowMemory || actManager.isLowRamDevice

        // Codecs scan
        val codecList = mutableListOf<CodecInfo>()
        try {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in list.codecInfos) {
                if (info.isEncoder) continue
                for (type in info.supportedTypes) {
                    val caps = info.getCapabilitiesForType(type)
                    val profiles = caps.profileLevels.map { it.profile }
                    val colors = caps.colorFormats.toList()
                    
                    // Hardware accelerated check (SDK 29+)
                    val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isHardwareAccelerated
                    } else {
                        val lowerName = info.name.lowercase()
                        !(lowerName.startsWith("omx.google.") || 
                          lowerName.startsWith("c2.android.") || 
                          lowerName.startsWith("omx.ffmpeg."))
                    }

                    codecList.add(
                        CodecInfo(
                            name = info.name,
                            mimeType = type,
                            isHardwareAccelerated = isHw,
                            maxInstances = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) caps.maxSupportedInstances else 1,
                            supportedProfiles = profiles,
                            colorFormats = colors
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Display details
        val winManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.display
                } catch (e: UnsupportedOperationException) {
                    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                    displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                }
            } else {
                @Suppress("DEPRECATION")
                winManager.defaultDisplay
            }
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            winManager.defaultDisplay
        }
        val refreshRate = display?.refreshRate ?: 60f
        
        var width = 0
        var height = 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = winManager.currentWindowMetrics.bounds
                width = bounds.width()
                height = bounds.height()
            } else {
                val metrics = android.util.DisplayMetrics()
                @Suppress("DEPRECATION")
                display?.getMetrics(metrics)
                width = metrics.widthPixels
                height = metrics.heightPixels
            }
        } catch (e: Exception) {
            val metrics = context.resources.displayMetrics
            width = metrics.widthPixels
            height = metrics.heightPixels
        }

        // HDR compatibility check
        val supportsHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
        } else {
            false
        }

        val caps = DeviceCapabilities(
            sdkVersion = sdk,
            cpuCores = cpu,
            totalRamGb = ramGb,
            lowRamDevice = lowRam,
            supportedCodecs = codecList,
            maxDisplayRefreshRate = refreshRate,
            displayWidth = width,
            displayHeight = height,
            supportsHdr = supportsHdr
        )
        cachedCapabilities = caps
        return caps
    }

    fun getBatteryLevel(): Float {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (scale > 0) (level.toFloat() / scale.toFloat()) * 100f else 100f
    }

    fun isThermalThrottling(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        }
        return false
    }

    fun getAvailableMemoryGb(): Double {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.availMem.toDouble() / (1024 * 1024 * 1024)
    }
}
