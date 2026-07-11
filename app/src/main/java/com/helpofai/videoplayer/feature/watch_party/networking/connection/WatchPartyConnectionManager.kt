package com.helpofai.videoplayer.feature.watch_party.networking.connection

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession

/**
 * Manages the persistent socket connection for a Watch Party session.
 */
class WatchPartyConnectionManager {

    fun connectToHost(hostIp: String, port: Int, onResult: (Boolean) -> Unit) {
        // TODO: Implement socket connection logic
    }

    fun disconnect() {
        // TODO: Close socket
    }
}
