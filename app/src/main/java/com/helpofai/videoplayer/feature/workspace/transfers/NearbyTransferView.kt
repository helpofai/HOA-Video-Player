package com.helpofai.videoplayer.feature.workspace.transfers

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.helpofai.videoplayer.core.model.Video

@Composable
fun NearbyTransferView(
    videos: List<Video>,
    onStartSimulatedTransfer: (Video) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSendSelector by remember { mutableStateOf(false) }
    var selectedVideoForShare by remember { mutableStateOf<Video?>(null) }
    var showReceiverScanner by remember { mutableStateOf(false) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showReceiverScanner = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nearby Sharing Portal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Ultrafast local peer-to-peer Wi-Fi network engine (up to 80 MB/s)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TransferActionCircle(
                label = "Send File",
                icon = Icons.Default.Share,
                backgroundColor = MaterialTheme.colorScheme.primary,
                onClick = { showSendSelector = true }
            )
            TransferActionCircle(
                label = "Receive File",
                icon = Icons.Default.LocationOn,
                backgroundColor = Color.Cyan,
                onClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        showReceiverScanner = true
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E222B).copy(alpha = 0.5f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Active Senders Nearby", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("OPPO Reno8 Pro", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text("Wi-Fi Direct ready", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "Connected to OPPO Reno8 Pro", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                    ) {
                        Text("Connect", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    
    // Sender File Picker
    if (showSendSelector) {
        VideoSelectorDialog(
            videos = videos,
            onDismiss = { showSendSelector = false },
            onSelect = { video ->
                showSendSelector = false
                selectedVideoForShare = video
            }
        )
    }
    
    // Sender QR Share Dialog
    selectedVideoForShare?.let { video ->
        SenderQrDialog(
            video = video,
            onDismiss = { selectedVideoForShare = null },
            onStartTransfer = {
                selectedVideoForShare = null
                onStartSimulatedTransfer(video)
            }
        )
    }
    
    // Receiver Scan Camera Viewfinder Dialog
    if (showReceiverScanner) {
        ReceiverScannerDialog(
            onDismiss = { showReceiverScanner = false },
            onScanSuccess = {
                showReceiverScanner = false
                if (videos.isNotEmpty()) {
                    onStartSimulatedTransfer(videos.first())
                    Toast.makeText(context, "Connected! File transfer started.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Connected! Waiting for sender payload...", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun TransferActionCircle(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(90.dp)
                .clickable { onClick() },
            shape = CircleShape,
            color = backgroundColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, backgroundColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = backgroundColor, modifier = Modifier.size(36.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun VideoSelectorDialog(
    videos: List<Video>,
    onDismiss: () -> Unit,
    onSelect: (Video) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Video to Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
                
                if (videos.isEmpty()) {
                    Text("No local media found", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                } else {
                    videos.forEach { video ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(video) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.03f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(video.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                    Text(video.path, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SenderQrDialog(
    video: Video,
    onDismiss: () -> Unit,
    onStartTransfer: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sender Share Portal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
                
                Text(
                    text = "Ready to share: ${video.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                
                // Big QR Code representation
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Sender QR",
                    tint = Color.White,
                    modifier = Modifier.size(160.dp)
                )
                
                Text(
                    text = "Instruction: Open the Receiver Scanner on the other device and scan this QR code to initiate the transfer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onStartTransfer,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Sharing Session", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiverScannerDialog(
    onDismiss: () -> Unit,
    onScanSuccess: () -> Unit
) {
    // Scanner moving laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffset"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Receiver Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
                
                Text(
                    text = "Align the Sender QR code inside the viewfinder frame",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                
                // Viewfinder scanning simulator
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                    
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner",
                        tint = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                    
                    // Moving laser line
                    Box(
                        modifier = Modifier
                            .offset(y = laserOffsetY.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.Green)
                    )
                }
                
                Button(
                    onClick = onScanSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate Successful Scan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    var isCameraActive by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                android.view.SurfaceView(ctx).apply {
                    holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        var camera: android.hardware.Camera? = null
                        
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            try {
                                camera = android.hardware.Camera.open()
                                camera?.setPreviewDisplay(holder)
                                camera?.setDisplayOrientation(90)
                                camera?.startPreview()
                                isCameraActive = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isCameraActive = false
                            }
                        }
                        
                        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                        
                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                            try {
                                camera?.stopPreview()
                                camera?.release()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (!isCameraActive) {
            CameraFallbackView(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CameraFallbackView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier.background(Color(0xFF07090C)),
        contentAlignment = Alignment.Center
    ) {
        // Grid crosshair graphics
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = alpha)), RoundedCornerShape(10.dp))
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.Cyan.copy(alpha = alpha + 0.3f),
                modifier = Modifier.size(36.dp)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pulsing Green LED indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Green.copy(alpha = alpha + 0.5f))
                )
                Text(
                    text = "Live Viewfinder Scanner",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            }
        }
    }
}
