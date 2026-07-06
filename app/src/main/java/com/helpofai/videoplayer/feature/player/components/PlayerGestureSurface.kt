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
package com.helpofai.videoplayer.feature.player.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.helpofai.videoplayer.feature.player.PlayerViewModel

@Composable
fun PlayerGestureSurface(
    viewModel: PlayerViewModel,
    activity: Activity?,
    context: Context,
    isPlaying: Boolean,
    isToolsExpanded: Boolean,
    isControllerVisible: Boolean,
    longPressBoostSpeed: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    seekAccumulation: Int,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit,
    onSeekAccumulationChange: (Int) -> Unit,
    onControllerVisibilityChange: (Boolean) -> Unit,
    onToolsExpandedChange: (Boolean) -> Unit,
    onPlayPauseToggle: (Boolean) -> Unit,
    onFeedbackEvent: (FeedbackEvent) -> Unit,
    onLongPressStateChange: (Boolean, Float, Float, Float, Float, Int, Float) -> Unit // visible, cx, cy, fx, fy, idx, savedSpeed
) {
    val isPlaying by rememberUpdatedState(isPlaying)
    val isToolsExpanded by rememberUpdatedState(isToolsExpanded)
    val isControllerVisible by rememberUpdatedState(isControllerVisible)
    val scale by rememberUpdatedState(scale)
    val offsetX by rememberUpdatedState(offsetX)
    val offsetY by rememberUpdatedState(offsetY)
    val seekAccumulation by rememberUpdatedState(seekAccumulation)

    val onScaleChange by rememberUpdatedState(onScaleChange)
    val onOffsetChange by rememberUpdatedState(onOffsetChange)
    val onSeekAccumulationChange by rememberUpdatedState(onSeekAccumulationChange)
    val onControllerVisibilityChange by rememberUpdatedState(onControllerVisibilityChange)
    val onToolsExpandedChange by rememberUpdatedState(onToolsExpandedChange)
    val onPlayPauseToggle by rememberUpdatedState(onPlayPauseToggle)
    val onFeedbackEvent by rememberUpdatedState(onFeedbackEvent)
    val onLongPressStateChange by rememberUpdatedState(onLongPressStateChange)

    var activePointers by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        activePointers = event.changes.count { it.pressed }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var zoom = 1f
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            if (event.changes.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                
                                if (!pastTouchSlop) {
                                    zoom *= zoomChange
                                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                    val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                                    if (zoomMotion > touchSlop) {
                                        pastTouchSlop = true
                                    }
                                }
                                
                                if (pastTouchSlop) {
                                    val newScale = (scale * zoomChange).coerceIn(0.5f, 5f)
                                    onScaleChange(newScale)
                                    
                                    val zoomPercent = (newScale * 100).toInt()
                                    val zoomText = when {
                                        newScale == 1.0f -> "Zoom: Fit (100%)"
                                        newScale > 1.0f -> "Zoom: ${zoomPercent}%"
                                        else -> "Zoom: Minimized (${zoomPercent}%)"
                                    }
                                    
                                    onFeedbackEvent(
                                        FeedbackEvent(
                                            type = FeedbackType.ZOOM,
                                            icon = if (zoomChange >= 1f) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                                            text = zoomText,
                                            color = Color(0xFF4CAF50),
                                            id = 9999L // Stable ID to prevent toast animation flickering
                                        )
                                    )

                                    if (newScale > 1f) {
                                        val maxX = (size.width * (newScale - 1)) / 2
                                        val maxY = (size.height * (newScale - 1)) / 2
                                        val newOx = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                                        val newOy = (offsetY + panChange.y).coerceIn(-maxY, maxY)
                                        onOffsetChange(newOx, newOy)
                                    } else {
                                        onOffsetChange(0f, 0f)
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            }
            .pointerInput(longPressBoostSpeed) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val downTime = System.currentTimeMillis()
                    val touchSlop = viewConfiguration.touchSlop
                    var longPressTriggered = false
                    var movedPastSlop = false
                    var currentSavedSpeed = 1.0f
                    var currentSelectedIndex = -1
                    
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val allUp = event.changes.all { !it.pressed }
                        
                        if (allUp) {
                            if (longPressTriggered) {
                                val previewSpeed = if (currentSelectedIndex >= 0)
                                    SPEED_OPTIONS[currentSelectedIndex].speed else longPressBoostSpeed
                                viewModel.videoPlayer.player.setPlaybackSpeed(1.0f)
                                onLongPressStateChange(false, downPos.x, downPos.y, change.position.x, change.position.y, -1, currentSavedSpeed)
                                onFeedbackEvent(FeedbackEvent(FeedbackType.SPEED, Icons.Default.Speed,
                                    "${previewSpeed}x → 1×", color = Color(0xFFBB86FC)))
                            }
                            break
                        }
                        
                        if (!movedPastSlop && !longPressTriggered) {
                            val dx = change.position.x - downPos.x
                            val dy = change.position.y - downPos.y
                            if (dx * dx + dy * dy > touchSlop * touchSlop) {
                                movedPastSlop = true
                                break 
                            }
                        }
                        
                        if (!longPressTriggered && !movedPastSlop && System.currentTimeMillis() - downTime >= 300) {
                            longPressTriggered = true
                            currentSavedSpeed = viewModel.videoPlayer.player.playbackParameters.speed
                            viewModel.videoPlayer.player.setPlaybackSpeed(longPressBoostSpeed)
                            currentSelectedIndex = SPEED_OPTIONS.indexOfFirst { it.speed == longPressBoostSpeed }.coerceAtLeast(0)
                            onLongPressStateChange(true, downPos.x, downPos.y, change.position.x, change.position.y, currentSelectedIndex, currentSavedSpeed)
                            if (!isPlaying) viewModel.videoPlayer.play()
                        }
                        
                        if (longPressTriggered && event.changes.size == 1) {
                            val fx = change.position.x
                            val fy = change.position.y
                            val newIndex = resolveSpeedIndex(fx, fy, downPos.x, downPos.y)
                            if (newIndex != currentSelectedIndex) {
                                currentSelectedIndex = newIndex
                                if (newIndex >= 0) {
                                    val speed = SPEED_OPTIONS[newIndex].speed
                                    viewModel.videoPlayer.player.setPlaybackSpeed(speed)
                                }
                            }
                            onLongPressStateChange(true, downPos.x, downPos.y, fx, fy, currentSelectedIndex, currentSavedSpeed)
                            change.consume()
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                var isSeeking = false
                var isAdjustingVolBright = false
                var seekAccumulator = 0f
                var startPosition = 0L
                var initialVolume = 0f
                var initialBrightness = 0f
                var accumulatedY = 0f
                var lastBrightness = -1f
                var lastVolume = -1f

                detectDragGestures(
                    onDragStart = { offset ->
                        if (activePointers > 1) return@detectDragGestures
                        seekAccumulator = 0f
                        startPosition = viewModel.videoPlayer.player.currentPosition
                        isSeeking = false
                        isAdjustingVolBright = false
                        accumulatedY = 0f
                        lastBrightness = -1f
                        lastVolume = -1f
                        
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol
                        
                        activity?.window?.let { window ->
                            var currentBrightness = window.attributes.screenBrightness
                            if (currentBrightness < 0) currentBrightness = 0.5f
                            initialBrightness = currentBrightness
                        }
                    },
                    onDragEnd = {
                        if (activePointers > 1) {
                            isSeeking = false
                            isAdjustingVolBright = false
                            return@detectDragGestures
                        }
                        if (isSeeking) {
                            val screenWidth = size.width
                            val seekAmountMs = (seekAccumulator / screenWidth) * 120000
                            val dur = viewModel.videoPlayer.player.duration.coerceAtLeast(0L)
                            val newPos = if (dur > 0L) (startPosition + seekAmountMs.toLong()).coerceIn(0L, dur) else startPosition
                            viewModel.videoPlayer.player.seekTo(newPos)
                        } else if (isAdjustingVolBright) {
                            if (lastBrightness >= 0f) viewModel.preferencesUseCase.saveBrightness(lastBrightness)
                            if (lastVolume >= 0f) viewModel.preferencesUseCase.saveVolume(lastVolume)
                        }
                        isSeeking = false
                        isAdjustingVolBright = false
                    },
                    onDragCancel = {
                        isSeeking = false
                        isAdjustingVolBright = false
                        onFeedbackEvent(FeedbackEvent(FeedbackType.INFO, Icons.Default.Close, "", id = 9999L)) // Clear OSD immediately
                    }
                ) { change, dragAmount ->
                    if (activePointers > 1) {
                        isSeeking = false
                        isAdjustingVolBright = false
                        return@detectDragGestures
                    }
                    change.consume()
                    val screenWidth = size.width
                    val screenHeight = size.height
                    val isLeftSide = change.position.x < screenWidth / 2

                    if (!isSeeking && !isAdjustingVolBright) {
                        if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y) * 1.5f) {
                            isSeeking = true
                        } else if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x) * 1.5f) {
                            isAdjustingVolBright = true
                        }
                    }

                    if (isSeeking) {
                        seekAccumulator += dragAmount.x
                        val seekAmountMs = (seekAccumulator / screenWidth) * 120000
                        val newPos = (startPosition + seekAmountMs.toLong()).coerceIn(0, viewModel.videoPlayer.player.duration)
                        
                        val currentSecs = newPos / 1000
                        val m = currentSecs / 60
                        val s = currentSecs % 60
                        val sign = if (seekAmountMs >= 0) "+" else ""
                        val diffSecs = (seekAmountMs / 1000).toInt()
                        onFeedbackEvent(FeedbackEvent(FeedbackType.SEEK, Icons.AutoMirrored.Filled.CompareArrows, String.format("%02d:%02d (%s%ds)", m, s, sign, diffSecs)))
                    } else if (isAdjustingVolBright) {
                        accumulatedY += dragAmount.y
                        val delta = -accumulatedY / screenHeight * 1.5f 
                        
                        if (isLeftSide) {
                            activity?.window?.let { window ->
                                val params = window.attributes
                                val newB = (initialBrightness + delta).coerceIn(0f, 1f)
                                params.screenBrightness = newB
                                window.attributes = params
                                lastBrightness = newB
                                
                                onFeedbackEvent(FeedbackEvent(FeedbackType.BRIGHTNESS, Icons.Default.BrightnessMedium, "${(params.screenBrightness * 100).toInt()}%", value = params.screenBrightness, color = Color(0xFFFFEB3B)))
                            }
                        } else {
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val newVolFloat = initialVolume + delta
                            val newVol = (newVolFloat * maxVol).toInt().coerceIn(0, maxVol)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                            lastVolume = newVolFloat.coerceIn(0f, 1f)
                            
                            onFeedbackEvent(FeedbackEvent(FeedbackType.VOLUME, Icons.AutoMirrored.Filled.VolumeUp, "${(newVol.toFloat() / maxVol * 100).toInt()}%", value = newVolFloat.coerceIn(0f, 1f), color = Color(0xFF2196F3)))
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (scale != 1.0f) {
                            // Professional Reset Zoom Gesture
                            onScaleChange(1.0f)
                            onOffsetChange(0f, 0f)
                            onFeedbackEvent(
                                FeedbackEvent(
                                    type = FeedbackType.ZOOM,
                                    icon = Icons.Default.ZoomIn,
                                    text = "Zoom: Fit (100%)",
                                    color = Color(0xFF4CAF50)
                                )
                            )
                        } else {
                            val screenWidth = size.width
                            val centerWidth = screenWidth * 0.3f // Center 30%
                            val leftBound = (screenWidth - centerWidth) / 2
                            val rightBound = leftBound + centerWidth

                            if (offset.x < leftBound) {
                                viewModel.videoPlayer.seekBack()
                                val newSeek = if (seekAccumulation > 0) -10 else seekAccumulation - 10
                                onSeekAccumulationChange(newSeek)
                                onFeedbackEvent(FeedbackEvent(FeedbackType.SEEK, Icons.Default.FastRewind, "${newSeek}s"))
                            } else if (offset.x > rightBound) {
                                viewModel.videoPlayer.seekForward()
                                val newSeek = if (seekAccumulation < 0) 10 else seekAccumulation + 10
                                onSeekAccumulationChange(newSeek)
                                onFeedbackEvent(FeedbackEvent(FeedbackType.SEEK, Icons.Default.FastForward, "+${newSeek}s"))
                            } else {
                                // Center double tap -> toggle play/pause
                                if (isPlaying) {
                                    viewModel.videoPlayer.pause()
                                    onPlayPauseToggle(true) // show ad popup
                                } else {
                                    viewModel.videoPlayer.play()
                                    onPlayPauseToggle(false)
                                }
                            }
                        }
                    },
                    onTap = {
                        if (isToolsExpanded) {
                            onToolsExpandedChange(false)
                        } else {
                            onControllerVisibilityChange(!isControllerVisible)
                        }
                    },
                )
            }
    )
}
