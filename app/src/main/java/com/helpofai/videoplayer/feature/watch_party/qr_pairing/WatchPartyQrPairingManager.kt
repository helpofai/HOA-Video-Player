package com.helpofai.videoplayer.feature.watch_party.qr_pairing

class WatchPartyQrPairingManager {
    fun generatePairingPayload(sessionId: String, hostIp: String, port: Int, token: String): String {
        val timestamp = System.currentTimeMillis()
        return "hoa_party://$sessionId?ip=$hostIp&port=$port&token=$token&t=$timestamp"
    }
    
    fun parsePairingPayload(payload: String): Map<String, String>? {
        if (!payload.startsWith("hoa_party://")) return null
        return try {
            val parts = payload.removePrefix("hoa_party://").split("?")
            val sessionId = parts[0]
            val params = parts[1].split("&").associate {
                val pair = it.split("=")
                pair[0] to pair[1]
            }
            params + mapOf("sessionId" to sessionId)
        } catch (e: Exception) {
            null
        }
    }
}
