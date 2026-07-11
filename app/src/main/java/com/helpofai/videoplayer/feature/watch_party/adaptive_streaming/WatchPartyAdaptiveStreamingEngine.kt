package com.helpofai.videoplayer.feature.watch_party.adaptive_streaming

class WatchPartyAdaptiveStreamingEngine {
    fun determineTargetResolution(bandwidthMbps: Float, latencyMs: Int): String {
        return when {
            bandwidthMbps >= 30f && latencyMs <= 15 -> "1080P (Original)"
            bandwidthMbps >= 15f && latencyMs <= 30 -> "720P (HD)"
            bandwidthMbps >= 5f && latencyMs <= 60 -> "480P (SD)"
            else -> "360P (LQ - Buffering Safe)"
        }
    }
    
    fun calculateChunkDelayMs(latencyMs: Int): Int {
        return if (latencyMs > 50) latencyMs + 200 else 50
    }
}
