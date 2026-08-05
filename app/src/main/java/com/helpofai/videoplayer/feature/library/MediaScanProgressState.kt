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
package com.helpofai.videoplayer.feature.library

/**
 * Real-time media scan progress tracking data.
 * Emitted during media library refresh to show detailed scan information.
 */
data class MediaScanProgressState(
    val isScanning: Boolean = false,
    val videosFound: Int = 0,
    val videosScanned: Int = 0,
    val currentFile: String = "",
    val currentDirectory: String = "",
    val progressPercentage: Float = 0f,
    val elapsedTimeMs: Long = 0L,
    val estimatedTotalTimeMs: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val currentSizeBytes: Long = 0L,
    val statusMessage: String = "Initializing...",
    val errorMessage: String? = null
) {
    val estimatedRemainingMs: Long
        get() = if (progressPercentage > 0f) {
            ((estimatedTotalTimeMs - elapsedTimeMs) * (100f - progressPercentage) / progressPercentage).toLong()
        } else {
            0L
        }
    
    val displayProgress: Float
        get() = progressPercentage.coerceIn(0f, 100f)
}
