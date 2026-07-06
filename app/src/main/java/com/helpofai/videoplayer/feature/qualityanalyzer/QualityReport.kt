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
package com.helpofai.videoplayer.feature.qualityanalyzer

data class QualityReport(
    val score: Int, // 0 to 100
    val resolution: String,
    val bitrateStr: String,
    val codec: String,
    val fps: Float,
    val isHdr: Boolean,
    val audioCodec: String,
    val audioChannels: String,
    val healthStatus: String, // "Excellent", "Good", "Fair", "Poor"
    val recommendations: List<String>
)
