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
package com.helpofai.videoplayer.core.playback.headset

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.view.KeyEvent
import com.helpofai.videoplayer.core.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Commands emitted by the HeadsetControlManager in response to wired or
 * Bluetooth headset button events, media keys, and audio routing changes.
 */
sealed interface HeadsetCommand {
    data class TogglePlayPause(val source: String) : HeadsetCommand
    object Play : HeadsetCommand
    object Pause : HeadsetCommand
    data class FastForward(val seconds: Int = 10, val isDoubleTap: Boolean = true) : HeadsetCommand
    data class Rewind(val seconds: Int = 10, val isTripleTap: Boolean = true) : HeadsetCommand
    object NextTrack : HeadsetCommand
    object PreviousTrack : HeadsetCommand
    object AudioBecomingNoisy : HeadsetCommand
    data class HeadsetConnected(val name: String, val isBluetooth: Boolean) : HeadsetCommand
    data class HeadsetDisconnected(val name: String, val isBluetooth: Boolean) : HeadsetCommand
}

/**
 * Advanced Handset / Headphone Button Control Manager.
 *
 * Handles:
 * - Wired headsets (3.5mm & USB-C with 1, 2, or 3 buttons)
 * - Bluetooth TWS earbuds, neckbands, and over-ear headphones
 * - Car steering wheel & Bluetooth audio unit controls
 * - Multi-click detection (Single click = Play/Pause, Double click = Skip +10s / Next, Triple click = Rewind -10s / Prev)
 * - Dedicated Media Keys (Play, Pause, Next, Prev, FastForward, Rewind, Stop)
 * - Smart audio becoming noisy protection (auto-pause on disconnect/unplug)
 * - Headset connection state tracking
 */
