/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.playback

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.theme.Primary
import com.helpofai.videoplayer.core.theme.Secondary
import kotlin.math.roundToInt

@Composable
fun GlobalMiniPlayer(
    videoPlayer: VideoPlayer,
    onRestore: (Video) -> Unit,
    modifier: Modifier = Modifier
) {
    val manager = GlobalMiniPlayerManager.getInstance()
    val isActive by manager.isMiniPlayerActive.collectAsState()
    val video by manager.activeVideo.collectAsState()
    val isInPipMode by manager.isInPipMode.collectAsState()

    if (!isActive) return
    // Snapshot the active video once the guard passed — safe to use in callbacks.
    val currentVideo = video ?: return

    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Floating card dimensions (Slightly larger for clean control placement)
    val widthDp = 240.dp
    val heightDp = 140.dp
    val cardWidthPx = with(density) { widthDp.toPx() }
    val cardHeightPx = with(density) { heightDp.toPx() }

    // Initial position: Bottom right corner with margins
    val marginPx = with(density) { 16.dp.toPx() }
    val bottomOffsetPx = with(density) { 80.dp.toPx() }
    var offsetX by remember { mutableStateOf<Float>(screenWidthPx - cardWidthPx - marginPx) }
    var offsetY by remember { mutableStateOf<Float>(screenHeightPx - cardHeightPx - marginPx - bottomOffsetPx) }

    // Auto-clamp position on orientation/screen size changes
    LaunchedEffect(screenWidthPx, screenHeightPx) {
        val maxOffsetX = screenWidthPx - cardWidthPx
        if (maxOffsetX >= 0f) {
            offsetX = offsetX.coerceIn(0f, maxOffsetX)
        } else {
            offsetX = 0f
        }

        val maxOffsetY = screenHeightPx - cardHeightPx
        if (maxOffsetY >= 0f) {
            offsetY = offsetY.coerceIn(0f, maxOffsetY)
        } else {
            offsetY = 0f
        }
    }

    // Playback state observation for progress tracking and play/pause state
    val playbackState by videoPlayer.playbackState.collectAsState()
    
    val isPlaying = if (!videoPlayer.isReleased) playbackState.isPlaying else false
    val currentPosition = if (!videoPlayer.isReleased) playbackState.currentPosition else 0L
    val duration = if (!videoPlayer.isReleased) playbackState.duration else 0L

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-hide controls after 3.5 seconds of inactivity while playing
    LaunchedEffect(controlsVisible, isPlaying, lastInteractionTime) {
        if (controlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3500)
            controlsVisible = false
        }
    }

    // Mute state, keyed to the active video so switching videos re-syncs it.
    var isMuted by remember(currentVideo.id, videoPlayer.isReleased) {
        mutableStateOf(!videoPlayer.isReleased && videoPlayer.player.volume == 0f)
    }
    // Remembered pre-mute volume so unmute restores the exact level instead
    // of jumping straight back to full volume.
    var lastNonMutedVolume by remember(currentVideo.id) { mutableStateOf(1f) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isInPipMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                if (!videoPlayer.isReleased) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = videoPlayer.player
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        update = { view ->
                            if (!videoPlayer.isReleased) view.player = videoPlayer.player
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(widthDp, heightDp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - cardWidthPx)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - cardHeightPx)
                        }
                    }
                    .clickable {
                        controlsVisible = !controlsVisible
                        if (controlsVisible) {
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEC0D0F17), // Glassmorphic dark blue-gray
                border = BorderStroke(1.dp, SolidColor(Color.White.copy(alpha = 0.08f))),
                shadowElevation = 16.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Seamlessly bind singleton ExoPlayer surface directly
                    if (!videoPlayer.isReleased) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    player = videoPlayer.player
                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                            },
                            update = { view ->
                                if (!videoPlayer.isReleased) view.player = videoPlayer.player
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    // Controls overlay with smooth fade in/out
                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
                        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(250)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 6.dp)
                            ) {
                                // Top Toolbar (Title & Utility Controls)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentVideo.title.ifBlank { "Streaming" },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .basicMarquee(iterations = Int.MAX_VALUE)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Mute / Unmute Button
                                    IconButton(
                                        onClick = {
                                            lastInteractionTime = System.currentTimeMillis()
                                            if (!videoPlayer.isReleased) {
                                                if (isMuted) {
                                                    videoPlayer.player.volume = lastNonMutedVolume
                                                    isMuted = false
                                                } else {
                                                    if (videoPlayer.player.volume > 0f) {
                                                        lastNonMutedVolume = videoPlayer.player.volume
                                                    }
                                                    videoPlayer.player.volume = 0f
                                                    isMuted = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Mute Toggle",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Restore/Fullscreen Button
                                    IconButton(
                                        onClick = { onRestore(currentVideo) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Restore Fullscreen",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Close/Dismiss Button
                                    IconButton(
                                        onClick = {
                                            videoPlayer.pause()
                                            manager.dismissMiniPlayer()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Center Control Options (Seek Back, Play/Pause, Seek Forward)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Seek Backward 10s
                                    IconButton(
                                        onClick = {
                                            lastInteractionTime = System.currentTimeMillis()
                                            if (!videoPlayer.isReleased) videoPlayer.seekBack()
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FastRewind,
                                            contentDescription = "Seek Back 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Play/Pause Button
                                    IconButton(
                                        onClick = {
                                            lastInteractionTime = System.currentTimeMillis()
                                            if (!videoPlayer.isReleased) {
                                                if (isPlaying) videoPlayer.pause() else videoPlayer.play()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(19.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(19.dp))
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Seek Forward 10s
                                    IconButton(
                                        onClick = {
                                            lastInteractionTime = System.currentTimeMillis()
                                            if (!videoPlayer.isReleased) videoPlayer.seekForward()
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FastForward,
                                            contentDescription = "Seek Forward 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Bottom Metadata / Time Status Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Timer / Progress bar at the very bottom edge (Always visible)
                    val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        val width = size.width
                        val height = size.height
                        // Background track line
                        drawRect(
                            color = Color.White.copy(alpha = 0.12f),
                            size = size
                        )
                        // Beautiful accent gradient active progress line
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Primary,   // Brand purple
                                    Secondary // Brand teal
                                )
                            ),
                            size = androidx.compose.ui.geometry.Size(width * progress, height)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
