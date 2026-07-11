package com.helpofai.videoplayer.feature.watch_party.room.create

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession
import java.util.UUID

/**
 * Responsible for the business logic of creating a new Watch Party room.
 */
class WatchPartyRoomCreator {

    fun generateRoomId(): String {
        return "ROOM-" + UUID.randomUUID().toString().take(6).uppercase()
    }

    fun createRoom(
        roomName: String,
        password: String?,
        maxGuests: Int,
        permissions: Map<String, Boolean>
    ): WatchPartySession {
        // Business logic to construct and initialize the room/session
        return WatchPartySession(
            id = generateRoomId(),
            name = roomName,
            hostIp = "192.168.1.1", // Placeholder, will be determined by network manager
            video = null, // Video set via PlayerScreen toggle
            securityToken = password ?: "",
            maxUsers = maxGuests
            // TODO: Map permissions
        )
    }
}
