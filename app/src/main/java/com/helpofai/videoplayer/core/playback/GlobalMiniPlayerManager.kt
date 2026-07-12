/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.playback

import com.helpofai.videoplayer.core.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GlobalMiniPlayerManager private constructor() {
    companion object {
        private val instance = GlobalMiniPlayerManager()
        fun getInstance(): GlobalMiniPlayerManager = instance
    }

    private val _activeVideo = MutableStateFlow<Video?>(null)
    val activeVideo: StateFlow<Video?> = _activeVideo.asStateFlow()

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive: StateFlow<Boolean> = _isMiniPlayerActive.asStateFlow()

    fun showMiniPlayer(video: Video) {
        _activeVideo.value = video
        _isMiniPlayerActive.value = true
    }

    fun dismissMiniPlayer() {
        _activeVideo.value = null
        _isMiniPlayerActive.value = false
    }
}
