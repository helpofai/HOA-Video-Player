package com.helpofai.videoplayer.feature.watch_party.ui.mini_player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession
import kotlin.math.roundToInt

private val BgCard       = Color(0xFF111520)
private val DivColor     = Color(0xFF1E2535)
private val AccentPurple = Color(0xFF7C5CE7)
private val AccentCyan   = Color(0xFF00CEC9)
private val TextPrimary  = Color(0xFFECF0F1)
private val TextSub      = Color(0xFF8E9CB0)

@OptIn(UnstableApi::class)
@Composable
fun WatchPartyMiniPlayer(
    session: WatchPartySession,
    onOpenFullPlayer: () -> Unit
) {
    val context = LocalContext.current
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val isStreaming = session.video != null

    // Track tab/lifecycle visibility to stop background streaming when player screen opens
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Only play if the Watch Party tab is resumed and active in the foreground
            isLifecycleResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Setup miniature ExoPlayer instance for live preview
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Initialize/release ExoPlayer based on streaming status and lifecycle resume state
    DisposableEffect(isStreaming, session.video?.id, isLifecycleResumed) {
        if (isStreaming && session.video != null && isLifecycleResumed) {
            val player = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                // Mute preview player so it doesn't conflict with main audio
                volume = 0f 
                playWhenReady = session.isPlaying
            }
            
            // Resolve Uri
            val videoUri = if (session.hostIp.isNotBlank() && session.hostIp != "127.0.0.1") {
                // Client gets stream from host HTTP server
                android.net.Uri.parse("http://${session.hostIp}:${session.port}/video")
            } else {
                // Host plays its local URI
                session.video.uri
            }

            player.setMediaItem(MediaItem.fromUri(videoUri))
            player.prepare()
            if (session.currentPositionMs > 0L) {
                player.seekTo(session.currentPositionMs)
            }
            exoPlayer = player
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Sync play/pause state and playback position from session updates
    LaunchedEffect(session.isPlaying, session.currentPositionMs) {
        exoPlayer?.let { player ->
            player.playWhenReady = session.isPlaying
            if (kotlin.math.abs(player.currentPosition - session.currentPositionMs) > 2000L) {
                player.seekTo(session.currentPositionMs)
            }
        }
    }

    // Pulsing animation for the "STREAMING" badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Glowing border gradient
    val borderGradient = Brush.sweepGradient(
        colors = listOf(AccentPurple, AccentCyan, AccentPurple)
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .width(320.dp)
            .padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BgCard,
            border = BorderStroke(1.dp, borderGradient),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Video visual preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isStreaming && exoPlayer != null && isLifecycleResumed) {
                        // Render actual video frame preview
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    player = exoPlayer
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view ->
                                view.player = exoPlayer
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Animated gradient simulation of playing video
                        val animatedOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1000f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "offset"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF2C3E50), Color(0xFF3498DB))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF00CEC9).copy(alpha = 0.2f), Color.Transparent),
                                        start = androidx.compose.ui.geometry.Offset(animatedOffset, 0f),
                                        end = androidx.compose.ui.geometry.Offset(animatedOffset + 200f, 200f)
                                    )
                                )
                        )

                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = TextPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // LIVE / STREAMING / IDLE indicator badge
                    val badgeColor = if (isStreaming) Color.Red.copy(alpha = alpha) else Color(0xFF7C5CE7).copy(alpha = 0.8f)
                    val badgeText = if (isStreaming) "LIVE STREAMING" else "ROOM ACTIVE (IDLE)"
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }

                // Info Section
                Column {
                    Text(
                        text = session.video?.title ?: "No active stream",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Connected in: ${session.name}",
                        color = TextSub,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Call to Action
                Button(
                    onClick = onOpenFullPlayer,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isStreaming) AccentPurple else Color(0xFF2C3E50)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isStreaming) "Streaming in Full Player" else "Open Player (Idle)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
