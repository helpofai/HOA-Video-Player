package com.helpofai.videoplayer.core.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("privacy_prefs", Context.MODE_PRIVATE)

    fun getPin(): String? = prefs.getString("pin_code", null)

    fun setPin(pin: String) {
        prefs.edit().putString("pin_code", pin).apply()
    }
    
    fun removePin() {
        prefs.edit().remove("pin_code").apply()
    }

    fun isLockEnabled(): Boolean = getPin() != null
}
