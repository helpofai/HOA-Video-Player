package com.helpofai.videoplayer.feature.watch_party.permissions

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

class WatchPartyPermissionManager {
    fun verifyPermission(device: WatchPartyDevice, action: String): Boolean {
        if (device.isHost) return true
        return when (action) {
            "PLAY_PAUSE" -> device.hasPlayPausePermission
            "SEEK" -> device.hasSeekPermission
            "VOLUME" -> device.hasVolumePermission
            else -> false
        }
    }
}
