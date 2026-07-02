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
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import com.helpofai.videoplayer.core.theme.VideoPlayerTheme

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
                    color = MaterialTheme.colorScheme.background
                ) {
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
                            HomeScreen(
                                onVideoClick = { video ->
                                    val encodedUri = Uri.encode(video.uri.toString())
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
                            PlayerScreen()
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
