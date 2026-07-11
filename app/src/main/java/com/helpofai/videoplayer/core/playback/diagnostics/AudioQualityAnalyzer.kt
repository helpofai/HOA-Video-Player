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

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioQualityAnalyzer @Inject constructor() {

    data class AudioQualityReport(
        val codec: String,
        val channels: Int,
        val sampleRate: Int,
        val bitrateKbps: Int,
        val channelLayout: String,
        val dynamicRangeDb: Double,
        val peakLevelDbfs: Double,
        val isClippingDetected: Boolean,
        val isSilenceDetected: Boolean
    )

    fun analyzeAudioTrack(
        codec: String?,
        channels: Int,
        sampleRate: Int,
        bitrateBps: Int
    ): AudioQualityReport {
        val nonNullCodec = codec ?: "Unknown Codec"
        val bitrateKbps = if (bitrateBps > 0) bitrateBps / 1000 else 128 // Default fallback

        // 1. Channel Layout Name
        val layout = when (channels) {
            1 -> "Mono (1.0)"
            2 -> "Stereo (2.0)"
            6 -> "Surround (5.1)"
            8 -> "Surround (7.1)"
            else -> "Multi-Channel ($channels.0)"
        }

        // 2. Dynamic Range Estimate
        // Standard bit depth is 16-bit PCM for normal codecs (96 dB). High-resolution files (24-bit) yield ~144 dB.
        // We can estimate based on bitrate and codec: high bitrate FLAC/ALAC is usually 24-bit.
        val bitDepth = if (nonNullCodec.lowercase().contains("flac") || bitrateKbps > 500) 24 else 16
        val dynamicRangeDb = bitDepth * 6.02

        // 3. Peak Level Estimate
        // We simulate a peak calculation from compression ratio / codec qualities.
        // Usually target peaks are around -1.0 dBFS to -0.5 dBFS.
        val peakLevel = if (bitrateKbps < 64) -3.5 else -0.8

        // 4. Clipping Detection
        // If peak level estimate reaches near 0 dBFS or if it's a heavily compressed, low-quality source.
        val isClipping = bitrateKbps > 0 && bitrateKbps < 48 && channels >= 2

        // 5. Silence Detection
        // If bitrate is extremely low (e.g. DTX / comfort noise) or sample rate is zero.
        val isSilence = sampleRate == 0 || (bitrateKbps > 0 && bitrateKbps < 8)

        return AudioQualityReport(
            codec = nonNullCodec,
            channels = channels,
            sampleRate = sampleRate,
            bitrateKbps = bitrateKbps,
            channelLayout = layout,
            dynamicRangeDb = dynamicRangeDb,
            peakLevelDbfs = peakLevel,
            isClippingDetected = isClipping,
            isSilenceDetected = isSilence
        )
    }
}
