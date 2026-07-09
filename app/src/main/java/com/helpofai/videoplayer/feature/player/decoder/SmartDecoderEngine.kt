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
package com.helpofai.videoplayer.feature.player.decoder

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory

object SmartDecoderEngine {
    
    @OptIn(UnstableApi::class)
    fun getOptimalRenderersFactory(context: Context): DefaultRenderersFactory {
        val factory = DefaultRenderersFactory(context)
        
        // 1. Check Battery and Power Save Mode
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSaveMode = powerManager.isPowerSaveMode
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.toFloat() ?: 100f
        
        val isLowBattery = batteryPct < 20f

        // 2. Decide Decoder Strategy
        // Software decoders (Extension) consume more battery but support more formats.
        // Hardware decoders (MediaCodec) are battery efficient.
        val extensionMode = if (isPowerSaveMode || isLowBattery) {
            // Force hardware decoders to save battery
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        } else {
            // Default: Try MediaCodec first, fallback to extension if needed
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        }

        factory.setExtensionRendererMode(extensionMode)
        factory.setEnableDecoderFallback(true) // Media3 core fallback
        
        return factory
    }
}