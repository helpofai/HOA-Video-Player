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
package com.helpofai.videoplayer.core.util

data class VideoItem(
    val title: String,
    val duration: String,
    val size: String,
    val progress: Float = 0f
)

val dummyVideos = listOf(
    VideoItem("Nature Documentary 4K", "45:20", "1.2 GB", 0.6f),
    VideoItem("Cyberpunk Cinematic Movie", "02:15:30", "4.5 GB", 0.1f),
    VideoItem("Family Vacation 2026", "12:05", "300 MB", 0f),
    VideoItem("Android Compose Tutorial", "25:10", "800 MB", 1f),
    VideoItem("Gaming Montage", "08:45", "150 MB", 0f)
)
