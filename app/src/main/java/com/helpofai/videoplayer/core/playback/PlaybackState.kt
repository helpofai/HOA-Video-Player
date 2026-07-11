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
package com.helpofai.videoplayer.core.playback

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackState: Int = 1, // Player.STATE_IDLE
    val currentDecoderName: String = "Unknown",
    val isHardwareDecoder: Boolean = true,
    val videoCodec: String = "Unknown",
    val audioCodec: String = "Unknown",
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoFps: Float = 0f,
    val videoBitrate: Int = 0,
    val isHdr: Boolean = false,
    val droppedFrames: Int = 0,
    val fallbackEvents: List<String> = emptyList(),
    val playbackStability: String = "Stable",
    val compatibilityStatus: String = "Compatible"
)
