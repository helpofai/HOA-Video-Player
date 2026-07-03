package com.helpofai.videoplayer.feature.qualityanalyzer

import com.arthenica.ffmpegkit.FFmpegKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class QualityAnalyzerEngine @Inject constructor() {

    suspend fun analyze(videoPath: String): QualityReport? = withContext(Dispatchers.IO) {
        try {
            // Using FFmpegKit to get media info via logs
            val command = "-i \"$videoPath\""
            val session = FFmpegKit.execute(command)
            val logs = session.allLogsAsString
            
            // Fallbacks
            var width = 0
            var height = 0
            var fps = 30f
            var codec = "Unknown"
            var bitrateKbps = 0L
            var audioCodec = "None"
            var audioChannels = "Unknown"
            var isHdr = false
            
            // Regex patterns
            val resRegex = "(\\d{3,5})x(\\d{3,5})".toRegex()
            val fpsRegex = "([0-9.]+)\\s+fps".toRegex()
            val videoCodecRegex = "Video:\\s+([\\w\\d]+)".toRegex()
            val bitrateRegex = "bitrate:\\s+(\\d+)\\s+kb/s".toRegex()
            val audioCodecRegex = "Audio:\\s+([\\w\\d]+)".toRegex()
            val audioChanRegex = "(stereo|5\\.1|7\\.1|mono)".toRegex()
            
            resRegex.find(logs)?.let { match ->
                width = match.groupValues[1].toInt()
                height = match.groupValues[2].toInt()
            }
            
            fpsRegex.find(logs)?.let { match ->
                fps = match.groupValues[1].toFloat()
            }
            
            videoCodecRegex.find(logs)?.let { match ->
                codec = match.groupValues[1].uppercase()
            }
            
            bitrateRegex.find(logs)?.let { match ->
                bitrateKbps = match.groupValues[1].toLong()
            }
            
            audioCodecRegex.find(logs)?.let { match ->
                audioCodec = match.groupValues[1].uppercase()
            }
            
            audioChanRegex.find(logs)?.let { match ->
                audioChannels = match.groupValues[1]
            }
            
            if (logs.contains("bt2020") || logs.contains("smpte2084") || logs.contains("arib-std-b67")) {
                isHdr = true
            }

            val resolution = "${width}x${height}"
            val bitrateMbps = bitrateKbps / 1000f
            val bitrateStr = if (bitrateMbps > 0) String.format("%.1f Mbps", bitrateMbps) else "Unknown"
            
            // Calculate Score (0-100)
            var score = 0
            
            // Resolution score (Max 40)
            val pixels = width * height
            if (pixels >= 3840 * 2160) score += 40 // 4K+
            else if (pixels >= 2560 * 1440) score += 35 // 1440p
            else if (pixels >= 1920 * 1080) score += 30 // 1080p
            else if (pixels >= 1280 * 720) score += 20 // 720p
            else score += 10 // SD
            
            // Bitrate score (Max 20)
            if (bitrateMbps > 25) score += 20
            else if (bitrateMbps > 10) score += 15
            else if (bitrateMbps > 5) score += 10
            else if (bitrateMbps > 2) score += 5
            
            // Codec Efficiency (Max 15)
            if (codec.contains("HEVC") || codec.contains("H265") || codec.contains("AV1")) score += 15
            else if (codec.contains("H264") || codec.contains("AVC")) score += 10
            else score += 5
            
            // FPS Score (Max 10)
            if (fps >= 59f) score += 10
            else if (fps >= 29f) score += 7
            else score += 4
            
            // HDR Bonus (Max 5)
            if (isHdr) score += 5
            
            // Audio Score (Max 10)
            if (audioCodec != "NONE") {
                if (audioChannels.contains("5.1") || audioChannels.contains("7.1") || audioCodec.contains("EAC3") || audioCodec.contains("AC3")) {
                    score += 10
                } else {
                    score += 5
                }
            }
            
            score = score.coerceIn(0, 100)
            
            val healthStatus = when {
                score >= 90 -> "Excellent"
                score >= 70 -> "Good"
                score >= 40 -> "Fair"
                else -> "Poor"
            }
            
            val recommendations = mutableListOf<String>()
            if (pixels < 1280 * 720) recommendations.add("Low resolution video. Consider obtaining a 1080p+ version for optimal viewing.")
            if (fps < 24f) recommendations.add("Low framerate detected. Video may appear choppy.")
            if (codec.contains("H264") && pixels >= 3840 * 2160) recommendations.add("4K video using older H.264 codec. File size is likely bloated compared to HEVC.")
            if (!isHdr && pixels >= 3840 * 2160) recommendations.add("4K video but no HDR metadata found. Colors may lack dynamic range.")
            if (audioCodec == "NONE") recommendations.add("No audio track detected.")
            if (recommendations.isEmpty()) recommendations.add("Video is highly optimized for playback.")
            
            return@withContext QualityReport(
                score = score,
                resolution = resolution,
                bitrateStr = bitrateStr,
                codec = codec,
                fps = fps,
                isHdr = isHdr,
                audioCodec = audioCodec,
                audioChannels = audioChannels,
                healthStatus = healthStatus,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
