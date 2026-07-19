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
package com.helpofai.videoplayer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpofai.videoplayer.core.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val videoRepository: com.helpofai.videoplayer.core.data.VideoRepository
) : ViewModel() {

    val defaultPlaybackSpeed = settingsRepository.defaultPlaybackSpeed.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        1.0f
    )

    val defaultSubtitleLanguage = settingsRepository.defaultSubtitleLanguage.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "Off"
    )

    val hardwareAcceleration = settingsRepository.hardwareAcceleration.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val dynamicColors = settingsRepository.dynamicColors.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val backgroundPlayback = settingsRepository.backgroundPlayback.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    // Subtitle Style StateFlows
    val subtitleFontSize = settingsRepository.subtitleFontSize.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "medium"
    )

    val subtitleFontColor = settingsRepository.subtitleFontColor.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        android.graphics.Color.WHITE
    )

    val subtitleBgColor = settingsRepository.subtitleBgColor.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        android.graphics.Color.TRANSPARENT
    )

    val subtitleEdgeType = settingsRepository.subtitleEdgeType.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "drop_shadow"
    )

    val subtitleEdgeColor = settingsRepository.subtitleEdgeColor.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        android.graphics.Color.BLACK
    )

    val subtitlePosition = settingsRepository.subtitlePosition.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0.88f
    )

    val subtitleDelayMs = settingsRepository.subtitleDelayMs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val subtitleEncoding = settingsRepository.subtitleEncoding.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "auto"
    )

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepository.setDefaultPlaybackSpeed(speed)
        }
    }

    fun setSubtitleLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultSubtitleLanguage(language)
        }
    }

    fun setHardwareAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHardwareAcceleration(enabled)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColors(enabled)
        }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBackgroundPlayback(enabled)
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            videoRepository.clearAllWatchHistory()
        }
    }

    // Subtitle Style Setters
    fun setSubtitleFontSize(size: String) {
        viewModelScope.launch {
            settingsRepository.setSubtitleFontSize(size)
        }
    }

    fun setSubtitleFontColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.setSubtitleFontColor(color)
        }
    }

    fun setSubtitleBgColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.setSubtitleBgColor(color)
        }
    }

    fun setSubtitleEdgeType(edgeType: String) {
        viewModelScope.launch {
            settingsRepository.setSubtitleEdgeType(edgeType)
        }
    }

    fun setSubtitleEdgeColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.setSubtitleEdgeColor(color)
        }
    }

    fun setSubtitlePosition(position: Float) {
        viewModelScope.launch {
            settingsRepository.setSubtitlePosition(position)
        }
    }

    fun setSubtitleDelayMs(delayMs: Int) {
        viewModelScope.launch {
            settingsRepository.setSubtitleDelayMs(delayMs)
        }
    }

    fun setSubtitleEncoding(encoding: String) {
        viewModelScope.launch {
            settingsRepository.setSubtitleEncoding(encoding)
        }
    }
}