/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
| Unauthorized copying, modification, redistribution, reverse engineering,
| decompilation, or commercial use of this source code, in whole or in part,
| is strictly prohibited without prior written permission from the copyright owner.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
| Location    : Basta Purba Para, Aranghata, Nadia, West Bengal, India
|
| This source code contains proprietary and confidential information.
| Any unauthorized access or distribution may violate applicable copyright laws.
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val DEFAULT_SUBTITLE_LANG = stringPreferencesKey("default_subtitle_lang")
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val FOLDER_VIEW_MODE = stringPreferencesKey("folder_view_mode")
        val LONG_PRESS_BOOST_SPEED = floatPreferencesKey("long_press_boost_speed")

        // Subtitle Style Preferences
        val SUBTITLE_FONT_SIZE = stringPreferencesKey("subtitle_font_size")       // "small", "medium", "large", "xlarge"
        val SUBTITLE_FONT_COLOR = intPreferencesKey("subtitle_font_color")         // ARGB int, default white
        val SUBTITLE_BG_COLOR = intPreferencesKey("subtitle_bg_color")             // ARGB int, default transparent
        val SUBTITLE_EDGE_TYPE = stringPreferencesKey("subtitle_edge_type")        // "none", "outline", "drop_shadow", "raised", "depressed"
        val SUBTITLE_EDGE_COLOR = intPreferencesKey("subtitle_edge_color")         // ARGB int, default black
        val SUBTITLE_POSITION = floatPreferencesKey("subtitle_position")           // 0.0 (bottom) to 1.0 (top), default 0.88
        val SUBTITLE_DELAY_MS = intPreferencesKey("subtitle_delay_ms")             // milliseconds, default 0
        val SUBTITLE_ENCODING = stringPreferencesKey("subtitle_encoding")          // "auto", "UTF-8", "ISO-8859-1", etc.
    }

    val defaultPlaybackSpeed: Flow<Float> = dataStore.data.map { preferences ->
        preferences[DEFAULT_PLAYBACK_SPEED] ?: 1.0f
    }

    val defaultSubtitleLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[DEFAULT_SUBTITLE_LANG] ?: "Off"
    }

    val hardwareAcceleration: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HARDWARE_ACCELERATION] ?: true
    }

    val dynamicColors: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLORS] ?: true
    }

    val backgroundPlayback: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BACKGROUND_PLAYBACK] ?: false
    }

    val folderViewMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[FOLDER_VIEW_MODE] ?: "list"
    }

    val longPressBoostSpeed: Flow<Float> = dataStore.data.map { preferences ->
        preferences[LONG_PRESS_BOOST_SPEED] ?: 2.0f
    }

    // Subtitle Style Flows
    val subtitleFontSize: Flow<String> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_FONT_SIZE] ?: "medium"
    }

    val subtitleFontColor: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_FONT_COLOR] ?: android.graphics.Color.WHITE
    }

    val subtitleBgColor: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_BG_COLOR] ?: android.graphics.Color.TRANSPARENT
    }

    val subtitleEdgeType: Flow<String> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_EDGE_TYPE] ?: "drop_shadow"
    }

    val subtitleEdgeColor: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_EDGE_COLOR] ?: android.graphics.Color.BLACK
    }

    val subtitlePosition: Flow<Float> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_POSITION] ?: 0.88f
    }

    val subtitleDelayMs: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_DELAY_MS] ?: 0
    }

    val subtitleEncoding: Flow<String> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_ENCODING] ?: "auto"
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setDefaultSubtitleLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_SUBTITLE_LANG] = language
        }
    }

    suspend fun setHardwareAcceleration(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HARDWARE_ACCELERATION] = enabled
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_PLAYBACK] = enabled
        }
    }

    suspend fun setFolderViewMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[FOLDER_VIEW_MODE] = mode
        }
    }

    suspend fun setLongPressBoostSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[LONG_PRESS_BOOST_SPEED] = speed
        }
    }

    // Subtitle Style Setters
    suspend fun setSubtitleFontSize(size: String) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_FONT_SIZE] = size
        }
    }

    suspend fun setSubtitleFontColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_FONT_COLOR] = color
        }
    }

    suspend fun setSubtitleBgColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_BG_COLOR] = color
        }
    }

    suspend fun setSubtitleEdgeType(edgeType: String) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_EDGE_TYPE] = edgeType
        }
    }

    suspend fun setSubtitleEdgeColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_EDGE_COLOR] = color
        }
    }

    suspend fun setSubtitlePosition(position: Float) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_POSITION] = position.coerceIn(0f, 1f)
        }
    }

    suspend fun setSubtitleDelayMs(delayMs: Int) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_DELAY_MS] = delayMs
        }
    }

    suspend fun setSubtitleEncoding(encoding: String) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_ENCODING] = encoding
        }
    }
}