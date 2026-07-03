package com.helpofai.videoplayer.feature.learning

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineLearningEngine @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("offline_learning_prefs", Context.MODE_PRIVATE)

    // 1. Playback Speed
    fun learnPlaybackSpeed(speed: Float) {
        val currentAvg = prefs.getFloat("avg_speed", 1.0f)
        val count = prefs.getInt("speed_count", 0)
        val newAvg = ((currentAvg * count) + speed) / (count + 1)
        prefs.edit()
            .putFloat("avg_speed", newAvg)
            .putInt("speed_count", count + 1)
            .apply()
    }
    fun getPreferredPlaybackSpeed(): Float = prefs.getFloat("avg_speed", 1.0f)

    // 2. Brightness
    fun learnBrightness(brightness: Float) {
        val currentAvg = prefs.getFloat("avg_brightness", -1f) // -1f means system default
        if (currentAvg == -1f) {
            prefs.edit().putFloat("avg_brightness", brightness).putInt("brightness_count", 1).apply()
            return
        }
        val count = prefs.getInt("brightness_count", 0)
        val newAvg = ((currentAvg * count) + brightness) / (count + 1)
        prefs.edit()
            .putFloat("avg_brightness", newAvg)
            .putInt("brightness_count", count + 1)
            .apply()
    }
    fun getPreferredBrightness(): Float = prefs.getFloat("avg_brightness", -1f)
    
    // 3. Volume
    fun learnVolume(volume: Float) {
        val currentAvg = prefs.getFloat("avg_volume", -1f) 
        if (currentAvg == -1f) {
            prefs.edit().putFloat("avg_volume", volume).putInt("volume_count", 1).apply()
            return
        }
        val count = prefs.getInt("volume_count", 0)
        val newAvg = ((currentAvg * count) + volume) / (count + 1)
        prefs.edit()
            .putFloat("avg_volume", newAvg)
            .putInt("volume_count", count + 1)
            .apply()
    }
    fun getPreferredVolume(): Float = prefs.getFloat("avg_volume", -1f)

    // 4. Subtitle Language
    fun learnSubtitleLanguage(language: String) {
        val key = "sub_lang_$language"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
        
        val currentTop = prefs.getString("top_sub_lang", null)
        val currentTopCount = currentTop?.let { prefs.getInt("sub_lang_$it", 0) } ?: 0
        
        if (count + 1 > currentTopCount) {
            prefs.edit().putString("top_sub_lang", language).apply()
        }
    }
    fun getPreferredSubtitleLanguage(): String? = prefs.getString("top_sub_lang", null)

    // 5. Aspect Ratio
    fun learnAspectRatio(ratioType: String) { // e.g., "Fit", "Fill", "Zoom"
        val key = "aspect_ratio_$ratioType"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
        
        val currentTop = prefs.getString("top_aspect_ratio", "0")
        val currentTopCount = prefs.getInt("aspect_ratio_$currentTop", 0)
        
        if (count + 1 > currentTopCount) {
            prefs.edit().putString("top_aspect_ratio", ratioType).apply()
        }
    }
    fun getPreferredAspectRatio(): String = prefs.getString("top_aspect_ratio", "0")!!

    // 6. Decoder Preference
    fun learnDecoderPreference(decoder: String) { // "Hardware", "Software", "Auto"
        val key = "decoder_$decoder"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
        
        val currentTop = prefs.getString("top_decoder", "Auto")
        val currentTopCount = prefs.getInt("decoder_$currentTop", 0)
        
        if (count + 1 > currentTopCount) {
            prefs.edit().putString("top_decoder", decoder).apply()
        }
    }
    fun getPreferredDecoder(): String = prefs.getString("top_decoder", "Auto")!!

    // 7. Sleep Timer
    fun learnSleepTimer(minutes: Int) {
        val currentAvg = prefs.getInt("avg_sleep_timer", 0)
        val count = prefs.getInt("sleep_timer_count", 0)
        val newAvg = ((currentAvg * count) + minutes) / (count + 1)
        prefs.edit()
            .putInt("avg_sleep_timer", newAvg)
            .putInt("sleep_timer_count", count + 1)
            .apply()
    }
    fun getPreferredSleepTimer(): Int = prefs.getInt("avg_sleep_timer", 0)
}
