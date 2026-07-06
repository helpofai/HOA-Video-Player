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

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("DEPRECATION")
class AudioEffectManager @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    var isEqualizerEnabled: Boolean = false
        set(value) {
            field = value
            equalizer?.enabled = value
        }

    var isBassBoostEnabled: Boolean = false
        set(value) {
            field = value
            bassBoost?.enabled = value
        }

    var isVirtualizerEnabled: Boolean = false
        set(value) {
            field = value
            virtualizer?.enabled = value
        }

    fun attachAudioSession(sessionId: Int) {
        if (sessionId == 0 || sessionId == android.media.AudioManager.ERROR) return

        release()

        try {
            equalizer = Equalizer(0, sessionId)
            bassBoost = BassBoost(0, sessionId)
            virtualizer = Virtualizer(0, sessionId)

            equalizer?.enabled = isEqualizerEnabled
            bassBoost?.enabled = isBassBoostEnabled
            virtualizer?.enabled = isVirtualizerEnabled
            
            // Set some default robust properties if supported
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    // Equalizer Controls
    fun getNumberOfBands(): Short = equalizer?.numberOfBands ?: 0
    
    fun getBandLevel(band: Short): Short = equalizer?.getBandLevel(band) ?: 0
    
    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    fun getBandFreqRange(band: Short): IntArray = equalizer?.getBandFreqRange(band) ?: intArrayOf(0, 0)
    
    fun getBandLevelRange(): ShortArray = equalizer?.bandLevelRange ?: shortArrayOf(0, 0)

    fun getPresets(): List<String> {
        val count = equalizer?.numberOfPresets ?: 0
        return (0 until count).map { equalizer?.getPresetName(it.toShort()) ?: "Preset $it" }
    }

    fun usePreset(preset: Short) {
        equalizer?.usePreset(preset)
    }

    // Bass Boost Controls
    fun setBassBoostStrength(strength: Short) {
        if (bassBoost?.strengthSupported == true) {
            bassBoost?.setStrength(strength)
        }
    }

    // Virtualizer Controls
    fun setVirtualizerStrength(strength: Short) {
        if (virtualizer?.strengthSupported == true) {
            virtualizer?.setStrength(strength)
        }
    }
}
