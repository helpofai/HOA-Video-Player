package com.helpofai.videoplayer.feature.watch_party.host

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

class WatchPartyHostManager(private val sessionManager: WatchPartySessionManager) {
    fun approveDevice(device: WatchPartyDevice) {
        sessionManager.addDevice(device.copy(status = "Connected"))
    }
    
    fun rejectDevice(deviceId: String) {
        sessionManager.removeDevice(deviceId)
    }
    
    fun kickDevice(deviceId: String) {
        sessionManager.removeDevice(deviceId)
    }
    
    fun banDevice(deviceId: String) {
        sessionManager.banDevice(deviceId)
    }
    
    fun unbanDevice(deviceId: String) {
        sessionManager.unbanDevice(deviceId)
    }
    
    fun setDevicePlaybackPermission(deviceId: String, playPause: Boolean, seek: Boolean) {
        val currentSession = sessionManager.activeSession.value ?: return
        val updated = currentSession.devices.map {
            if (it.id == deviceId) {
                it.copy(hasPlayPausePermission = playPause, hasSeekPermission = seek)
            } else it
        }
        // Save back via manager update mechanism (or mock implementation update)
    }
}
