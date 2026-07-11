package com.helpofai.videoplayer.feature.watch_party.device_manager

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

class WatchPartyDeviceMonitor {
    fun fetchDeviceHealthReport(device: WatchPartyDevice): Map<String, Any> {
        return mapOf(
            "battery" to device.batteryLevel,
            "latency" to device.latency,
            "speed" to device.connectionSpeed,
            "health" to if (device.latency > 100) "Critical" else "Excellent"
        )
    }
}
