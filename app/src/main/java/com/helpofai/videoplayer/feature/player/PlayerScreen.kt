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
package com.helpofai.videoplayer.feature.player

import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.AspectRatioFrameLayout
import com.helpofai.videoplayer.feature.player.components.CircularSpeedWheel
import com.helpofai.videoplayer.feature.player.components.FeedbackEvent
import com.helpofai.videoplayer.feature.player.components.FeedbackType
import com.helpofai.videoplayer.feature.player.components.PlayerFeedbackOverlay
import com.helpofai.videoplayer.feature.player.components.PlayerTopToolbar

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
        ?: context as? android.app.Activity

    LaunchedEffect(Unit) {
        viewModel.showMidRollAdEvent.collect {
            viewModel.videoPlayer.pause()
            activity?.let { act ->
                com.helpofai.videoplayer.core.ads.AdManager.showInterstitialAd(act) {
                    viewModel.videoPlayer.play()
                }
            } ?: run {
                viewModel.videoPlayer.play()
            }
        }
    }

    androidx.activity.compose.BackHandler(onBack = onNavigateBack)
    
    var isLongPressing by remember { mutableStateOf(false) }
    
    // Long-Press Speed Selector State
    var longPressSelectorVisible by remember { mutableStateOf(false) }
    var longPressCenterX by remember { mutableFloatStateOf(0f) }
    var longPressCenterY by remember { mutableFloatStateOf(0f) }
    var longPressFingerX by remember { mutableFloatStateOf(0f) }
    var longPressFingerY by remember { mutableFloatStateOf(0f) }
    var selectedSpeedIndex by remember { mutableIntStateOf(-1) }
    var savedSpeedBeforeBoost by remember { mutableFloatStateOf(1.0f) }
    val longPressBoostSpeed by viewModel.longPressBoostSpeed.collectAsState()
     
    // Resize Mode State (Fit -> Crop -> Fill)
    var resizeMode by remember { 
        mutableIntStateOf(viewModel.preferencesUseCase.getPreferredAspectRatio().toIntOrNull() ?: AspectRatioFrameLayout.RESIZE_MODE_FIT) 
    }
    
    // Rotation State
    var isLandscape by remember { mutableStateOf(false) }
    var isManualOrientationLocked by remember { mutableStateOf(false) }

    val currentVideoPath by viewModel.currentPathFlow.collectAsState()
    LaunchedEffect(currentVideoPath) {
        isManualOrientationLocked = false
    }
    
    // Dialog State
    var activeDialog by remember { mutableStateOf<com.helpofai.videoplayer.feature.player.components.PlayerDialogType?>(null) }
    
    // Subtitles/Audio State
    var trackSelectorInitialTab by remember { mutableIntStateOf(0) }

    // Decoder State
    var decoderMode by remember { mutableStateOf("HW") }
    
    val bookmarks by viewModel.bookmarks.collectAsState()

    // Auto Chapters State
    var isGeneratingChapters by remember { mutableStateOf(false) }

    // Loop State
    var isLooping by remember { mutableStateOf(
        try { viewModel.videoPlayer.player.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE } catch (_: Exception) { false }
    ) }

    // Lock State
    var isControlsLocked by remember { mutableStateOf(false) }

    // Zoom and Pan State
    val initialZoom by viewModel.zoomLevel.collectAsState()
    var scale by remember(initialZoom) { mutableFloatStateOf(initialZoom) }
    
    LaunchedEffect(scale) {
        viewModel.lastZoomLevel = scale
    }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showVideoEnhancer by remember { mutableStateOf(false) }
    val enhancementConfig by viewModel.videoEnhancementManager.config.collectAsState()
    var rotationZ by remember { mutableFloatStateOf(0f) }
    var isBuffering by remember { mutableStateOf(false) }
    var processData by remember { mutableStateOf("") }
    
    var isMirrored by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }

    // OSD Feedback State
    var feedbackEvent by remember { mutableStateOf<FeedbackEvent?>(null) }
    var seekAccumulation by remember { mutableIntStateOf(0) }
    var isControllerVisible by remember { mutableStateOf(true) }
    var autoHideTrigger by remember { mutableStateOf(0) }
    var isToolsExpanded by remember { mutableStateOf(false) }
    var currentVideoTitle by remember { mutableStateOf("Video Player") }
    val watchPartyVideoTitle by viewModel.watchPartyVideoTitle.collectAsState()
    // Use real title from session when client is streaming from host
    LaunchedEffect(watchPartyVideoTitle) {
        watchPartyVideoTitle?.let { currentVideoTitle = it }
    }

    val playlist by viewModel.playlist.collectAsState()
    val isPlayPauseAllowed by viewModel.isPlayPauseAllowed.collectAsState()
    val isSeekAllowed by viewModel.isSeekAllowed.collectAsState()
    val isVolumeAllowed by viewModel.isVolumeAllowed.collectAsState()
    val isAudioTrackAllowed by viewModel.isAudioTrackAllowed.collectAsState()
    val isSubtitleToggleAllowed by viewModel.isSubtitleToggleAllowed.collectAsState()
    val isGesturesAllowed by viewModel.isGesturesAllowed.collectAsState()

    // AB Repeat State
    var abRepeatA by remember { mutableStateOf<Long?>(null) }
    var abRepeatB by remember { mutableStateOf<Long?>(null) }
    
    // Playback State
    var isPlaying by remember { mutableStateOf(viewModel.videoPlayer.player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(viewModel.videoPlayer.player.currentPosition) }
    var duration by remember { mutableLongStateOf(viewModel.videoPlayer.player.duration.coerceAtLeast(0)) }
    
    val isInPipMode = rememberIsInPipMode()
    val isBgPlaybackEnabled by viewModel.backgroundPlaybackEnabled.collectAsState()

    LaunchedEffect(Unit) {
        val intent = android.content.Intent(context, com.helpofai.videoplayer.core.playback.PlaybackService::class.java)
        try {
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val subtitlePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.addExternalSubtitle(uri)
        if (uri != null) {
            feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Subtitles, "External Subtitle Loaded")
        }
    }

    DisposableEffect(Unit) {
        com.helpofai.videoplayer.MainActivity.isPlayerActive = true
        
        // Apply Learned Global Preferences
        val prefBright = viewModel.preferencesUseCase.getPreferredBrightness()
        if (prefBright >= 0f) {
            activity?.window?.let { win ->
                val params = win.attributes
                params.screenBrightness = prefBright
                win.attributes = params
            }
        }
        
        val prefVol = viewModel.preferencesUseCase.getPreferredVolume()
        if (prefVol >= 0f) {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (prefVol * maxVol).toInt(), 0)
        }

        // Smart Pause on Headphone Disconnection
        val headphoneReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    viewModel.videoPlayer.pause()
                }
            }
        }
        val noisyFilter = android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        context.registerReceiver(headphoneReceiver, noisyFilter)

        onDispose {
            try { context.unregisterReceiver(headphoneReceiver) } catch (e: Exception) {}
            com.helpofai.videoplayer.MainActivity.isPlayerActive = false
        }
    }


    LaunchedEffect(seekAccumulation) {
        if (seekAccumulation != 0) {
            kotlinx.coroutines.delay(800)
            seekAccumulation = 0
        }
    }
    
    LaunchedEffect(isControllerVisible, isPlaying) {
        while (isControllerVisible && isPlaying) {
            currentPosition = viewModel.videoPlayer.player.currentPosition
            duration = viewModel.videoPlayer.player.duration.coerceAtLeast(0)
            kotlinx.coroutines.delay(1000)
        }
    }

    // Auto-hide controls after 5 seconds, reset whenever interaction happens
    LaunchedEffect(isControllerVisible, isPlaying, isControlsLocked, isToolsExpanded, autoHideTrigger) {
        if (isControllerVisible && isPlaying && !isControlsLocked && !isToolsExpanded) {
            kotlinx.coroutines.delay(5000)
            isControllerVisible = false
        }
    }

    DisposableEffect(lifecycleOwner, isInPipMode, isBgPlaybackEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isInPipMode && !isBgPlaybackEnabled) {
                        viewModel.videoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> viewModel.videoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        // Enter immersive mode
        val window = activity?.window
        window?.let { androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false) }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window?.attributes?.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        
        val decorView = window?.decorView
        val insetsListener = android.view.View.OnApplyWindowInsetsListener { view, insets ->
            insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.onApplyWindowInsets(insets)
        }
        decorView?.setOnApplyWindowInsetsListener(insetsListener)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            decorView?.setOnApplyWindowInsetsListener(null)
            // Exit immersive mode
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            window?.let { androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, true) }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                window?.attributes?.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            // Restore orientation when leaving player
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Continuously enforce immersive mode to prevent Android 15 / rotation from breaking it
    SideEffect {
        val window = activity?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    }


    // Auto-rotate and Play/Pause feedback
    DisposableEffect(viewModel.videoPlayer.player) {
        val player = viewModel.videoPlayer.player

        fun applyAutoRotation(videoSize: androidx.media3.common.VideoSize) {
            if (isManualOrientationLocked) return

            if (videoSize.width > 0 && videoSize.height > 0) {
                val isRotated = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
                val effectiveWidth = if (isRotated) videoSize.height.toFloat() else videoSize.width.toFloat()
                val effectiveHeight = if (isRotated) videoSize.width.toFloat() else videoSize.height.toFloat()
                val aspectRatio = effectiveWidth / effectiveHeight

                val targetOrientation = when {
                    aspectRatio > 1.08f -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    aspectRatio < 0.92f -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }

                isLandscape = targetOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                activity?.let { act ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        act.requestedOrientation = targetOrientation
                    }
                }
            }
        }

        // Apply auto-rotation immediately for already-prepared videos
        applyAutoRotation(player.videoSize)

        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                applyAutoRotation(videoSize)
            }
            
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                val titleFromMeta = mediaMetadata.title?.toString()
                // Only update from media metadata if we're NOT in client stream mode
                if (titleFromMeta != null && titleFromMeta != "http_stream" && watchPartyVideoTitle == null) {
                    currentVideoTitle = titleFromMeta
                }
            }
            
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
                feedbackEvent = FeedbackEvent(
                    type = FeedbackType.PLAY_PAUSE,
                    icon = if (isPlayingChange) Icons.Default.PlayArrow else Icons.Default.Pause,
                    text = if (isPlayingChange) "Play" else "Pause"
                )
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                if (isBuffering) {
                    val tracks = viewModel.videoPlayer.player.currentTracks
                    val sb = StringBuilder()
                    tracks.groups.forEach { group ->
                        if (group.isSelected) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSelected(i)) {
                                    val format = group.getTrackFormat(i)
                                    val mime = format.sampleMimeType ?: "Unknown Codec"
                                    val type = if (mime.startsWith("video/")) "VIDEO" else if (mime.startsWith("audio/")) "AUDIO" else "DATA"
                                    sb.append("[$type] ${mime.substringAfter("/").uppercase()}\n")
                                    if (format.width > 0 && format.height > 0) {
                                        sb.append("  Resolution: ${format.width}x${format.height}")
                                        if (format.frameRate > 0) sb.append(" @ ${format.frameRate}fps")
                                        sb.append("\n")
                                    }
                                    if (format.bitrate > 0) sb.append("  Bitrate: ${format.bitrate / 1000} kbps\n")
                                }
                            }
                        }
                    }
                    if (sb.isEmpty()) {
                        processData = "Parsing Media Streams...\nDecoding High Quality Source"
                    } else {
                        processData = "DECODING PIPELINE:\n" + sb.toString().trim()
                    }
                }
            }
        }
        viewModel.videoPlayer.player.addListener(listener)
        onDispose {
            viewModel.videoPlayer.player.removeListener(listener)
        }
    }

    // SubtitleView reference for reactive style updates
    var sbView by remember { mutableStateOf<androidx.media3.ui.SubtitleView?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Player View
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = viewModel.videoPlayer.player
                    useController = false
                    keepScreenOn = true
                    subtitleView?.let { sv ->
                        sv.visibility = android.view.View.VISIBLE
                        sbView = sv
                    }
                    // Apply user-preference subtitle style from SubtitleStyleManager
                    val styleManager = viewModel.subtitleStyleManager
                    subtitleView?.let { styleManager.applyToSubtitleView(it, styleManager.currentConfig()) }
                }
            },
            update = { playerView ->
                playerView.player = viewModel.videoPlayer.player
                playerView.resizeMode = resizeMode
                // Keep subtitleView ref updated
                playerView.subtitleView?.let { sbView = it }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale * if (isMirrored) -1f else 1f
                scaleY = scale * if (isFlipped) -1f else 1f
                translationX = offsetX
                translationY = offsetY
                this.rotationZ = rotationZ
            }
        )

        // Reactively apply subtitle style whenever user preferences change
        LaunchedEffect(sbView) {
            if (sbView == null) return@LaunchedEffect
            viewModel.subtitleStyleManager.config.collect { config ->
                sbView?.let { viewModel.subtitleStyleManager.applyToSubtitleView(it, config) }
            }
        }
        


        if (!isInPipMode) {
            if (!isControlsLocked) {
            // Gesture Overlay Layer (Double tap, Long press, Swipe, Zoom)
            com.helpofai.videoplayer.feature.player.components.PlayerGestureSurface(
                viewModel = viewModel,
                activity = activity,
                context = context,
                isPlaying = isPlaying,
                isToolsExpanded = isToolsExpanded,
                isControllerVisible = isControllerVisible,
                longPressBoostSpeed = longPressBoostSpeed,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                seekAccumulation = seekAccumulation,
                onScaleChange = { scale = it; autoHideTrigger++ },
                onOffsetChange = { ox, oy -> offsetX = ox; offsetY = oy; autoHideTrigger++ },
                onSeekAccumulationChange = { seekAccumulation = it; autoHideTrigger++ },
                onControllerVisibilityChange = { isControllerVisible = it; autoHideTrigger++ },
                onToolsExpandedChange = { isToolsExpanded = it; autoHideTrigger++ },
                onPlayPauseToggle = { showAd -> 
                    if (showAd) activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.AD_POPUP 
                    else if (activeDialog == com.helpofai.videoplayer.feature.player.components.PlayerDialogType.AD_POPUP) activeDialog = null
                    autoHideTrigger++
                },
                onFeedbackEvent = { feedbackEvent = it; autoHideTrigger++ },
                onLongPressStateChange = { visible, cx, cy, fx, fy, idx, savedSpeed ->
                    longPressSelectorVisible = visible
                    longPressCenterX = cx
                    longPressCenterY = cy
                    longPressFingerX = fx
                    longPressFingerY = fy
                    selectedSpeedIndex = idx
                    savedSpeedBeforeBoost = savedSpeed
                    autoHideTrigger++
                },
                isPlayPauseAllowed = isPlayPauseAllowed,
                isSeekAllowed = isSeekAllowed,
                isVolumeAllowed = isVolumeAllowed,
                isGesturesAllowed = isGesturesAllowed
            )
        }
        

        if (!isControlsLocked) {
            // Modern MX Player Inspired Top Toolbar
            PlayerTopToolbar(
                isVisible = isControllerVisible,
                title = currentVideoTitle,
                onBackClick = onNavigateBack,
                onMinimizeClick = {
                    viewModel.minimizePlayer()
                    onNavigateBack()
                },
                onLockClick = { isControlsLocked = true },
                onSpeedClick = { activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.SPEED_DIAL },
                onEqClick = { activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.EQUALIZER },
                onLoopClick = {
                    val player = viewModel.videoPlayer.player
                    player.repeatMode = if (isLooping) androidx.media3.common.Player.REPEAT_MODE_OFF else androidx.media3.common.Player.REPEAT_MODE_ONE
                    isLooping = !isLooping
                    feedbackEvent = FeedbackEvent(
                        type = FeedbackType.INFO,
                        icon = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        text = if (isLooping) "Loop On" else "Loop Off"
                    )
                },
                abRepeatState = when {
                    abRepeatA != null && abRepeatB != null -> "A-B"
                    abRepeatA != null -> "A"
                    else -> ""
                },
                onABRepeatClick = {
                    val pos = viewModel.videoPlayer.player.currentPosition
                    if (abRepeatA == null) {
                        abRepeatA = pos
                        feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Repeat, "Point A Set")
                    } else if (abRepeatB == null) {
                        if (pos > abRepeatA!!) {
                            abRepeatB = pos
                            feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Repeat, "Point B Set")
                        } else {
                            feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Repeat, "Invalid B Point")
                        }
                    } else {
                        abRepeatA = null
                        abRepeatB = null
                        feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Repeat, "A-B Repeat Cleared")
                    }
                },
                onAudioClick = {
                    if (isAudioTrackAllowed) {
                        trackSelectorInitialTab = 0
                        activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.TRACK_SELECTOR_AUDIO
                    } else {
                        android.widget.Toast.makeText(context, "Host has disabled changing audio tracks", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onSubtitlesClick = {
                    if (isSubtitleToggleAllowed) {
                        trackSelectorInitialTab = 1
                        activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.TRACK_SELECTOR_SUBTITLE
                    } else {
                        android.widget.Toast.makeText(context, "Host has disabled subtitle control", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onScreenshotClick = {
                    viewModel.takeScreenshot(context) { path ->
                        feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.PhotoCamera, "Screenshot Saved")
                    }
                },
                onInfoClick = { activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.VIDEO_INFO },
                onRotateClick = {
                    isManualOrientationLocked = true
                    isLandscape = !isLandscape
                    activity?.requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                },
                onVideoEnhancerClick = {
                    if (isGesturesAllowed) {
                        showVideoEnhancer = true
                    } else {
                        android.widget.Toast.makeText(context, "Host has disabled video enhancements", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onVideoAdjustmentsClick = {
                    if (isGesturesAllowed) {
                        activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.VIDEO_ADJUSTMENTS
                    } else {
                        android.widget.Toast.makeText(context, "Host has disabled video adjustments", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onMoreClick = { activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.MORE_POPUP },
                isToolsExpanded = isToolsExpanded,
                onToolsExpandedChange = { isToolsExpanded = it },
                onEmptyClick = { 
                    isControllerVisible = !isControllerVisible
                    autoHideTrigger++
                },
                isStreaming = com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance().activeSession.value != null,
                isHost = !com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance().isClientMode,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // Watch Party Host Monitoring Overlay
            val sessionManager = com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance()
            val activeSession by sessionManager.activeSession.collectAsState()
            
            // Streaming/LIVE tag
            if (activeSession != null) {
                val isHost = !sessionManager.isClientMode
                val tagText = "LIVE STREAMING"
                val tagColor = if (isHost) Color(0xFF7C5CE7) else Color(0xFFE74C3C)
                Surface(
                    color = tagColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 80.dp)
                ) {
                    Text(
                        text = tagText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (activeSession != null && !sessionManager.isClientMode && isControllerVisible) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 100.dp, end = 16.dp)
                        .width(250.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard.WatchPartyPlayerMonitoringView(
                        session = activeSession!!
                    )
                }
            }
        }
        if (!isControlsLocked) {
            val playbackState by viewModel.videoPlayer.playbackState.collectAsState()
            com.helpofai.videoplayer.feature.player.components.PlayerBottomController(
                isVisible = isControllerVisible,
                isPlaying = isPlaying,
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                isLandscape = isLandscape,
                bookmarks = emptyList(), // Pass actual bookmarks later if needed
                lastPlayedPosition = null,
                onPlayPauseClick = {
                    if (isPlayPauseAllowed) {
                        viewModel.togglePlayPause()
                    } else {
                        android.widget.Toast.makeText(context, "Host has disabled play/pause control", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onSeek = { pos -> 
                    if (isSeekAllowed) {
                        viewModel.seekTo(pos) 
                    } else {
                        android.widget.Toast.makeText(context, "Host has disabled seeking", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onNextClick = { viewModel.playNextVideo() },
                onPrevClick = { viewModel.playPrevVideo() },
                onFullscreenClick = {
                    isLandscape = !isLandscape
                    activity?.requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                },
                isSeekEnabled = isSeekAllowed,
                abRepeatA = abRepeatA,
                abRepeatB = abRepeatB,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Resume Prompt
        AnimatedVisibility(
            visible = viewModel.showResumePrompt,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 120.dp, end = 16.dp)
        ) {
            val resumeSecs = viewModel.resumePosition / 1000
            val formattedResume = String.format(java.util.Locale.US, "%02d:%02d:%02d", resumeSecs / 3600, (resumeSecs % 3600) / 60, resumeSecs % 60)
                .replaceFirst("^00:".toRegex(), "")
            
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Resume playing?", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        Text(formattedResume, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { viewModel.onResumeAccepted() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Continue", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { viewModel.onResumeDismissed() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Locked Screen Click Handler
        if (isControlsLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        isControllerVisible = !isControllerVisible
                        autoHideTrigger++
                    }
            )
        }

        // Lock Controls Button
        AnimatedVisibility(
            visible = isControllerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            IconButton(
                onClick = { isControlsLocked = !isControlsLocked },
                modifier = Modifier
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Controls",
                    tint = if (isControlsLocked) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        val qualityReport by viewModel.qualityReport.collectAsState()
        val isAnalyzingQuality by viewModel.isAnalyzingQuality.collectAsState()

        com.helpofai.videoplayer.feature.player.components.PlayerDialogManager(
            activeDialog = activeDialog,
            onDismissRequest = { activeDialog = null },
            viewModel = viewModel,
            decoderMode = decoderMode,
            onDecoderModeSelect = {
                decoderMode = it
                activeDialog = null
            },

            resizeMode = resizeMode,
            onResizeModeSelected = { resizeMode = it },
            brightness = viewModel.preferencesUseCase.getPreferredBrightness().coerceAtLeast(0f),
            onBrightnessChanged = { 
                activity?.window?.let { win ->
                    val params = win.attributes
                    params.screenBrightness = it
                    win.attributes = params
                }
                viewModel.preferencesUseCase.saveBrightness(it)
            },
            isMirrored = isMirrored,
            onMirrorToggled = { isMirrored = it },
            isFlipped = isFlipped,
            onFlipToggled = { isFlipped = it },
            rotationZ = rotationZ,
            onRotationChanged = { rotationZ = it; feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.AutoMirrored.Filled.RotateRight, "${(it % 360).toInt()}°") },
            qualityReport = qualityReport,
            isAnalyzingQuality = isAnalyzingQuality,
            playlist = playlist,
            currentVideoPath = viewModel.currentVideoPath ?: "",
            onVideoSelect = { path ->
                viewModel.playVideo(path)
                activeDialog = null
            },
            onReorderPlaylist = { from, to -> viewModel.reorderPlaylist(from, to) },
            onShowDialog = { activeDialog = it },
            isPlaying = isPlaying,
            bookmarks = bookmarks,
            onSeekTo = { pos -> 
                viewModel.videoPlayer.player.seekTo(pos)
                currentPosition = pos
                activeDialog = null
            },
            isGeneratingChapters = isGeneratingChapters,
            onGenerateAutoChapters = { onStart, onComplete ->
                viewModel.generateAutoChapters(
                    onStart = {
                        isGeneratingChapters = true
                        onStart()
                    },
                    onComplete = { success ->
                        isGeneratingChapters = false
                        onComplete(success)
                    }
                )
            },
            onLoadExternalSubtitle = { subtitlePickerLauncher.launch("*/*") },
            onOpenSubtitleStyle = { activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.SUBTITLE_STYLE },
            onFeedbackEvent = { feedbackEvent = it }
        )
        val mediaReport by viewModel.mediaCompatibilityReport.collectAsState()
        if (showVideoEnhancer) {
            com.helpofai.videoplayer.feature.player.components.VideoEnhancerSheet(
                enhancementManager = viewModel.videoEnhancementManager,
                report = mediaReport,
                onDismissRequest = { showVideoEnhancer = false }
            )
        }

        if (isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x99000000), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PROCESSING MEDIA",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = processData,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
        
        } // End of !isInPipMode wrapper

        // Professional OSD Feedback Layer
        PlayerFeedbackOverlay(
            feedback = feedbackEvent,
            isLongPressing = isLongPressing,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Long-Press Speed Selector Overlay
        CircularSpeedWheel(
            isVisible = longPressSelectorVisible,
            centerX = longPressCenterX,
            centerY = longPressCenterY,
            fingerX = longPressFingerX,
            fingerY = longPressFingerY,
            selectedIndex = selectedSpeedIndex,
            currentSpeed = 1.0f,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun rememberIsInPipMode(): Boolean {
    val context = LocalContext.current
    val activity = (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
        ?: context as? android.app.Activity
    var pipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode ?: false) }
    
    DisposableEffect(activity) {
        val observer = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            pipMode = info.isInPictureInPictureMode
        }
        (activity as? androidx.activity.ComponentActivity)?.addOnPictureInPictureModeChangedListener(observer)
        onDispose {
            (activity as? androidx.activity.ComponentActivity)?.removeOnPictureInPictureModeChangedListener(observer)
        }
    }
    
    return pipMode
}