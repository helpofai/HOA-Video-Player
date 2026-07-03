package com.helpofai.videoplayer.feature.qualityanalyzer

data class QualityReport(
    val score: Int, // 0 to 100
    val resolution: String,
    val bitrateStr: String,
    val codec: String,
    val fps: Float,
    val isHdr: Boolean,
    val audioCodec: String,
    val audioChannels: String,
    val healthStatus: String, // "Excellent", "Good", "Fair", "Poor"
    val recommendations: List<String>
)
