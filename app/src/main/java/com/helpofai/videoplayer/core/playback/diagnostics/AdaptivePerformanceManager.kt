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
package com.helpofai.videoplayer.core.playback.diagnostics

import com.helpofai.videoplayer.core.playback.VideoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptivePerformanceManager @Inject constructor(
    private val videoPlayer: VideoPlayer,
    private val capabilityDetector: DeviceCapabilityDetector
) {
    private val _canRunBackgroundTasks = MutableStateFlow(true)
    val canRunBackgroundTasks = _canRunBackgroundTasks.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Start periodic active monitoring of device capabilities & player status
        scope.launch {
            // 1. Instantly listen to player updates to pause background tasks immediately when playback starts.
            launch {
                videoPlayer.playbackState.collectLatest { state ->
                    if (state.isPlaying) {
                        _canRunBackgroundTasks.value = false
                    } else {
                        checkSystemHealth()
                    }
                }
            }

            // 2. Periodically check device thermals, RAM, and battery state (e.g., every 5 seconds)
            while (isActive) {
                if (!videoPlayer.playbackState.value.isPlaying) {
                    checkSystemHealth()
                }
                delay(5000)
            }
        }
    }

    private fun checkSystemHealth() {
        val battery = capabilityDetector.getBatteryLevel()
        val isLowBattery = battery < 20f
        
        val isOverheating = capabilityDetector.isThermalThrottling()
        val availMem = capabilityDetector.getAvailableMemoryGb()
        val isLowRam = availMem < 0.5 // Less than 500 MB free

        // Background tasks are suspended if any condition is unsafe
        val isSafe = !isLowBattery && !isOverheating && !isLowRam
        _canRunBackgroundTasks.value = isSafe
    }
}
