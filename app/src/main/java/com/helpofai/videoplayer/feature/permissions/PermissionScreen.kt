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
package com.helpofai.videoplayer.feature.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

fun hasRequiredPermissions(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var permissionsState by remember {
        mutableStateOf(
            permissionsToRequest.associateWith { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    fun updatePermissionsState() {
        val newState = permissionsToRequest.associateWith { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        permissionsState = newState
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        updatePermissionsState()
    }

    // Reactively update permission status on Resume (e.g., if user grants via Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePermissionsState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allGranted = permissionsState.values.all { it }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Premium Animated Icon Container
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color(0xFF00CEC9)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "HOA Video Player operates entirely offline and requires permission to discover and stream media files on your device.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Scrollable list of permissions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Video Access Item
                    val isVideoGranted = permissionsState[Manifest.permission.READ_MEDIA_VIDEO] == true
                    PermissionCard(
                        title = "Video Library Access",
                        icon = Icons.Default.Movie,
                        whyNeed = "To scan, index, and load local video files from your device.",
                        whatFor = "Populating the local video player library and smart folders.",
                        features = "Video Grid, Masonry Folder previews, Watch History, and resume-play.",
                        isGranted = isVideoGranted,
                        onClick = {
                            if (!isVideoGranted) {
                                launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
                            }
                        }
                    )

                    // Audio Access Item
                    val isAudioGranted = permissionsState[Manifest.permission.READ_MEDIA_AUDIO] == true
                    PermissionCard(
                        title = "Audio & Soundtrack Access",
                        icon = Icons.Default.MusicNote,
                        whyNeed = "To read video soundtracks, custom audio tracks, and linked media files.",
                        whatFor = "Enabling soundtrack switching and custom audio sync.",
                        features = "Dual Audio track switcher, custom external audio sync, and background audio.",
                        isGranted = isAudioGranted,
                        onClick = {
                            if (!isAudioGranted) {
                                launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
                            }
                        }
                    )

                    // Camera Access Item
                    val isCameraGranted = permissionsState[Manifest.permission.CAMERA] == true
                    PermissionCard(
                        title = "Camera Access",
                        icon = Icons.Default.CameraAlt,
                        whyNeed = "To scan QR codes and discover Watch Party rooms instantly.",
                        whatFor = "Scanning and pairing rooms via camera QR scanner.",
                        features = "Instant room join, quick pairing validation, and connection check.",
                        isGranted = isCameraGranted,
                        onClick = {
                            if (!isCameraGranted) {
                                launcher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        }
                    )

                    // Location Access Item
                    val isLocationGranted = permissionsState[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    PermissionCard(
                        title = "Location Discovery Access",
                        icon = Icons.Default.LocationOn,
                        whyNeed = "To scan nearby Wi-Fi networks and discover peer devices on local subnets.",
                        whatFor = "Discovering rooms and sending connection invites via local hotspot.",
                        features = "Local network device scan, connection speed monitor, and latency checks.",
                        isGranted = isLocationGranted,
                        onClick = {
                            if (!isLocationGranted) {
                                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        }
                    )

                    // Notifications Access Item
                    val isNotificationGranted = permissionsState[Manifest.permission.POST_NOTIFICATIONS] == true
                    PermissionCard(
                        title = "System Notification Access",
                        icon = Icons.Default.Notifications,
                        whyNeed = "To notify you of incoming Watch Party invitations and streaming session updates.",
                        whatFor = "Sending in-app and background system-tray room invitations.",
                        features = "Join requests alert, streaming health update, and foreground service controls.",
                        isGranted = isNotificationGranted,
                        onClick = {
                            if (!isNotificationGranted) {
                                launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            }
                        }
                    )
                } else {
                    // Storage Access Item (API 32 and below)
                    val isStorageGranted = permissionsState[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                    PermissionCard(
                        title = "Device Storage Access",
                        icon = Icons.Default.Folder,
                        whyNeed = "To scan and read video files on your device's external storage.",
                        whatFor = "Loading and playing video, audio, and subtitle files.",
                        features = "Video Library, Folder Preview masonry, history, and custom subtitle track loading.",
                        isGranted = isStorageGranted,
                        onClick = {
                            if (!isStorageGranted) {
                                launcher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                            }
                        }
                    )

                    // Camera Access Item
                    val isCameraGranted = permissionsState[Manifest.permission.CAMERA] == true
                    PermissionCard(
                        title = "Camera Access",
                        icon = Icons.Default.CameraAlt,
                        whyNeed = "To scan QR codes and discover Watch Party rooms instantly.",
                        whatFor = "Scanning and pairing rooms via camera QR scanner.",
                        features = "Instant room join, quick pairing validation, and connection check.",
                        isGranted = isCameraGranted,
                        onClick = {
                            if (!isCameraGranted) {
                                launcher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        }
                    )

                    // Location Access Item
                    val isLocationGranted = permissionsState[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    PermissionCard(
                        title = "Location Discovery Access",
                        icon = Icons.Default.LocationOn,
                        whyNeed = "To scan nearby Wi-Fi networks and discover peer devices on local subnets.",
                        whatFor = "Discovering rooms and sending connection invites via local hotspot.",
                        features = "Local network device scan, connection speed monitor, and latency checks.",
                        isGranted = isLocationGranted,
                        onClick = {
                            if (!isLocationGranted) {
                                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        }
                    )
                }
            }

            // Bottom Actions Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (allGranted) {
                            onPermissionsGranted()
                        } else {
                            launcher.launch(permissionsToRequest)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allGranted) Color(0xFF00CEC9) else Color(0xFF6C5CE7),
                        contentColor = if (allGranted) Color.Black else Color.White
                    )
                ) {
                    Text(
                        text = if (allGranted) "Continue to Player" else "Grant All Permissions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    icon: ImageVector,
    whyNeed: String,
    whatFor: String,
    features: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isGranted) Color(0x3300CEC9) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                Color(0x0A00CEC9)
            } else {
                Color.White.copy(alpha = 0.03f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) Color(0x1F00CEC9) else Color.White.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF00CEC9) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isGranted) "Permission Granted" else "Tap to Grant Access",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isGranted) Color(0xFF00CEC9) else Color.White.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Circular Check Tick Indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) Color(0xFF00CEC9) else Color.Transparent
                        )
                        .border(
                            1.5.dp,
                            if (isGranted) Color(0xFF00CEC9) else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Granted",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(10.dp))

            // Explanatory detail rows
            PermissionDetailRow(label = "Why Need", value = whyNeed)
            Spacer(modifier = Modifier.height(6.dp))
            PermissionDetailRow(label = "What For", value = whatFor)
            Spacer(modifier = Modifier.height(6.dp))
            PermissionDetailRow(label = "Features", value = features)
        }
    }
}

@Composable
fun PermissionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
    }
}
