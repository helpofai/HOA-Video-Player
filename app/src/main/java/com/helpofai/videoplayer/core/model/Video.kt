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
package com.helpofai.videoplayer.core.model

import android.net.Uri

data class Video(
    val id: Long,
    val uri: Uri,
    val title: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val path: String,
    val isFavorite: Boolean = false,
    val lastPlayedPosition: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val category: String = "General",
    val playbackSpeed: Float = 1.0f,
    val subtitleTrackLanguage: String? = null,
    val audioTrackLanguage: String? = null,
    val width: Int = 0,
    val height: Int = 0
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
        
    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.2f MB", mb)
                else -> String.format("%.2f KB", kb)
            }
        }
}
