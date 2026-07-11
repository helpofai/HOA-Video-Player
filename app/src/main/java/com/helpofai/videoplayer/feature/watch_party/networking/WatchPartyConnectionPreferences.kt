package com.helpofai.videoplayer.feature.watch_party.networking

import android.content.Context

class WatchPartyConnectionPreferences private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "watch_party_connection_prefs"
        private const val KEY_USER_DISCONNECTED   = "user_manually_disconnected"
        private const val KEY_AUTO_WIFI            = "auto_wifi_enabled"
        private const val KEY_BACKGROUND_KEEP_ALIVE = "background_keep_alive"
        private const val KEY_LAST_SESSION_ID      = "last_session_id"

        @Volatile private var instance: WatchPartyConnectionPreferences? = null
        fun getInstance(context: Context): WatchPartyConnectionPreferences {
            return instance ?: synchronized(this) {
                instance ?: WatchPartyConnectionPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var userManuallydisconnected: Boolean
        get() = prefs.getBoolean(KEY_USER_DISCONNECTED, false)
        set(value) { prefs.edit().putBoolean(KEY_USER_DISCONNECTED, value).apply() }

    var autoWifiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_WIFI, true)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_WIFI, value).apply() }

    var backgroundKeepAlive: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_KEEP_ALIVE, true)
        set(value) { prefs.edit().putBoolean(KEY_BACKGROUND_KEEP_ALIVE, value).apply() }

    var lastSessionId: String?
        get() = prefs.getString(KEY_LAST_SESSION_ID, null)
        set(value) { prefs.edit().putString(KEY_LAST_SESSION_ID, value).apply() }

    fun markUserConnected() { userManuallydisconnected = false }
    fun markUserDisconnected() { userManuallydisconnected = true; lastSessionId = null }
}
