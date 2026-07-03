package com.helpofai.videoplayer.feature.player.decoder

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.media3.exoplayer.DefaultRenderersFactory

object SmartDecoderEngine {
    
    fun getOptimalRenderersFactory(context: Context): DefaultRenderersFactory {
        val factory = DefaultRenderersFactory(context)
        
        // 1. Check Battery and Power Save Mode
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSaveMode = powerManager.isPowerSaveMode

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()) else 100f
        
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
