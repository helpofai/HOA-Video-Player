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
        preferences[DYNAMIC_COLORS] ?: true // default to true
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
}
