package com.helpofai.videoplayer.feature.watch_party.authentication

class WatchPartyAuthenticationManager {
    fun authenticateSessionToken(sessionToken: String, deviceToken: String): Boolean {
        // Authenticates handshake encryption tokens
        return sessionToken.hashCode() == deviceToken.hashCode()
    }
}
