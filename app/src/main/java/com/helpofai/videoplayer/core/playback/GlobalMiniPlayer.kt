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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    if (!isActive || video == null) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Floating card dimensions
    val widthDp = 220.dp
    val heightDp = 124.dp
    val cardWidthPx = with(density) { widthDp.toPx() }
    val cardHeightPx = with(density) { heightDp.toPx() }

    // Initial position: Bottom right corner with margins
    val marginPx = with(density) { 16.dp.toPx() }
    val bottomOffsetPx = with(density) { 80.dp.toPx() }
    var offsetX by remember { mutableStateOf<Float>(screenWidthPx - cardWidthPx - marginPx) }
    var offsetY by remember { mutableStateOf<Float>(screenHeightPx - cardHeightPx - marginPx - bottomOffsetPx) }

    // State observers
    var isPlaying by remember { mutableStateOf(videoPlayer.player.isPlaying) }

    DisposableEffect(videoPlayer.player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        videoPlayer.player.addListener(listener)
        onDispose {
            videoPlayer.player.removeListener(listener)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {} // Block touch propagation to underlying screens
    ) {
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
                .clickable { onRestore(video!!) },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161922),
            shadowElevation = 12.dp,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Seamlessly bind singleton ExoPlayer surface directly
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = videoPlayer.player
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Dark subtle overlay gradient for controls visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                // Close/Dismiss Button (Top-Right)
                IconButton(
                    onClick = {
                        videoPlayer.release()
                        manager.dismissMiniPlayer()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Play/Pause Button overlay (Center)
                IconButton(
                    onClick = {
                        if (isPlaying) videoPlayer.pause() else videoPlayer.play()
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Video Title info (Bottom)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = video?.title ?: "Streaming",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
