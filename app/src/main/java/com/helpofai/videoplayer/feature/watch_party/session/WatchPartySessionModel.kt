package com.helpofai.videoplayer.feature.watch_party.session

import com.helpofai.videoplayer.core.model.Video

enum class DevicePermissionPreset(val label: String) {
    VIEWER("Viewer"),
    CONTROLLER("Controller"),
    CO_HOST("Co-Host"),
    RESTRICTED("Restricted")
}

data class WatchPartyDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val batteryLevel: Int = 100,
    val connectionSpeed: Float = 45f, // Mbps
    val latency: Int = 10, // ms
    val isHost: Boolean = false,
    val hasPlayPausePermission: Boolean = true,
    val hasSeekPermission: Boolean = false,
    val hasVolumePermission: Boolean = true,
    val hasGesturePermission: Boolean = true,
    val hasAudioTrackPermission: Boolean = false,
    val hasSubtitlePermission: Boolean = false,
    val hasReactionPermission: Boolean = true,
    val status: String = "Idle", // "Playing", "Paused", "Buffering"
    val isBanned: Boolean = false
) {
    fun getRole(): DevicePermissionPreset {
        return when {
            isHost -> DevicePermissionPreset.CO_HOST
            hasPlayPausePermission && hasSeekPermission && hasAudioTrackPermission && hasSubtitlePermission -> DevicePermissionPreset.CO_HOST
            hasPlayPausePermission && hasSeekPermission -> DevicePermissionPreset.CONTROLLER
            !hasPlayPausePermission && !hasSeekPermission && !hasGesturePermission -> DevicePermissionPreset.RESTRICTED
            else -> DevicePermissionPreset.VIEWER
        }
    }
}

data class WatchPartySession(
    val id: String,
    val name: String,
    val hostIp: String,
    val port: Int = 8080,
    val tunnelPort: Int = 9990,
    val video: Video?,
    val devices: List<WatchPartyDevice> = emptyList(),
    val currentPositionMs: Long = 0,
    val isPlaying: Boolean = false,
    val maxUsers: Int = 10,
    val securityToken: String = "",
    val usePassword: Boolean = false,
    val password: String = "",
    val allowPlayPause: Boolean = false,
    val allowSeek: Boolean = false,
    val allowVolume: Boolean = true,
    val allowNextPrev: Boolean = false,
    val allowGestures: Boolean = true,
    val allowReactions: Boolean = true,
    val allowSubtitleToggle: Boolean = false,
    val allowAudioTrack: Boolean = false,
    val allowFolderQueue: Boolean = false
)
