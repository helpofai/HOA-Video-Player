package com.helpofai.videoplayer.feature.watch_party.room.lifecycle

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the state and lifecycle of an active Watch Party room.
 */
class WatchPartyRoomLifecycleManager {

    private val _activeRoom = MutableStateFlow<WatchPartySession?>(null)
    val activeRoom = _activeRoom.asStateFlow()

    fun startRoom(session: WatchPartySession) {
        _activeRoom.value = session
        // Trigger background services, network broadcasting, etc.
    }

    fun endRoom() {
        _activeRoom.value = null
        // Stop services, cleanup networking.
    }

    fun updateRoomData(updatedSession: WatchPartySession) {
        _activeRoom.value = updatedSession
    }
}
