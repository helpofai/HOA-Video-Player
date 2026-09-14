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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

object SmartDecoderEngine {
    
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getOptimalRenderersFactory(context: Context, decoderMode: String = "HW"): DefaultRenderersFactory {
        val factory = DefaultRenderersFactory(context)
        
        when (decoderMode.uppercase()) {
            "SW" -> {
                // Software Decoder mode:
                // 1. Prefer bundled software extensions (FFmpeg) over device MediaCodec
                factory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                factory.setEnableDecoderFallback(true)
                // 2. Prioritize software MediaCodec codecs (Google, Android, FFmpeg)
                factory.setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                        mimeType, requiresSecureDecoder, requiresTunnelingDecoder
                    )
                    decoders.sortedWith(compareByDescending { info ->
                        val name = info.name.lowercase()
                        name.startsWith("c2.android.") || name.startsWith("omx.google.") || name.startsWith("omx.ffmpeg.")
                    })
                }
            }
            "HW+" -> {
                // Hardware+ Decoder mode:
                // High-performance hardware acceleration with full FFmpeg software extension fallback
                factory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                factory.setEnableDecoderFallback(true)
            }
            else -> {
                // Pure HW (Hardware) mode:
                // Prioritize vendor hardware decoders (Qualcomm, MediaTek, Exynos, Mali)
                factory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                factory.setEnableDecoderFallback(true)
                factory.setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                        mimeType, requiresSecureDecoder, requiresTunnelingDecoder
                    )
                    decoders.sortedWith(compareBy { info ->
                        val name = info.name.lowercase()
                        name.startsWith("c2.android.") || name.startsWith("omx.google.") || name.startsWith("omx.ffmpeg.")
                    })
                }
            }
        }
        
        return factory
    }
}