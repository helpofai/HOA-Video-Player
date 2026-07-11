package com.helpofai.videoplayer.feature.watch_party.client

import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartyDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WatchPartyClientManager(private val sessionManager: WatchPartySessionManager) {
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    fun joinSession(ip: String, port: Int, token: String) {
        _connectionStatus.value = "Connecting"
        // Setup local connection sync
        _connectionStatus.value = "Connected"
    }
    
    fun disconnect() {
        _connectionStatus.value = "Disconnected"
    }
}