@Singleton
class HeadsetControlManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _commands = MutableSharedFlow<HeadsetCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<HeadsetCommand> = _commands.asSharedFlow()

    // Multi-click detection engine for single-button headsets (KEYCODE_HEADSETHOOK & KEYCODE_MEDIA_PLAY_PAUSE)
    private var clickCount = 0
    private var clickDebounceJob: Job? = null
    private var lastClickTime = 0L
    private val multiClickWindowMs = 280L

    // Guard against duplicate UP/DOWN events on some Bluetooth headsets
    private var lastHandledDownTime = 0L

    private var isReceiverRegistered = false

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                when (action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        scope.launch {
                            val pauseOnDisconnect = settingsRepository.headsetPauseOnDisconnect.first()
                            if (pauseOnDisconnect) {
                                _commands.emit(HeadsetCommand.AudioBecomingNoisy)
                            }
                        }
                    }
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", -1)
                        val name = intent.getStringExtra("name") ?: "Wired Headset"
                        if (state == 1) {
                            scope.launch {
                                _commands.emit(HeadsetCommand.HeadsetConnected(name = name, isBluetooth = false))
                                if (settingsRepository.headsetResumeOnConnect.first()) {
                                    _commands.emit(HeadsetCommand.Play)
                                }
                            }
                        } else if (state == 0) {
                            scope.launch {
                                _commands.emit(HeadsetCommand.HeadsetDisconnected(name = name, isBluetooth = false))
                            }
                        }
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                        @Suppress("DEPRECATION")
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val deviceName = try { device?.name ?: "Bluetooth Audio" } catch (_: SecurityException) { "Bluetooth Audio" }

                        if (state == BluetoothHeadset.STATE_CONNECTED) {
                            scope.launch {
                                _commands.emit(HeadsetCommand.HeadsetConnected(name = deviceName, isBluetooth = true))
                                if (settingsRepository.headsetResumeOnConnect.first()) {
                                    _commands.emit(HeadsetCommand.Play)
                                }
                            }
                        } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                            scope.launch {
                                _commands.emit(HeadsetCommand.HeadsetDisconnected(name = deviceName, isBluetooth = true))
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        registerConnectionReceiver()
    }

    /**
     * Registers receivers for wired headset plugs, bluetooth connections, and becoming noisy.
     */
    fun registerConnectionReceiver() {
        if (isReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(Intent.ACTION_HEADSET_PLUG)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
            context.registerReceiver(connectionReceiver, filter)
            isReceiverRegistered = true
        } catch (e: Exception) {
            // Ignore if registration fails
        }
    }

    /**
     * Unregisters broadcast receivers to prevent leaks.
     */
    fun unregisterConnectionReceiver() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(connectionReceiver)
            isReceiverRegistered = false
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Primary entry point for intercepting media and headset button key events from
     * Activity.dispatchKeyEvent or MediaSession.Callback.onMediaButtonEvent.
     *
     * @return true if the event was handled and consumed, false otherwise.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        // Only handle relevant media and headset keys
        if (!isHandsetMediaKey(keyCode)) {
            return false
        }

        // We process the gesture primarily on ACTION_DOWN to avoid double counting on ACTION_UP.
        // Some Bluetooth devices emit rapid down+up, so we guard with event time and repeat count.
        if (action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount > 0) {
                // Long press holding: can be used for fast scrubbing or ignored
                return true
            }

            val now = System.currentTimeMillis()
            if (now - lastHandledDownTime < 60) {
                return true // Jitter filter
            }
            lastHandledDownTime = now

            when (keyCode) {
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    onMultiClickMediaButtonPressed(source = if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) "Wired/BT Hook" else "Play/Pause Button")
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    scope.launch { _commands.emit(HeadsetCommand.Play) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    scope.launch { _commands.emit(HeadsetCommand.Pause) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    scope.launch {
                        val actionPref = settingsRepository.headsetDoubleClickAction.first()
                        if (actionPref == "next_video") {
                            _commands.emit(HeadsetCommand.NextTrack)
                        } else {
                            _commands.emit(HeadsetCommand.FastForward(seconds = 10, isDoubleTap = false))
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    scope.launch {
                        val actionPref = settingsRepository.headsetTripleClickAction.first()
                        if (actionPref == "prev_video") {
                            _commands.emit(HeadsetCommand.PreviousTrack)
                        } else {
                            _commands.emit(HeadsetCommand.Rewind(seconds = 10, isTripleTap = false))
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    scope.launch { _commands.emit(HeadsetCommand.FastForward(seconds = 10, isDoubleTap = false)) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    scope.launch { _commands.emit(HeadsetCommand.Rewind(seconds = 10, isTripleTap = false)) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    scope.launch { _commands.emit(HeadsetCommand.Pause) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                    scope.launch { _commands.emit(HeadsetCommand.FastForward(seconds = 5, isDoubleTap = false)) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                    scope.launch { _commands.emit(HeadsetCommand.Rewind(seconds = 5, isTripleTap = false)) }
                    return true
                }
            }
        } else if (action == KeyEvent.ACTION_UP) {
            // Consume ACTION_UP so other components do not re-trigger default behaviour
            return true
        }

        return false
    }

    /**
     * Multi-click detection engine for single-button headsets:
     * - 1 Click  : Play / Pause toggle
     * - 2 Clicks : Fast Forward (+10s) or Next Track
     * - 3 Clicks : Rewind (-10s) or Previous Track
     */
    private fun onMultiClickMediaButtonPressed(source: String) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < multiClickWindowMs) {
            clickCount++
        } else {
            clickCount = 1
        }
        lastClickTime = now

        // Cancel previous pending click execution
        clickDebounceJob?.cancel()

        if (clickCount >= 3) {
            // Triple click reached immediately
            val currentCount = clickCount
            clickCount = 0
            scope.launch {
                val actionPref = settingsRepository.headsetTripleClickAction.first()
                if (actionPref == "prev_video") {
                    _commands.emit(HeadsetCommand.PreviousTrack)
                } else {
                    _commands.emit(HeadsetCommand.Rewind(seconds = 10, isTripleTap = true))
                }
            }
            return
        }

        // Wait for potential next clicks in multi-click window
        clickDebounceJob = scope.launch {
            delay(multiClickWindowMs)
            val finalCount = clickCount
            clickCount = 0

            when (finalCount) {
                1 -> {
                    _commands.emit(HeadsetCommand.TogglePlayPause(source = source))
                }
                2 -> {
                    val actionPref = settingsRepository.headsetDoubleClickAction.first()
                    if (actionPref == "next_video") {
                        _commands.emit(HeadsetCommand.NextTrack)
                    } else {
                        _commands.emit(HeadsetCommand.FastForward(seconds = 10, isDoubleTap = true))
                    }
                }
                3 -> {
                    val actionPref = settingsRepository.headsetTripleClickAction.first()
                    if (actionPref == "prev_video") {
                        _commands.emit(HeadsetCommand.PreviousTrack)
                    } else {
                        _commands.emit(HeadsetCommand.Rewind(seconds = 10, isTripleTap = true))
                    }
                }
            }
        }
    }

    private fun isHandsetMediaKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> true
            else -> false
        }
    }
}
