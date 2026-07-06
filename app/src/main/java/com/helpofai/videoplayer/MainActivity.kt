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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import com.helpofai.videoplayer.core.theme.VideoPlayerTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.helpofai.videoplayer.feature.library.HomeScreen
import com.helpofai.videoplayer.feature.player.PlayerScreen
import com.helpofai.videoplayer.feature.settings.SettingsScreen

import com.helpofai.videoplayer.core.data.PrivacyRepository
import com.helpofai.videoplayer.feature.privacy.PinScreen
import com.helpofai.videoplayer.feature.permissions.PermissionScreen
import com.helpofai.videoplayer.feature.permissions.hasRequiredPermissions
import com.helpofai.videoplayer.feature.splash.AnimatedSplashScreen
import com.helpofai.videoplayer.core.ads.AdManager
import com.helpofai.videoplayer.core.ads.InterstitialAdTrigger
import com.helpofai.videoplayer.core.model.Video
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import javax.inject.Inject

import com.helpofai.videoplayer.core.data.SettingsRepository

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var privacyRepository: PrivacyRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

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

                        val navController = rememberNavController()
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
                            // Interstitial trigger state — set to the clicked video to trigger the ad.
                            var pendingVideo by remember { mutableStateOf<Video?>(null) }

                            // InterstitialAdTrigger observes pendingVideo and shows the ad
                            // (with frequency capping) then navigates when done.
                            InterstitialAdTrigger(
                                trigger    = pendingVideo != null,
                                onComplete = {
                                    val video = pendingVideo
                                    if (video != null) {
                                        val encodedUri  = Uri.encode(video.uri.toString())
                                        val encodedPath = Uri.encode(video.path)
                                        navController.navigate("player/$encodedUri?path=$encodedPath")
                                    }
                                    pendingVideo = null
                                }
                            )

                            HomeScreen(
                                onVideoClick = { video ->
                                    pendingVideo = video
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
                            PlayerScreen()
                        }
                    }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && isPlayerActive) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
    
    companion object {
        var isPlayerActive = false
    }
}
