package com.helpofai.videoplayer.feature.player

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.helpofai.videoplayer.feature.player.components.FeedbackEvent
import com.helpofai.videoplayer.feature.player.components.FeedbackType
import com.helpofai.videoplayer.feature.player.components.BookmarksSheet
import com.helpofai.videoplayer.feature.player.components.PlayerBottomController
import com.helpofai.videoplayer.feature.player.components.DecoderSelectorSheet
import com.helpofai.videoplayer.feature.player.components.PlayerFeedbackOverlay
import com.helpofai.videoplayer.feature.player.components.PlayerMorePopup
import com.helpofai.videoplayer.feature.player.components.PlayerTopToolbar
import com.helpofai.videoplayer.feature.player.components.PlayerTopToolbar
import com.helpofai.videoplayer.feature.player.components.VideoEnhancerSheet
import com.helpofai.videoplayer.feature.player.components.VideoAdjustmentsSheet

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
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

    var isLongPressing by remember { mutableStateOf(false) }
    
    // Resize Mode State (Fit -> Crop -> Fill)
    var resizeMode by remember { 
        mutableIntStateOf(viewModel.learningEngine.getPreferredAspectRatio().toIntOrNull() ?: AspectRatioFrameLayout.RESIZE_MODE_FIT) 
    }
    
    // Rotation State
    var isLandscape by remember { mutableStateOf(false) }
    
    // Subtitles/Audio State
    var showTrackSelector by remember { mutableStateOf(false) }
    var trackSelectorInitialTab by remember { mutableIntStateOf(0) }

    // Decoder State
    var decoderMode by remember { mutableStateOf("HW") }
    var showDecoderSelector by remember { mutableStateOf(false) }
    
    // Info Dialog State
    var showInfoDialog by remember { mutableStateOf(false) }
    
    // Equalizer State
    var showEqualizer by remember { mutableStateOf(false) }
    
    // Speed Dial State
    var showSpeedDial by remember { mutableStateOf(false) }
    
    // Video Adjustments State
    var showVideoAdjustments by remember { mutableStateOf(false) }
    
    // More Popup State
    var showMorePopup by remember { mutableStateOf(false) }
    
    // Bookmarks State
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showAdPopup by remember { mutableStateOf(false) }
    val bookmarks by viewModel.bookmarks.collectAsState()
    
    // Subtitles State
    var showSubtitlesSheet by remember { mutableStateOf(false) }

    // Auto Chapters State
    var isGeneratingChapters by remember { mutableStateOf(false) }

    // Loop State
    var isLooping by remember { mutableStateOf(viewModel.videoPlayer.player.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) }

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
    var activeEnhancerProfile by remember { mutableStateOf("standard") }
    var rotationZ by remember { mutableFloatStateOf(0f) }
    var isBuffering by remember { mutableStateOf(false) }
    var processData by remember { mutableStateOf("") }
    
    var isMirrored by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }

    // OSD Feedback State
    var feedbackEvent by remember { mutableStateOf<FeedbackEvent?>(null) }
    var seekAccumulation by remember { mutableIntStateOf(0) }
    var isControllerVisible by remember { mutableStateOf(true) }
    var isToolsExpanded by remember { mutableStateOf(false) }
    var currentVideoTitle by remember { mutableStateOf("Video Player") }
    
    val playlist by viewModel.playlist.collectAsState()
    val videoMetadata by viewModel.videoMetadata.collectAsState()
    
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
        val prefBright = viewModel.learningEngine.getPreferredBrightness()
        if (prefBright >= 0f) {
            activity?.window?.let { win ->
                val params = win.attributes
                params.screenBrightness = prefBright
                win.attributes = params
            }
        }
        
        val prefVol = viewModel.learningEngine.getPreferredVolume()
        if (prefVol >= 0f) {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (prefVol * maxVol).toInt(), 0)
        }

        onDispose {
            com.helpofai.videoplayer.MainActivity.isPlayerActive = false
        }
    }

    LaunchedEffect(abRepeatA, abRepeatB, isPlaying) {
        val a = abRepeatA
        val b = abRepeatB
        if (a != null && b != null && isPlaying) {
            while (true) {
                val pos = viewModel.videoPlayer.player.currentPosition
                if (pos >= b || pos < a) { // also seek if user manually seeks before A
                    viewModel.videoPlayer.player.seekTo(a)
                    currentPosition = a
                }
                kotlinx.coroutines.delay(100)
            }
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

    // Auto-hide controls
    LaunchedEffect(isControllerVisible, isPlaying, isControlsLocked, isToolsExpanded) {
        if (isControllerVisible && isPlaying && !isControlsLocked && !isToolsExpanded) {
            kotlinx.coroutines.delay(4000)
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
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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

    // Effect for long-press speed boost
    DisposableEffect(isLongPressing) {
        if (isLongPressing) {
            viewModel.videoPlayer.setPlaybackSpeed(2.0f)
        } else {
            viewModel.videoPlayer.setPlaybackSpeed(1.0f)
        }
        onDispose {
            viewModel.videoPlayer.setPlaybackSpeed(1.0f)
        }
    }

    // Auto-rotate and Play/Pause feedback
    DisposableEffect(viewModel.videoPlayer.player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val isVideoLandscape = videoSize.width > videoSize.height
                    isLandscape = isVideoLandscape
                    activity?.requestedOrientation = if (isVideoLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                }
            }
            
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                mediaMetadata.title?.let { currentVideoTitle = it.toString() }
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Player View
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = viewModel.videoPlayer.player
                    useController = false
                    keepScreenOn = true
                    subtitleView?.visibility = android.view.View.VISIBLE
                    // Set subtitle style to look professional
                    subtitleView?.setStyle(
                        androidx.media3.ui.CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                            android.graphics.Color.BLACK,
                            null
                        )
                    )
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale * if (isMirrored) -1f else 1f
                scaleY = scale * if (isFlipped) -1f else 1f
                translationX = offsetX
                translationY = offsetY
                this.rotationZ = rotationZ

                // AI Video Enhancer is selected via activeEnhancerProfile
                // Applying real-time ColorMatrix to SurfaceView requires a custom Media3 VideoSink 
                // or OpenGL shader, which is managed at the ExoPlayer level.
            }
        )
        
        // AI Video Enhancer Overlay (Soft UI Tint)
        val aiOverlayColor = when (activeEnhancerProfile) {
            "cinematic" -> Color(0x208B4513) // Warm cinematic tint
            "hdr_boost" -> Color(0x15FFFFFF) // Brightness lift
            "vivid" -> Color(0x1A0055FF) // Cool vivid tint
            "dark_detail" -> Color(0x25FFFFFF) // Shadow lift
            else -> Color.Transparent
        }
        
        if (aiOverlayColor != Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize().background(aiOverlayColor))
        }

        if (!isInPipMode) {
            if (!isControlsLocked) {
            // Gesture Overlay Layer (Double tap, Long press, Swipe, Zoom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val screenWidth = size.width
                                val centerWidth = screenWidth * 0.3f // Center 30%
                                val leftBound = (screenWidth - centerWidth) / 2
                                val rightBound = leftBound + centerWidth

                                if (offset.x < leftBound) {
                                    viewModel.videoPlayer.seekBack()
                                    if (seekAccumulation > 0) seekAccumulation = 0
                                    seekAccumulation -= 10
                                    feedbackEvent = FeedbackEvent(FeedbackType.SEEK, Icons.Default.FastRewind, "${seekAccumulation}s")
                                } else if (offset.x > rightBound) {
                                    viewModel.videoPlayer.seekForward()
                                    if (seekAccumulation < 0) seekAccumulation = 0
                                    seekAccumulation += 10
                                    feedbackEvent = FeedbackEvent(FeedbackType.SEEK, Icons.Default.FastForward, "+${seekAccumulation}s")
                                } else {
                                    // Center double tap -> toggle play/pause
                                    if (isPlaying) {
                                        viewModel.videoPlayer.pause()
                                        showAdPopup = true
                                    } else {
                                        viewModel.videoPlayer.play()
                                        showAdPopup = false
                                    }
                                }
                            },
                            onTap = {
                                if (isToolsExpanded) {
                                    isToolsExpanded = false
                                } else {
                                    isControllerVisible = !isControllerVisible
                                }
                            },
                            onPress = {
                                tryAwaitRelease()
                                isLongPressing = false
                            },
                            onLongPress = {
                                isLongPressing = true
                                feedbackEvent = FeedbackEvent(FeedbackType.SPEED, Icons.Default.Speed, "2.0x Speed", color = Color(0xFFBB86FC))
                            }
                        )
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
                                            scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                                            if (scale > 1f) {
                                                val maxX = (size.width * (scale - 1)) / 2
                                                val maxY = (size.height * (scale - 1)) / 2
                                                offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                                                offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
                                            } else {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                        }
                                    } else {
                                        pastTouchSlop = false
                                        zoom = 1f
                                    }
                                }
                            } while (!canceled && event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(Unit) {
                        // Removed (scale == 1f) to allow gesture controls always
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
                                seekAccumulator = 0f
                                startPosition = viewModel.videoPlayer.player.currentPosition
                                isSeeking = false
                                isAdjustingVolBright = false
                                accumulatedY = 0f
                                lastBrightness = -1f
                                lastVolume = -1f
                                
                                val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                initialVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxVol
                                
                                activity?.window?.let { window ->
                                    var currentBrightness = window.attributes.screenBrightness
                                    if (currentBrightness < 0) currentBrightness = 0.5f
                                    initialBrightness = currentBrightness
                                }
                            },
                            onDragEnd = {
                                if (isSeeking) {
                                    val screenWidth = size.width
                                    val seekAmountMs = (seekAccumulator / screenWidth) * 120000
                                    val newPos = (startPosition + seekAmountMs.toLong()).coerceIn(0, viewModel.videoPlayer.player.duration)
                                    viewModel.videoPlayer.player.seekTo(newPos)
                                } else if (isAdjustingVolBright) {
                                    if (lastBrightness >= 0f) viewModel.learningEngine.learnBrightness(lastBrightness)
                                    if (lastVolume >= 0f) viewModel.learningEngine.learnVolume(lastVolume)
                                }
                                isSeeking = false
                                isAdjustingVolBright = false
                            },
                            onDragCancel = {
                                isSeeking = false
                                isAdjustingVolBright = false
                            }
                        ) { change, dragAmount ->
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
                                feedbackEvent = FeedbackEvent(FeedbackType.SEEK, Icons.AutoMirrored.Filled.CompareArrows, String.format("%02d:%02d (%s%ds)", m, s, sign, diffSecs))
                            } else if (isAdjustingVolBright) {
                                accumulatedY += dragAmount.y
                                // A full screen height swipe changes the value by 150% (so 2/3 screen = 100%)
                                val delta = -accumulatedY / screenHeight * 1.5f 
                                
                                if (isLeftSide) {
                                    // Brightness
                                    activity?.window?.let { window ->
                                        val params = window.attributes
                                        val newB = (initialBrightness + delta).coerceIn(0f, 1f)
                                        params.screenBrightness = newB
                                        window.attributes = params
                                        lastBrightness = newB
                                        
                                        feedbackEvent = FeedbackEvent(FeedbackType.BRIGHTNESS, Icons.Default.BrightnessMedium, "${(params.screenBrightness * 100).toInt()}%", value = params.screenBrightness, color = Color(0xFFFFEB3B))
                                    }
                                } else {
                                    // Volume
                                    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                    val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                    val newVolFloat = initialVolume + delta
                                    val newVol = (newVolFloat * maxVol).toInt().coerceIn(0, maxVol)
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                                    lastVolume = newVolFloat.coerceIn(0f, 1f)
                                    
                                    feedbackEvent = FeedbackEvent(FeedbackType.VOLUME, Icons.AutoMirrored.Filled.VolumeUp, "${(newVol.toFloat() / maxVol * 100).toInt()}%", value = newVolFloat.coerceIn(0f, 1f), color = Color(0xFF2196F3))
                            }
                        }
                    }
                }
            )
        }

        if (!isControlsLocked) {
            // Modern MX Player Inspired Top Toolbar
            PlayerTopToolbar(
                isVisible = isControllerVisible,
                title = currentVideoTitle,
                onBackClick = { activity?.finish() },
                onLockClick = { isControlsLocked = true },
                onSpeedClick = { showSpeedDial = true },
                onEqClick = { showEqualizer = true },
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
                    trackSelectorInitialTab = 0
                    showTrackSelector = true
                },
                onSubtitlesClick = {
                    trackSelectorInitialTab = 1
                    showTrackSelector = true
                },
                onScreenshotClick = {
                    viewModel.takeScreenshot(context) { path ->
                        feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.PhotoCamera, "Screenshot Saved")
                    }
                },
                onInfoClick = { showInfoDialog = true },
                onRotateClick = {
                    isLandscape = !isLandscape
                    activity?.requestedOrientation = if (isLandscape) {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                },
                onVideoEnhancerClick = { showVideoEnhancer = true },
                onVideoAdjustmentsClick = { showVideoAdjustments = true },
                onMoreClick = { showMorePopup = true },
                isToolsExpanded = isToolsExpanded,
                onToolsExpandedChange = { isToolsExpanded = it },
                modifier = Modifier.align(Alignment.TopCenter)
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

        // Lock Controls Button
        IconButton(
            onClick = { isControlsLocked = !isControlsLocked },
            modifier = Modifier
                .align(Alignment.CenterStart)
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

        if (showTrackSelector) {
            TrackSelectorBottomSheet(
                player = viewModel.videoPlayer.player,
                initialTab = trackSelectorInitialTab,
                onDismissRequest = { showTrackSelector = false },
                onLoadExternalSubtitle = {
                    subtitlePickerLauncher.launch("*/*")
                }
            )
        }

        // Decoder Selector
        if (showDecoderSelector) {
            DecoderSelectorSheet(
                currentDecoder = decoderMode,
                onDecoderSelect = { mode ->
                    decoderMode = mode
                    // TODO: Implement actual decoder switching (e.g. exo_player track flags for SW vs HW)
                    feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Memory, "Decoder set to $mode")
                },
                onDismissRequest = { showDecoderSelector = false }
            )
        }

        // Video Info Dialog
        if (showInfoDialog) {
            VideoInfoDialog(
                player = viewModel.videoPlayer.player,
                videoPath = viewModel.currentVideoPath,
                onDismissRequest = { showInfoDialog = false }
            )
        }

        // Audio Equalizer Sheet
        if (showEqualizer) {
            AudioEqualizerSheet(
                audioEffectManager = viewModel.audioEffectManager,
                onDismissRequest = { showEqualizer = false }
            )
        }

        // Speed Dial Sheet
        if (showSpeedDial) {
            PlaybackSpeedSheet(
                currentSpeed = viewModel.videoPlayer.player.playbackParameters.speed,
                onSpeedSelected = { spd ->
                    viewModel.videoPlayer.player.setPlaybackSpeed(spd)
                    viewModel.learningEngine.learnPlaybackSpeed(spd)
                },
                onDismissRequest = { showSpeedDial = false }
            )
        }
        
        // Video Enhancer Sheet
        if (showVideoEnhancer) {
            VideoEnhancerSheet(
                videoTitle = currentVideoTitle,
                currentProfileId = activeEnhancerProfile,
                onProfileSelect = { profileId ->
                    activeEnhancerProfile = profileId
                    feedbackEvent = FeedbackEvent(
                        type = FeedbackType.INFO,
                        icon = Icons.Default.AutoFixHigh,
                        text = "AI Profile Applied"
                    )
                },
                onDismissRequest = { showVideoEnhancer = false }
            )
        }
        
        // Video Adjustments Sheet
        if (showVideoAdjustments) {
            var currentBrightness by remember { 
                mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f) 
            }
            VideoAdjustmentsSheet(
                currentResizeMode = resizeMode,
                onResizeModeSelected = { 
                    resizeMode = it 
                    viewModel.learningEngine.learnAspectRatio(it.toString())
                },
                currentBrightness = currentBrightness,
                onBrightnessChanged = { newBrightness ->
                    currentBrightness = newBrightness
                    activity?.window?.let { window ->
                        val params = window.attributes
                        params.screenBrightness = newBrightness
                        window.attributes = params
                    }
                    viewModel.learningEngine.learnBrightness(newBrightness)
                },
                isMirrored = isMirrored,
                onMirrorToggled = { 
                    isMirrored = it 
                    feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Flip, if (it) "Mirror On" else "Mirror Off")
                },
                isFlipped = isFlipped,
                onFlipToggled = { 
                    isFlipped = it 
                    feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.Flip, if (it) "Flip On" else "Flip Off")
                },
                rotationZ = rotationZ,
                onRotationChanged = { 
                    rotationZ = it 
                    feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.AutoMirrored.Filled.RotateRight, "${(it % 360).toInt()}°")
                },
                onDismissRequest = { showVideoAdjustments = false }
            )
        }

        if (!isControlsLocked) {
            PlayerBottomController(
                isVisible = isControllerVisible,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                isLandscape = isLandscape,
                bookmarks = bookmarks.map { it.timeMs },
                lastPlayedPosition = videoMetadata?.lastPlayedPosition,
                onPlayPauseClick = {
                    if (isPlaying) viewModel.videoPlayer.pause() else viewModel.videoPlayer.play()
                },
                onSeek = { pos -> 
                    viewModel.videoPlayer.player.seekTo(pos)
                    currentPosition = pos
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Quality Analyzer Sheet
        val qualityReport by viewModel.qualityReport.collectAsState()
        val isAnalyzingQuality by viewModel.isAnalyzingQuality.collectAsState()

        if (showQualitySheet || isAnalyzingQuality || qualityReport != null) {
            com.helpofai.videoplayer.feature.qualityanalyzer.components.QualityReportSheet(
                report = qualityReport,
                isAnalyzing = isAnalyzingQuality,
                onDismissRequest = { 
                    showQualitySheet = false
                    viewModel.clearQualityReport()
                }
            )
        }

        // More Popup (Video List & Bookmarks)
        if (showMorePopup) {
            PlayerMorePopup(
                videos = playlist,
                currentVideoPath = viewModel.currentVideoPath,
                onVideoSelect = { path ->
                    viewModel.playVideo(path)
                    showMorePopup = false
                },
                onReorderPlaylist = { from, to ->
                    viewModel.reorderPlaylist(from, to)
                },
                onBookmarksClick = {
                    showMorePopup = false
                    showBookmarksSheet = true
                },
                onQualityAnalyzerClick = {
                    showMorePopup = false
                    showQualitySheet = true
                    viewModel.analyzeVideoQuality()
                },
                onDismissRequest = { showMorePopup = false }
            )
        }
        
        // Ad Popup when paused via center double-tap
        if (showAdPopup && !isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { 
                        viewModel.videoPlayer.play()
                        showAdPopup = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Paused",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Show Native Ad
                    com.helpofai.videoplayer.core.ads.NativeAdCard(
                        modifier = Modifier.width(320.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    IconButton(
                        onClick = { 
                            viewModel.videoPlayer.play()
                            showAdPopup = false 
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow, 
                            contentDescription = "Resume", 
                            tint = Color.White, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Bookmarks & Chapters Sheet
        if (showBookmarksSheet) {
            com.helpofai.videoplayer.feature.scenedetection.components.SceneSelectionSheet(
                videoPath = viewModel.currentVideoPath,
                bookmarks = bookmarks,
                onSeekTo = { pos -> 
                    viewModel.videoPlayer.player.seekTo(pos)
                    currentPosition = pos
                },
                onGenerateAutoChapters = {
                    viewModel.generateAutoChapters(
                        onStart = { isGeneratingChapters = true },
                        onComplete = { success ->
                            isGeneratingChapters = false
                            if (success) {
                                feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.AutoAwesome, "Scenes Detected")
                            } else {
                                feedbackEvent = FeedbackEvent(FeedbackType.INFO, Icons.Default.AutoAwesome, "No Scenes Found")
                            }
                        }
                    )
                },
                isGeneratingChapters = isGeneratingChapters,
                onDismissRequest = { showBookmarksSheet = false }
            )
        }
        
        if (showSubtitlesSheet) {
            TrackSelectorBottomSheet(
                player = viewModel.videoPlayer.player,
                initialTab = 1,
                onDismissRequest = { showSubtitlesSheet = false },
                onLoadExternalSubtitle = {
                    subtitlePickerLauncher.launch("*/*")
                }
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
