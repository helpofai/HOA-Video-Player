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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SectionHeader("Playback")
                SwitchSettingItem(
                    title = "Hardware Acceleration",
                    subtitle = "Use device GPU for smoother playback. Disable if experiencing glitches.",
                    checked = hardwareAccelEnabled,
                    onCheckedChange = { viewModel.setHardwareAcceleration(it) }
                )
                HorizontalDivider()
                val bgPlaybackEnabled by viewModel.backgroundPlayback.collectAsState()
                SwitchSettingItem(
                    title = "Background Playback",
                    subtitle = "Continue playing audio when app is in background.",
                    checked = bgPlaybackEnabled,
                    onCheckedChange = { viewModel.setBackgroundPlayback(it) }
                )
                HorizontalDivider()
                SettingItem(
                    title = "Default Playback Speed",
                    subtitle = "${defaultSpeed}x",
                    onClick = { showSpeedDialog = true }
                )
                HorizontalDivider()
                SwitchSettingItem(
                    title = "Auto-Play Next Video",
                    subtitle = "Automatically play the next video in the folder when the current one finishes.",
                    checked = autoPlayEnabled,
                    onCheckedChange = { autoPlayEnabled = it }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SectionHeader("Appearance")
                val dynamicColorsEnabled by viewModel.dynamicColors.collectAsState()
                SwitchSettingItem(
                    title = "Dynamic Colors (Material You)",
                    subtitle = "Extract colors from your wallpaper to theme the app. (Requires Android 12+)",
                    checked = dynamicColorsEnabled,
                    onCheckedChange = { viewModel.setDynamicColors(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SectionHeader("Subtitles")
                SettingItem(
                    title = "Subtitle Engine",
                    subtitle = subtitleEngine,
                    onClick = {
                        subtitleEngine = if (subtitleEngine == "Advanced Auto-Detect") "ExoPlayer Default" else "Advanced Auto-Detect"
                    }
                )
                HorizontalDivider()
                SettingItem(
                    title = "Default Subtitle Language",
                    subtitle = defaultSubtitle,
                    onClick = { showSubtitleDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SectionHeader("Privacy & Security")
                SwitchSettingItem(
                    title = "App Privacy Lock",
                    subtitle = "Require a PIN (1234) to open the app.",
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

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SectionHeader("Data")
                SettingItem(
                    title = "Clear Watch History",
                    subtitle = "Remove resume positions for all videos.",
                    onClick = { viewModel.clearWatchHistory() }
                )
                HorizontalDivider()
                SettingItem(
                    title = "Clear Cache",
                    subtitle = "Free up space used by video thumbnails.",
                    onClick = { 
                        context.imageLoader.diskCache?.clear()
                        context.imageLoader.memoryCache?.clear()
                        java.io.File(context.cacheDir, "smart_thumbnails").deleteRecursively()
                    }
                )
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item {
                SectionHeader("About & Legal")
                SettingItemWithIcon(
                    title = "Changelog",
                    subtitle = "See what's new in this version",
                    icon = Icons.Default.History,
                    onClick = { selectedHtmlFile = "changelog.html" }
                )
                HorizontalDivider()
                SettingItemWithIcon(
                    title = "Privacy Policy",
                    subtitle = "Read our privacy guidelines",
                    icon = Icons.Default.Policy,
                    onClick = { selectedHtmlFile = "privacy.html" }
                )
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item {
                SectionHeader("App Information")
                SettingItemWithIcon(
                    title = "HOA Video Player Advanced",
                    subtitle = "Version 1.1.0 (Build 2026.06.30)",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = Color(0xCC1E293B), // 80% Transparent
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
            containerColor = Color(0xCC1E293B), // 80% Transparent
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
        HtmlBottomSheet(
            fileName = selectedHtmlFile!!,
            onDismissRequest = { selectedHtmlFile = null }
        )
    }
}

@Composable
fun HtmlBottomSheet(fileName: String, onDismissRequest: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close", 
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            loadUrl("file:///android_asset/$fileName")
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingItemWithIcon(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
