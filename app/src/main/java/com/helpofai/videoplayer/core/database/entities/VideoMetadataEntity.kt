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
package com.helpofai.videoplayer.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_metadata")
data class VideoMetadataEntity(
    @PrimaryKey val path: String, // Path is unique
    val isFavorite: Boolean = false,
    val lastPlayedPosition: Long = 0L,
    val totalDuration: Long = 0L,
    val playCount: Int = 0,
    val isHidden: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val abRepeatStart: Long? = null,
    val abRepeatEnd: Long? = null,
    val externalSubtitleUri: String? = null,
    val playbackSpeed: Float = 1.0f,
    val subtitleTrackLanguage: String? = null,
    val audioTrackLanguage: String? = null,
    val zoomLevel: Float = 1.0f
)
