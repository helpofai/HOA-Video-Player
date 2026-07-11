package com.helpofai.videoplayer.feature.watch_party.notification

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice

/**
 * Notification types for the Watch Party global notification system.
 */
enum class WatchPartyNotificationType {
    /** A device successfully connected to the party */
    DEVICE_CONNECTED,
    /** A device was disconnected from the party */
    DEVICE_DISCONNECTED,
    /** A device's join request was rejected (shows Accept button) */
    DEVICE_REJECTED,
    /** A new watch party session was created */
    SESSION_CREATED,
    /** The watch party session ended */
    SESSION_ENDED,
    /** Summary: N devices are now connected */
    CONNECTION_SUMMARY,
    /** Client-side: you were accepted into a party */
    JOIN_ACCEPTED,
    /** Client-side: your join request was rejected */
    JOIN_REJECTED,
    /** Join request expired without action */
    REQUEST_EXPIRED
}

/**
 * A single notification item in the global Watch Party notification queue.
 *
 * @param id Unique ID for dedup and dismiss
 * @param type The notification category
 * @param title Short headline text
 * @param message Detailed body text
 * @param device Optional device data (for accept/reject actions)
 * @param deviceCount Total connected devices (for summary notifications)
 * @param timestamp Creation time in millis
 * @param hasAcceptAction Whether this notification has an Accept action button
 * @param autoDismissMs Auto-dismiss duration in millis (0 = manual dismiss only)
 */
data class WatchPartyNotification(
    val id: String = "notif_${System.nanoTime()}",
    val type: WatchPartyNotificationType,
    val title: String,
    val message: String,
    val device: WatchPartyDevice? = null,
    val deviceCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val hasAcceptAction: Boolean = false,
    val autoDismissMs: Long = 4000L
)
