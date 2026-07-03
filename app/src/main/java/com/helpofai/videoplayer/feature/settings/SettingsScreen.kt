package com.helpofai.videoplayer.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Close
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpofai.videoplayer.core.data.PrivacyRepository
import com.helpofai.videoplayer.feature.settings.components.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import coil.imageLoader

@OptIn(ExperimentalMaterial3Api::class, coil.annotation.ExperimentalCoilApi::class)
@Suppress("DEPRECATION")
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val privacyRepository = remember { PrivacyRepository(context) }
    var isLocked by remember { mutableStateOf(privacyRepository.isLockEnabled()) }

    val hardwareAccelEnabled by viewModel.hardwareAcceleration.collectAsState()
    val defaultSpeed by viewModel.defaultPlaybackSpeed.collectAsState()
    val defaultSubtitle by viewModel.defaultSubtitleLanguage.collectAsState()
    
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    
    var autoPlayEnabled by remember { mutableStateOf(true) }
    var subtitleEngine by remember { mutableStateOf("Advanced Auto-Detect") }
    
    var selectedHtmlFile by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier.blur(if (selectedHtmlFile != null || showSpeedDialog || showSubtitleDialog) 16.dp else 0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(96.dp)) {
                        @OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
                        val image = androidx.compose.animation.graphics.vector.AnimatedImageVector.animatedVectorResource(id = com.helpofai.videoplayer.R.drawable.ic_logo_animated)
                        var atEnd by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { atEnd = true }
                        
                        @OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(image, atEnd = atEnd),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "HOA Video Player",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Advanced Version 1.1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsCategoryCard("Playback") {
                    SettingsSwitchItem(
                        title = "Hardware Acceleration",
                        subtitle = "Use device GPU for smoother playback.",
                        checked = hardwareAccelEnabled,
                        onCheckedChange = { viewModel.setHardwareAcceleration(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    val bgPlaybackEnabled by viewModel.backgroundPlayback.collectAsState()
                    SettingsSwitchItem(
                        title = "Background Playback",
                        subtitle = "Continue playing audio when in background.",
                        checked = bgPlaybackEnabled,
                        onCheckedChange = { viewModel.setBackgroundPlayback(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        title = "Default Playback Speed",
                        subtitle = "${defaultSpeed}x",
                        onClick = { showSpeedDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchItem(
                        title = "Auto-Play Next Video",
                        subtitle = "Play the next video automatically.",
                        checked = autoPlayEnabled,
                        onCheckedChange = { autoPlayEnabled = it }
                    )
                }
            }

            item {
                SettingsCategoryCard("Appearance") {
                    val dynamicColorsEnabled by viewModel.dynamicColors.collectAsState()
                    SettingsSwitchItem(
                        title = "Dynamic Colors (Material You)",
                        subtitle = "Extract colors from your wallpaper.",
                        checked = dynamicColorsEnabled,
                        onCheckedChange = { viewModel.setDynamicColors(it) }
                    )
                }
            }

            item {
                SettingsCategoryCard("Subtitles") {
                    SettingsItem(
                        title = "Subtitle Engine",
                        subtitle = subtitleEngine,
                        onClick = {
                            subtitleEngine = if (subtitleEngine == "Advanced Auto-Detect") "ExoPlayer Default" else "Advanced Auto-Detect"
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        title = "Default Language",
                        subtitle = defaultSubtitle,
                        onClick = { showSubtitleDialog = true }
                    )
                }
            }

            item {
                SettingsCategoryCard("Privacy & Security") {
                    SettingsSwitchItem(
                        title = "App Privacy Lock",
                        subtitle = "Require a PIN to open the app.",
                        checked = isLocked,
                        onCheckedChange = { 
                            if (it) {
                                privacyRepository.setPin("1234")
                            } else {
                                privacyRepository.removePin()
                            }
                            isLocked = it
                        }
                    )
                }
            }

            item {
                SettingsCategoryCard("Data Management") {
                    SettingsItem(
                        title = "Clear Watch History",
                        subtitle = "Remove all resume positions.",
                        onClick = { viewModel.clearWatchHistory() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        title = "Clear Cache",
                        subtitle = "Free up thumbnail storage space.",
                        onClick = { 
                            context.imageLoader.diskCache?.clear()
                            context.imageLoader.memoryCache?.clear()
                            java.io.File(context.cacheDir, "smart_thumbnails").deleteRecursively()
                        }
                    )
                }
            }
            
            item {
                SettingsCategoryCard("About & Legal") {
                    SettingsItemWithIcon(
                        title = "Changelog",
                        subtitle = "See what's new in this version",
                        icon = Icons.Default.History,
                        onClick = { selectedHtmlFile = "changelog.html" }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItemWithIcon(
                        title = "Privacy Policy",
                        subtitle = "Read our privacy guidelines",
                        icon = Icons.Default.Policy,
                        onClick = { selectedHtmlFile = "privacy.html" }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = Color(0x66000000), // 40% Opaque Glassmorphism
            title = { Text("Playback Speed", color = Color.White) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${speed}x", modifier = Modifier.weight(1f))
                            if (defaultSpeed == speed) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showSubtitleDialog) {
        val languages = listOf("Off", "en", "es", "fr", "de", "hi", "zh")
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            containerColor = Color(0x66000000), // 40% Opaque Glassmorphism
            title = { Text("Default Subtitle Language", color = Color.White) },
            text = {
                Column {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSubtitleLanguage(lang)
                                    showSubtitleDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (lang == "Off") "Off" else lang.uppercase(), modifier = Modifier.weight(1f))
                            if (defaultSubtitle == lang) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    if (selectedHtmlFile != null) {
        SettingsHtmlDialog(
            fileName = selectedHtmlFile!!,
            onDismissRequest = { selectedHtmlFile = null }
        )
    }
}
