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
                } else if (mime.startsWith("text/") || mime.contains("subtitle") || mime.contains("subrip") ||
                    mime == "application/x-subrip" ||
                    mime == "application/x-srt" ||
                    mime == "application/ttml+xml" ||
                    mime == "application/x-ssa" ||
                    mime == "application/x-ass" ||
                    mime == "application/vtt" ||
                    mime == "text/vtt" ||
                    mime == "text/x-ssa" ||
                    mime == "text/x-ass" ||
                    mime == "application/x-webvtt" ||
                    mime.contains("webvtt") ||
                    mime.contains("ttml") ||
                    mime.contains("ssa") ||
                    mime.contains("ass") ||
                    mime.contains("vtt") ||
                    mime.contains("srt") ||
                    mime.contains("pgs") ||
                    mime.contains("vobsub") ||
                    mime.contains("dvdsub") ||
                    mime.contains("hdmv")) {
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

        // Video codec support check - ENHANCED
        if (videoCodec != null) {
            val supportedVideoCodec = caps.supportedCodecs.firstOrNull { it.mimeType.equals(videoCodec, ignoreCase = true) }
            val isHwSupported = supportedVideoCodec?.isHardwareAccelerated == true
            val codecName = supportedVideoCodec?.name ?: ""
            val hasProfiles = supportedVideoCodec?.supportedProfiles?.isNotEmpty() == true
            val maxInstances = supportedVideoCodec?.maxInstances ?: 1
            
            // Determine container format from file extension/container
            val containerFormat = when {
                container.contains("mp4", ignoreCase = true) -> "MP4"
                container.contains("webm", ignoreCase = true) -> "WebM"
                container.contains("mkv", ignoreCase = true) -> "MKV"
                container.contains("avi", ignoreCase = true) -> "AVI"
                container.contains("flv", ignoreCase = true) -> "FLV"
                container.contains("mov", ignoreCase = true) -> "MOV"
                container.contains("3gp", ignoreCase = true) -> "3GP"
                container.contains("ts", ignoreCase = true) -> "MPEG-TS"
                else -> "Unknown"
            }
            
            // Frame rate analysis
            val frameRateIssues = when {
                fps > 120f -> "Frame rate ${fps}Hz exceeds typical display capability (120Hz max)"
                fps > 60f && !caps.supportsHdr -> "High frame rate (${fps}Hz) may cause issues on non-HDR displays"
                fps < 23f && fps > 0f -> "Low frame rate (${fps}Hz) may cause choppy playback"
                fps <= 0f -> "Frame rate not detected - may cause sync issues"
                else -> null
            }
            
            // Resolution analysis against device capabilities
            val maxDisplayPixels = (caps.displayWidth * caps.displayHeight).toLong()
            val videoPixels = (width.toLong() * height.toLong())
            val videoResolution = "$width×$height"
            
            val resolutionIssues = when {
                videoPixels > maxDisplayPixels * 4 -> "Resolution ($videoResolution) significantly exceeds display capabilities (${caps.displayWidth}×${caps.displayHeight})"
                videoPixels > maxDisplayPixels * 2 -> "Resolution ($videoResolution) exceeds display capabilities (${caps.displayWidth}×${caps.displayHeight})"
                videoPixels > 3840 * 2160 && caps.totalRamGb < 4.0 -> "4K+ content (${videoResolution}) requires more RAM (${caps.totalRamGb.toString().take(3)}GB available)"
                videoPixels > 1920 * 1080 && caps.totalRamGb < 2.0 -> "1080p+ content may stutter on low-RAM device (${caps.totalRamGb.toString().take(3)}GB)"
                width > 0 && height > 0 && (width % 16 != 0 || height % 16 != 0) -> "Non-standard resolution ($videoResolution) may not hardware decode efficiently"
                else -> null
            }
            
            // Bitrate analysis
            val bitrateIssues = when {
                totalBitrate > 50_000_000 && caps.totalRamGb < 4.0 -> "High bitrate (${(totalBitrate / 1_000_000).toString()} Mbps) may exceed device bandwidth"
                totalBitrate > 100_000_000 -> "Very high bitrate (${(totalBitrate / 1_000_000).toString()} Mbps) - consider lower quality source"
                else -> null
            }
            
            // Codec profile/level analysis
            val profileIssues = when {
                videoProfile != -1 && videoLevel != -1 && !hasProfiles -> "Codec profile $videoProfile@$videoLevel not in supported profiles list"
                videoProfile == -1 && videoCodec?.contains("avc", ignoreCase = true) == true -> "H.264 profile not detected - may use baseline fallback"
                videoProfile == -1 && videoCodec?.contains("hevc", ignoreCase = true) == true -> "HEVC profile not detected - check Main/Main 10 support"
                videoProfile == -1 && videoCodec?.contains("vp9", ignoreCase = true) == true -> "VP9 profile not detected - may limit quality"
                videoProfile == -1 && videoCodec?.contains("av1", ignoreCase = true) == true -> "AV1 profile not detected - requires Android 10+ for hardware"
                else -> null
            }
            
            // HDR analysis
            val hdrIssues = when {
                isHdr && !caps.supportsHdr -> "HDR content ($hdrType) not supported by display - will tone-map to SDR"
                isHdr && hdrType?.contains("Dolby Vision", ignoreCase = true) == true && caps.sdkVersion < 29 -> "Dolby Vision requires Android 10+ for proper playback"
                isHdr && videoCodec?.contains("hevc", ignoreCase = true) == true && !isHwSupported -> "HDR HEVC requires hardware decoding for optimal performance"
                else -> null
            }
            
            // Container-specific issues
            val containerIssues = when (containerFormat) {
                "MKV" -> {
                    if (subtitleTrackCount > 2 && audioTrackCount > 2) {
                        "MKV with multiple tracks ($audioTrackCount audio, $subtitleTrackCount subtitles) may need external renderers"
                    } else if (subtitleTrackCount > 0 && videoCodec?.contains("hevc", ignoreCase = true) == true) {
                        "MKV with HEVC + subtitles may require software rendering for text overlays"
                    } else null
                }
                "WebM" -> {
                    if (fps > 30f && !caps.supportsHdr) {
                        "WebM with high frame rate (${fps}Hz) may need hardware VP9/AV1 decoder"
                    } else if (videoCodec?.contains("av1", ignoreCase = true) == true && caps.sdkVersion < 29) {
                        "WebM AV1 requires Android 10+ for hardware decoding"
                    } else null
                }
                "MPEG-TS" -> {
                    if (videoCodec?.contains("hevc", ignoreCase = true) == true) {
                        "TS streams with HEVC may have PTS/DTS sync issues"
                    } else null
                }
                "AVI" -> {
                    "AVI is an older container - consider converting to MP4/MKV for better compatibility"
                }
                else -> null
            }
            
            // Hardware acceleration analysis
            val hwAccelIssues = when {
                !isHwSupported && videoCodec?.contains("hevc", ignoreCase = true) == true -> "HEVC software decoding - high CPU usage, consider hardware decoder"
                !isHwSupported && videoCodec?.contains("vp9", ignoreCase = true) == true -> "VP9 software decoding - may cause battery drain"
                !isHwSupported && videoCodec?.contains("av1", ignoreCase = true) == true -> "AV1 software decoding - extremely CPU intensive"
                !isHwSupported && maxInstances == 1 -> "Limited codec instances - may conflict with other apps"
                else -> null
            }
            
            // Build comprehensive issue list
            if (supportedVideoCodec == null) {
                isVideoSupported = false
                issues.add("Unsupported video codec: $videoCodec")
                recommendations.add("Consider using H.264, HEVC, VP9, or AV1 in MP4/MKV containers")
            } else {
                // Codec found - add detailed analysis
                if (!isHwSupported) {
                    recommendations.add("This codec requires software decoding (${codecName}) - impacts battery/performance")
                }
                if (!hasProfiles) {
                    recommendations.add("Codec profile info missing - basic decoding path used")
                }
                
                // Add all collected issues
                listOf(frameRateIssues, resolutionIssues, bitrateIssues, profileIssues, hdrIssues, containerIssues, hwAccelIssues)
                    .filterNotNull()
                    .forEach { issues.add(it) }
                
                // Add specific recommendations based on issues
                if (hwAccelIssues != null) {
                    recommendations.add("Enable hardware acceleration in settings or use H.264/HEVC with supported profiles")
                }
                if (resolutionIssues != null) {
                    recommendations.add("Consider lower resolution version (1080p max for this device)")
                }
                if (bitrateIssues != null) {
                    recommendations.add("Use network cache or local copy for high bitrate content")
                }
            }
        } else {
            isVideoSupported = false
            issues.add("No video track found.")
            recommendations.add("File may be corrupted or unsupported format")
        }

        // Audio codec support check - ENHANCED
        if (audioCodec != null) {
            val supportedAudioCodec = caps.supportedCodecs.firstOrNull { it.mimeType.equals(audioCodec, ignoreCase = true) }
            val isAudioHwSupported = supportedAudioCodec?.isHardwareAccelerated == true
            val audioCodecName = supportedAudioCodec?.name ?: ""
            val audioHasProfiles = supportedAudioCodec?.supportedProfiles?.isNotEmpty() == true
            
            if (supportedAudioCodec == null) {
                isAudioSupported = false
                issues.add("Unsupported audio codec: $audioCodec")
                recommendations.add("Audio will use software fallback - may cause sync issues or battery drain")
            } else {
                if (!isAudioHwSupported) {
                    recommendations.add("Audio codec ($audioCodecName) requires software decoding - higher CPU usage")
                }
                if (!audioHasProfiles) {
                    recommendations.add("Audio profile info missing - basic decoding used")
                }
            }
            
            // Channel analysis
            val channelIssues = when {
                audioChannels > 7 && audioChannels <= 8 -> "7.1 surround audio ($audioChannels ch) - will downmix to stereo"
                audioChannels > 2 && audioChannels <= 6 -> "Multichannel audio ($audioChannels ch) - will downmix to stereo"
                audioChannels > 8 -> "High channel count ($audioChannels ch) - may not downmix properly"
                else -> null
            }
            
            // Sample rate analysis
            val sampleRateIssues = when {
                audioSampleRate > 48000 -> "High sample rate (${audioSampleRate}Hz) - may resample on older devices"
                audioSampleRate > 0 && audioSampleRate < 8000 -> "Very low sample rate (${audioSampleRate}Hz) - poor audio quality"
                else -> null
            }
            
            // Audio format specific checks
            val audioFormatIssues = when {
                audioCodec?.contains("dts", ignoreCase = true) == true && !isAudioHwSupported -> "DTS audio requires hardware decoder for best quality"
                audioCodec?.contains("truehd", ignoreCase = true) == true -> "Dolby TrueHD - requires HDMI passthrough or software decode"
                audioCodec?.contains("eac3", ignoreCase = true) == true && !isAudioHwSupported -> "Dolby Digital Plus (E-AC3) software decode may have quality loss"
                audioCodec?.contains("ac3", ignoreCase = true) == true && !isAudioHwSupported -> "Dolby Digital (AC3) - check hardware support"
                audioCodec?.contains("opus", ignoreCase = true) == true && !isAudioHwSupported -> "Opus audio software decode - good quality but CPU intensive"
                else -> null
            }
            
            listOf(channelIssues, sampleRateIssues, audioFormatIssues)
                .filterNotNull()
                .forEach { issues.add(it) }
                
            if (audioChannels > 2) {
                recommendations.add("Multichannel audio ($audioChannels ch) will be downmixed to stereo for playback")
            }
        } else {
            issues.add("No audio track found - video will play silent")
            recommendations.add("Check if file has audio track or add external audio")
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
