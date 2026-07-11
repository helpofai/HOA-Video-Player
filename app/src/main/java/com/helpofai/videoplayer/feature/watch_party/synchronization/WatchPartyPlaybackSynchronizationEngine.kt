package com.helpofai.videoplayer.feature.watch_party.synchronization

class WatchPartyPlaybackSynchronizationEngine {
    fun calculateSyncOffsetMs(hostTimeMs: Long, clientTimeMs: Long, networkLatencyMs: Int): Long {
        val estimatedClientPosition = clientTimeMs + (networkLatencyMs / 2)
        return hostTimeMs - estimatedClientPosition
    }
    
    fun isOutOfSync(offsetMs: Long, maxThresholdMs: Int = 1000): Boolean {
        return kotlin.math.abs(offsetMs) > maxThresholdMs
    }
}
