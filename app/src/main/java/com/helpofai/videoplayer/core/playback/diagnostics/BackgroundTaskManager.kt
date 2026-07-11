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

import android.content.Context
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.scanner.ScannerIntelligentThumbnailEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundTaskManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VideoRepository,
    private val thumbnailEngine: ScannerIntelligentThumbnailEngine,
    private val performanceManager: AdaptivePerformanceManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var backgroundJob: Job? = null

    init {
        scope.launch {
            performanceManager.canRunBackgroundTasks.collect { canRun ->
                if (canRun) {
                    startBackgroundTasks()
                } else {
                    stopBackgroundTasks()
                }
            }
        }
    }

    private fun startBackgroundTasks() {
        backgroundJob?.cancel()
        backgroundJob = scope.launch(Dispatchers.IO) {
            // Collect videos and generate thumbnails for those missing one-by-one
            repository.getVideosWithMetadata().collectLatest { videos ->
                for (video in videos) {
                    if (!isActive) break

                    val thumbFile = File(context.cacheDir, "smart_thumbnails/thumb_${video.id}.jpg")
                    if (!thumbFile.exists()) {
                        thumbnailEngine.generateBestThumbnail(video.path, video.id)
                        // Battery and memory-efficient delay between generations
                        delay(2500)
                    }
                }
            }
        }
    }

    private fun stopBackgroundTasks() {
        backgroundJob?.cancel()
        backgroundJob = null
    }
}
