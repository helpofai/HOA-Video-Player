package com.helpofai.videoplayer.feature.watch_party.notification

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global singleton notification manager for Watch Party events.
 * 
 * Any component can call [push] to enqueue a notification.
 * The overlay UI observes [notifications] and renders them.
 * 
 * Thread-safe: all mutations go through MutableStateFlow which is atomic.
 */
class WatchPartyNotificationManager private constructor() {

    companion object {
        private val instance = WatchPartyNotificationManager()
        fun getInstance(): WatchPartyNotificationManager = instance
    }

    private val _notifications = MutableStateFlow<List<WatchPartyNotification>>(emptyList())
    val notifications: StateFlow<List<WatchPartyNotification>> = _notifications.asStateFlow()

    /** Maximum concurrent visible notifications */
    private val maxVisible = 5

    /**
     * Push a new notification to the queue.
     * If queue exceeds [maxVisible], oldest auto-dismiss notifications are dropped.
     */
    fun push(notification: WatchPartyNotification) {
        val current = _notifications.value.toMutableList()
        
        // Prevent duplicate notifications for the same device within 2 seconds
        val isDuplicate = current.any { existing ->
            existing.type == notification.type &&
            existing.device?.id == notification.device?.id &&
            (notification.timestamp - existing.timestamp) < 2000
        }
        if (isDuplicate) return
        
        current.add(notification)
        
        // Trim oldest auto-dismissible if over max
        while (current.size > maxVisible) {
            val oldest = current.firstOrNull { it.autoDismissMs > 0 }
            if (oldest != null) current.remove(oldest) else break
        }
        
        _notifications.value = current.toList()
    }

    /**
     * Dismiss a specific notification by ID.
     */
    fun dismiss(notificationId: String) {
        _notifications.value = _notifications.value.filterNot { it.id == notificationId }
    }

    /**
     * Clear all notifications.
     */
    fun clearAll() {
        _notifications.value = emptyList()
    }

    // ──────────────────────────────────────────────────────────────────
    // Convenience methods for common notification types
    // ──────────────────────────────────────────────────────────────────

    /** Notify that a device connected successfully. */
    fun notifyDeviceConnected(device: WatchPartyDevice, totalCount: Int) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.DEVICE_CONNECTED,
            title = "Device Connected",
            message = "${device.name} joined the Watch Party",
            device = device,
            deviceCount = totalCount,
            autoDismissMs = 4000L
        ))
        // Also push a summary if multiple devices
        if (totalCount > 1) {
            push(WatchPartyNotification(
                type = WatchPartyNotificationType.CONNECTION_SUMMARY,
                title = "Party Status",
                message = "$totalCount devices are now connected",
                deviceCount = totalCount,
                autoDismissMs = 3000L
            ))
        }
    }

    /** Notify that a device was rejected — includes Accept action button. */
    fun notifyDeviceRejected(device: WatchPartyDevice) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.DEVICE_REJECTED,
            title = "Device Rejected",
            message = "${device.name} was rejected from your party",
            device = device,
            hasAcceptAction = true,
            autoDismissMs = 10000L  // Longer display for action notifications
        ))
    }

    /** Notify that a device disconnected. */
    fun notifyDeviceDisconnected(device: WatchPartyDevice, remainingCount: Int) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.DEVICE_DISCONNECTED,
            title = "Device Disconnected",
            message = "${device.name} left the party • $remainingCount remaining",
            device = device,
            deviceCount = remainingCount,
            autoDismissMs = 4000L
        ))
    }

    /** Notify that a session was created. */
    fun notifySessionCreated(sessionName: String) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.SESSION_CREATED,
            title = "Watch Party Created",
            message = "Room \"$sessionName\" is now active",
            autoDismissMs = 3000L
        ))
    }

    /** Notify that the session ended. */
    fun notifySessionEnded() {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.SESSION_ENDED,
            title = "Watch Party Ended",
            message = "The session has been closed",
            autoDismissMs = 4000L
        ))
    }

    /** Client-side: notify that join was accepted. */
    fun notifyJoinAccepted(hostName: String) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.JOIN_ACCEPTED,
            title = "Joined Successfully",
            message = "You are now connected to $hostName's party",
            autoDismissMs = 10000L
        ))
    }

    /** Host-side: notify that a new client just connected (auto-hides after 10 s). */
    fun notifyClientConnectedToRoom(deviceName: String, roomName: String) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.DEVICE_CONNECTED,
            title = "New Viewer Joined",
            message = "$deviceName connected to your room \"$roomName\"",
            autoDismissMs = 10000L
        ))
    }

    /** Client-side: notify they are connected and stream is starting. */
    fun notifyConnectedToStream(roomName: String) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.JOIN_ACCEPTED,
            title = "Connected to Stream",
            message = "You joined \"$roomName\" — stream will start when host plays a video",
            autoDismissMs = 10000L
        ))
    }

    /** Client-side: notify that join was rejected. */
    fun notifyJoinRejected(hostName: String) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.JOIN_REJECTED,
            title = "Join Rejected",
            message = "$hostName declined your join request",
            autoDismissMs = 6000L
        ))
    }

    /** Notify that a join request expired. */
    fun notifyRequestExpired(deviceName: String) {
        push(WatchPartyNotification(
            type = WatchPartyNotificationType.REQUEST_EXPIRED,
            title = "Request Expired",
            message = "$deviceName's join request timed out",
            autoDismissMs = 4000L
        ))
    }
}
