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
package com.helpofai.videoplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.helpofai.videoplayer.core.ads.AdManager
import com.helpofai.videoplayer.core.data.PrivacyRepository
import com.helpofai.videoplayer.core.data.SettingsRepository
import com.helpofai.videoplayer.core.theme.VideoPlayerTheme
import com.helpofai.videoplayer.feature.library.HomeScreen
import com.helpofai.videoplayer.feature.permissions.PermissionScreen
import com.helpofai.videoplayer.feature.permissions.hasRequiredPermissions
import com.helpofai.videoplayer.feature.player.PlayerScreen
import com.helpofai.videoplayer.feature.privacy.PinScreen
import com.helpofai.videoplayer.feature.settings.SettingsScreen
import com.helpofai.videoplayer.feature.splash.AnimatedSplashScreen
import com.helpofai.videoplayer.feature.watch_party.notification.WatchPartyNotificationOverlay
import com.helpofai.videoplayer.feature.watch_party.ui.WatchPartyJoinRequestOverlay
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var privacyRepository: PrivacyRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var videoPlayer: com.helpofai.videoplayer.core.playback.VideoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        enableEdgeToEdge()
        
        installSplashScreen()
        
        // For testing purposes, we could set a PIN here if we wanted to
        // privacyRepository.setPin("1234")
        
        val startDestination = "splash"
        setContent {
            val useDynamicColors by settingsRepository.dynamicColors.collectAsState(initial = true)
            
            VideoPlayerTheme(dynamicColor = useDynamicColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Premium Glassmorphism Background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF0F0C29), 
                                            Color(0xFF302B63), 
                                            Color(0xFF0F0C29)
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(300.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x406C5CE7), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(350.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x4000CEC9), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(250.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x40FD79A8), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )

                        val miniPlayerManager = com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance()
                        val isInPipMode by miniPlayerManager.isInPipMode.collectAsState()
                        val navController = rememberNavController()

                        if (!isInPipMode || isPlayerActive) {
                            NavHost(navController = navController, startDestination = startDestination) {
                                composable("splash") {
                                    AnimatedSplashScreen(
                                        onSplashFinished = {
                                            val nextRoute = if (privacyRepository.isLockEnabled()) {
                                                "pin"
                                            } else if (!hasRequiredPermissions(this@MainActivity)) {
                                                "permissions"
                                            } else {
                                                "home"
                                            }
                                            navController.navigate(nextRoute) {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("pin") {
                                    PinScreen(
                                        privacyRepository = privacyRepository,
                                        onSuccess = {
                                            val nextRoute = if (!hasRequiredPermissions(this@MainActivity)) "permissions" else "home"
                                            navController.navigate(nextRoute) {
                                                popUpTo("pin") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("permissions") {
                                    PermissionScreen(
                                        onPermissionsGranted = {
                                            navController.navigate("home") {
                                                popUpTo("permissions") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("home") {
                                    HomeScreen(
                                        onVideoClick = { video ->
                                            val encodedUri  = Uri.encode(video.uri.toString())
                                            val encodedPath = Uri.encode(video.path)
                                            navController.navigate("player/$encodedUri?path=$encodedPath")
                                        },
                                        onSettingsClick = {
                                            navController.navigate("settings")
                                        }
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "player/{videoUri}?path={path}",
                                    arguments = listOf(
                                        navArgument("videoUri") { type = NavType.StringType },
                                        navArgument("path") { 
                                            type = NavType.StringType 
                                            nullable = true
                                        }
                                    )
                                ) {
                                    PlayerScreen(
                                        onNavigateBack = {
                                            // Disable PiP before showing ad because launching the Ad Activity triggers onUserLeaveHint
                                            isPlayerActive = false
                                            AdManager.showInterstitialAd(this@MainActivity) {
                                                navController.popBackStack()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (!isInPipMode) {
                            // Global Watch Party join request overlay — renders on ANY page
                            WatchPartyJoinRequestOverlay()

                            // Global Watch Party notification toast overlay — renders on ANY page
                            WatchPartyNotificationOverlay()
                        }

                        // Global Mini Player overlay — renders on ANY page
                        com.helpofai.videoplayer.core.playback.GlobalMiniPlayer(
                            videoPlayer = videoPlayer,
                            onRestore = { video ->
                                val encodedUri  = Uri.encode(video.uri.toString())
                                val encodedPath = Uri.encode(video.path)
                                // Navigate back to player, dropping active mini player state
                                com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance().dismissMiniPlayer()
                                navController.navigate("player/$encodedUri?path=$encodedPath") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        // Client watch party auto-start silent preview listener
                        val sessionManager = com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance()
                        val activeSession by sessionManager.activeSession.collectAsState()
                        val isClientMode by sessionManager.isClientModeFlow.collectAsState()
                        val isFullPlayerActive by sessionManager.isFullPlayerActive.collectAsState()

                        class HostStreamingState {
                            var lastVideoPath: String? = null
                            var lastIsPlaying: Boolean = false
                        }
                        val hostState = androidx.compose.runtime.remember { HostStreamingState() }

                        androidx.compose.runtime.LaunchedEffect(activeSession, isClientMode, isFullPlayerActive) {
                            val session = activeSession
                            if (isClientMode && session != null) {
                                val hostVideo = session.video
                                val isPlaying = session.isPlaying
                                val videoPath = hostVideo?.path

                                val streamingStarted = (isPlaying && !hostState.lastIsPlaying) ||
                                                       (videoPath != null && videoPath != hostState.lastVideoPath && isPlaying)

                                hostState.lastIsPlaying = isPlaying
                                hostState.lastVideoPath = videoPath

                                if (hostVideo != null && isPlaying) {
                                    if (!isFullPlayerActive && streamingStarted) {
                                        val miniPlayerManager = com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance()
                                        if (!miniPlayerManager.isMiniPlayerActive.value) {
                                            val streamPort = session.port
                                            val hostIp = if (session.hostIp.isNotBlank()) session.hostIp else "127.0.0.1"
                                            val videoId = hostVideo.id
                                            val streamUri = Uri.parse("http://$hostIp:$streamPort/video?v=$videoId&t=${System.currentTimeMillis()}")
                                            val clientVideo = hostVideo.copy(
                                                uri = streamUri,
                                                path = "http_stream"
                                            )
                                            
                                            // Prepare and play the stream silently
                                            videoPlayer.player.volume = 0f
                                            videoPlayer.prepare(androidx.media3.common.MediaItem.fromUri(streamUri))
                                            videoPlayer.play()
                                            
                                            miniPlayerManager.showMiniPlayer(clientVideo)
                                        }
                                    }
                                } else {
                                    val miniPlayerManager = com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance()
                                    if (miniPlayerManager.isMiniPlayerActive.value && !isFullPlayerActive) {
                                        miniPlayerManager.dismissMiniPlayer()
                                        videoPlayer.pause()
                                    }
                                }
                            } else {
                                hostState.lastIsPlaying = false
                                hostState.lastVideoPath = null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        intent?.data?.let { handleDeepLink(it) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(uri: Uri) {
        if (uri.scheme == "vidplay" && uri.host == "join") {
            com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance()
                .pendingDeepLink.value = uri.toString()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val miniPlayerManager = com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
            (isPlayerActive || miniPlayerManager.isMiniPlayerActive.value)) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance()
            .setInPipMode(isInPictureInPictureMode)
    }
    
    companion object {
        var isPlayerActive = false
    }
}
