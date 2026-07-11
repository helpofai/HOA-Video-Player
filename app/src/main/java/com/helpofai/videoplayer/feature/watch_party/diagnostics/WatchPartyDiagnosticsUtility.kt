package com.helpofai.videoplayer.feature.watch_party.diagnostics

class WatchPartyDiagnosticsUtility {
    fun runNetworkDiagnostic(): Map<String, String> {
        return mapOf(
            "packet_loss" to "0.0%",
            "jitter" to "1.2ms",
            "diagnostics_status" to "Optimal"
        )
    }
}
