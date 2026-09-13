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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("DEPRECATION")
class AudioEffectManager @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _isEqualizerEnabled = mutableStateOf(false)
    var isEqualizerEnabled: Boolean
        get() = _isEqualizerEnabled.value
        set(value) {
            _isEqualizerEnabled.value = value
            equalizer?.enabled = value
        }

    private val _isBassBoostEnabled = mutableStateOf(false)
    var isBassBoostEnabled: Boolean
        get() = _isBassBoostEnabled.value
        set(value) {
            _isBassBoostEnabled.value = value
            bassBoost?.enabled = value
            if (value && _bassStrength.value > 0) {
                setBassBoostStrength(_bassStrength.value.toShort())
            }
        }

    private val _bassStrength = mutableStateOf(0)
    var bassStrength: Int
        get() = _bassStrength.value
        set(value) {
            _bassStrength.value = value.coerceIn(0, 1000)
            if (value > 0 && !_isBassBoostEnabled.value) {
                _isBassBoostEnabled.value = true
                bassBoost?.enabled = true
            }
            setBassBoostStrength(value.toShort())
        }

    private val _isVirtualizerEnabled = mutableStateOf(false)
    var isVirtualizerEnabled: Boolean
        get() = _isVirtualizerEnabled.value
        set(value) {
            _isVirtualizerEnabled.value = value
            virtualizer?.enabled = value
            if (value && _virtualizerStrength.value > 0) {
                setVirtualizerStrength(_virtualizerStrength.value.toShort())
            }
        }

    private val _virtualizerStrength = mutableStateOf(0)
    var virtualizerStrength: Int
        get() = _virtualizerStrength.value
        set(value) {
            _virtualizerStrength.value = value.coerceIn(0, 1000)
            if (value > 0 && !_isVirtualizerEnabled.value) {
                _isVirtualizerEnabled.value = true
                virtualizer?.enabled = true
            }
            setVirtualizerStrength(value.toShort())
        }

    // Volume Boost (0% to 100% additional gain up to +15dB via LoudnessEnhancer)
    private val _volumeBoostPercent = mutableStateOf(0)
    var volumeBoostPercent: Int
        get() = _volumeBoostPercent.value
        set(value) {
            setVolumeBoost(value)
        }

    // Dialogue / Vocal Clarity Enhancement
    private val _isDialogueClarityEnabled = mutableStateOf(false)
    var isDialogueClarityEnabled: Boolean
        get() = _isDialogueClarityEnabled.value
        set(value) {
            _isDialogueClarityEnabled.value = value
            applyDialogueClarity(value)
        }

    fun attachAudioSession(sessionId: Int) {
        if (sessionId == 0 || sessionId == android.media.AudioManager.ERROR) return

        release()

        try {
            equalizer = Equalizer(1, sessionId)
            bassBoost = BassBoost(1, sessionId)
            virtualizer = Virtualizer(1, sessionId)
            loudnessEnhancer = LoudnessEnhancer(sessionId)

            equalizer?.enabled = isEqualizerEnabled
            bassBoost?.enabled = isBassBoostEnabled
            virtualizer?.enabled = isVirtualizerEnabled

            if (_volumeBoostPercent.value > 0) {
                loudnessEnhancer?.apply {
                    enabled = true
                    setTargetGain(_volumeBoostPercent.value * 15) // up to 1500 mB
                }
            }

            if (bassBoost?.strengthSupported == true && _bassStrength.value > 0) {
                bassBoost?.setStrength(_bassStrength.value.toShort())
            }
            if (virtualizer?.strengthSupported == true && _virtualizerStrength.value > 0) {
                virtualizer?.setStrength(_virtualizerStrength.value.toShort())
            }

            if (_isDialogueClarityEnabled.value) {
                applyDialogueClarity(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }

    // Volume Boost Control
    fun setVolumeBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _volumeBoostPercent.value = clamped
        try {
            loudnessEnhancer?.apply {
                if (clamped > 0) {
                    enabled = true
                    setTargetGain(clamped * 15) // 100% -> 1500 mB (+15dB)
                } else {
                    enabled = false
                    setTargetGain(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Dialogue / Speech Clarity
    fun applyDialogueClarity(enabled: Boolean) {
        _isDialogueClarityEnabled.value = enabled
        if (equalizer == null) return
        try {
            val numBands = equalizer?.numberOfBands ?: 0
            if (numBands > 0) {
                equalizer?.enabled = true
                _isEqualizerEnabled.value = true
                val levelRange = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
                val boost = (levelRange[1] * 0.40f).toInt().toShort()

                for (i in 0 until numBands) {
                    val band = i.toShort()
                    val freqRange = equalizer?.getBandFreqRange(band) ?: intArrayOf(0, 0)
                    val centerFreqHz = if (freqRange.isNotEmpty()) freqRange[0] / 1000 else 0

                    if (centerFreqHz in 1000..4000) {
                        // Boost speech/dialogue presence
                        equalizer?.setBandLevel(band, if (enabled) boost else 0)
                    } else if (centerFreqHz < 200 && enabled) {
                        // Gently attenuate deep rumble so speech is clearer
                        equalizer?.setBandLevel(band, (-boost / 3).toShort())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            try {
                bassBoost?.setStrength(strength)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Virtualizer Controls
    fun setVirtualizerStrength(strength: Short) {
        if (virtualizer?.strengthSupported == true) {
            try {
                virtualizer?.setStrength(strength)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}