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
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDetector: DeviceCapabilityDetector
) {
    data class MediaCompatibilityReport(
        val path: String,
        val container: String,
        val durationMs: Long,
        val totalBitrateBps: Int,
        val videoCodec: String?,
        val videoProfile: Int,
        val videoLevel: Int,
        val width: Int,
        val height: Int,
        val fps: Float,
        val rotation: Int,
        val audioCodec: String?,
        val audioChannels: Int,
        val audioSampleRate: Int,
        val isVideoSupported: Boolean,
        val isAudioSupported: Boolean,
        val isHdr: Boolean,
        val hdrType: String?,
        val audioTrackCount: Int,
        val subtitleTrackCount: Int,
        val languageTracks: List<String>,
        val issues: List<String>,
        val recommendations: List<String>
    )

    fun analyzeMedia(uri: Uri, path: String): MediaCompatibilityReport {
        val retriever = MediaMetadataRetriever()
        var container = "Unknown"
        var durationMs = 0L
        var totalBitrate = 0
        var rotation = 0

        try {
            retriever.setDataSource(context, uri)
            container = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "Unknown"
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            totalBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }

        var videoCodec: String? = null
        var videoProfile = -1
        var videoLevel = -1
        var width = 0
        var height = 0
        var fps = 0.0f
        var isHdr = false
        var hdrType: String? = null

        var audioCodec: String? = null
        var audioChannels = 0
        var audioSampleRate = 0
        var audioTrackCount = 0
        var subtitleTrackCount = 0
        val languageTracks = mutableListOf<String>()

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                val lang = format.getString(MediaFormat.KEY_LANGUAGE)
                if (!lang.isNullOrBlank()) {
                    languageTracks.add(lang)
                }
                
                if (mime.startsWith("video/")) {
                    videoCodec = mime
                    width = format.getInteger(MediaFormat.KEY_WIDTH, 0)
                    height = format.getInteger(MediaFormat.KEY_HEIGHT, 0)
                    fps = try {
                        format.getFloat(MediaFormat.KEY_FRAME_RATE, 0.0f)
                    } catch (e: Exception) {
                        format.getInteger(MediaFormat.KEY_FRAME_RATE, 0).toFloat()
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        videoProfile = format.getInteger(MediaFormat.KEY_PROFILE, -1)
                        videoLevel = format.getInteger(MediaFormat.KEY_LEVEL, -1)
                    }

                    // HDR checks
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val colorStandard = format.getInteger(MediaFormat.KEY_COLOR_STANDARD, -1)
                        val colorTransfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER, -1)
                        if (colorStandard == MediaFormat.COLOR_STANDARD_BT2020 || 
                            colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 || 
                            colorTransfer == MediaFormat.COLOR_TRANSFER_HLG) {
                            isHdr = true
                            hdrType = when (colorTransfer) {
                                MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR10 / Dolby Vision"
                                MediaFormat.COLOR_TRANSFER_HLG -> "HLG"
                                else -> "HDR"
                            }
                        }
                    }
                } else if (mime.startsWith("audio/")) {
                    audioTrackCount++
                    if (audioCodec == null) {
                        audioCodec = mime
                        audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 0)
                        audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 0)
                    }
                } else if (mime.startsWith("text/") || mime.contains("subtitle") || mime.contains("subrip")) {
                    subtitleTrackCount++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }

        // Evaluate compatibility against device capabilities
        val caps = capabilityDetector.getCapabilities()
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        var isVideoSupported = true
        var isAudioSupported = true

        // Video codec support check
        if (videoCodec != null) {
            val supportedVideoCodec = caps.supportedCodecs.firstOrNull { it.mimeType.equals(videoCodec, ignoreCase = true) }
            if (supportedVideoCodec == null) {
                isVideoSupported = false
                issues.add("Unsupported video codec: $videoCodec")
                recommendations.add("Consider transcoding the file or installing external decoder plugins.")
            } else {
                // Resolution check
                val maxDim = maxOf(width, height)
                if (maxDim > 3840 && caps.totalRamGb < 6.0) {
                    issues.add("High resolution ($width x $height) may cause lag on this device.")
                    recommendations.add("Disable background apps or drop display scaling to improve performance.")
                }
                
                // HDR check
                if (isHdr && !caps.supportsHdr) {
                    issues.add("HDR is not natively supported by the display.")
                    recommendations.add("The player will tone-map the colors to SDR standard.")
                }
            }
        } else {
            isVideoSupported = false
            issues.add("No video track found.")
        }

        // Audio codec support check
        if (audioCodec != null) {
            val supportedAudioCodec = caps.supportedCodecs.firstOrNull { it.mimeType.equals(audioCodec, ignoreCase = true) }
            if (supportedAudioCodec == null) {
                isAudioSupported = false
                issues.add("Unsupported audio codec: $audioCodec")
                recommendations.add("Fallback software decoding will be used for audio playback.")
            }
            if (audioChannels > 2) {
                recommendations.add("Multichannel audio ($audioChannels ch) will be downmixed to stereo.")
            }
        }

        return MediaCompatibilityReport(
            path = path,
            container = container,
            durationMs = durationMs,
            totalBitrateBps = totalBitrate,
            videoCodec = videoCodec,
            videoProfile = videoProfile,
            videoLevel = videoLevel,
            width = width,
            height = height,
            fps = fps,
            rotation = rotation,
            audioCodec = audioCodec,
            audioChannels = audioChannels,
            audioSampleRate = audioSampleRate,
            isVideoSupported = isVideoSupported,
            isAudioSupported = isAudioSupported,
            isHdr = isHdr,
            hdrType = hdrType,
            audioTrackCount = audioTrackCount,
            subtitleTrackCount = subtitleTrackCount,
            languageTracks = languageTracks,
            issues = issues,
            recommendations = recommendations
        )
    }
}
