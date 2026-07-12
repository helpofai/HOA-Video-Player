package com.helpofai.videoplayer.feature.nearbyshare

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.watch_party.qr.generator.WatchPartyQrGenerator
import com.helpofai.videoplayer.feature.watch_party.ui.qr_scanner.WatchPartyQrScannerScreen

// ─── Color Palette (Matches Watch Party premium theme) ───────────────────────
private val NearbyBgDeep    = Color(0xFF090B10)
private val NearbyBgCard    = Color(0xFF111520)
private val NearbyAccentC   = Color(0xFF00CEC9)
private val NearbyAccentG   = Color(0xFF00B894)
private val NearbyAccentP   = Color(0xFF7C5CE7)
private val NearbyTextPri   = Color(0xFFECF0F1)
private val NearbyTextSub   = Color(0xFF8E9CB0)
private val NearbyDivider   = Color(0xFF1E2535)

@Composable
fun NearbyShareScreen(
    videos: List<Video>,
    modifier: Modifier = Modifier,
    viewModel: NearbyShareViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val transferQueue by viewModel.transferQueue.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val qrCodeData by viewModel.qrCodeData.collectAsState()

    var showSendSelector by remember { mutableStateOf(false) }
    var showReceiverScanner by remember { mutableStateOf(false) }
    var selectedVideoForShare by remember { mutableStateOf<Video?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showReceiverScanner = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-start discovery when screen is opened
    DisposableEffect(Unit) {
        viewModel.startDiscovering()
        onDispose {
            viewModel.stopDiscovering()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NearbyBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Device Status Dashboard ──────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = NearbyBgCard,
            border = BorderStroke(1.dp, NearbyDivider)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NearbyAccentC.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = NearbyAccentC)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(deviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NearbyTextPri)
                        Text("IP: ${localIp ?: "Offline"}", style = MaterialTheme.typography.bodySmall, color = NearbyTextSub)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NearbyAccentG.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NearbyAccentG.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NearbyAccentG)
                        )
                        Text("Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NearbyAccentG)
                    }
                }
            }
        }

        // ── Action Hub ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NearbyActionTile(
                title = "Send Video",
                subtitle = "Share files peer-to-peer",
                icon = Icons.Default.FileUpload,
                gradient = Brush.linearGradient(listOf(NearbyAccentC, NearbyAccentP)),
                modifier = Modifier.weight(1f),
                onClick = { showSendSelector = true }
            )
            NearbyActionTile(
                title = "Receive Video",
                subtitle = "Scan QR or search peers",
                icon = Icons.Default.QrCodeScanner,
                gradient = Brush.linearGradient(listOf(NearbyAccentG, NearbyAccentC)),
                modifier = Modifier.weight(1f),
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

        // ── Active Transfer Queue ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = transferQueue.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Transfer Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NearbyTextPri
                )
                
                transferQueue.forEach { transfer ->
                    NearbyTransferCard(
                        transfer = transfer,
                        onPauseToggle = { viewModel.pauseTransfer(transfer) },
                        onResumeToggle = { viewModel.resumeTransfer(transfer) },
                        onCancel = { viewModel.cancelTransfer(transfer) }
                    )
                }
            }
        }

        // ── Discovered Devices Panel ──────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Senders Nearby",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NearbyTextPri
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = NearbyAccentC
                    )
                    Text("Scanning", fontSize = 10.sp, color = NearbyAccentC)
                }
            }

            if (discoveredDevices.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NearbyBgCard.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, NearbyDivider)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = NearbyTextSub, modifier = Modifier.size(32.dp))
                        Text(
                            text = "Looking for active transmitters on local network...",
                            style = MaterialTheme.typography.bodySmall,
                            color = NearbyTextSub,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                discoveredDevices.forEach { device ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NearbyBgCard,
                        border = BorderStroke(1.dp, NearbyDivider)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NearbyAccentC.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhoneAndroid, null, tint = NearbyAccentC, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(device.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NearbyTextPri)
                                    Text("IP: ${device.ipAddress} • Port: ${device.port}", style = MaterialTheme.typography.bodySmall, color = NearbyTextSub)
                                }
                            }
                            Button(
                                onClick = { viewModel.connectToPeer(device) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NearbyAccentC,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Video Selector ────────────────────────────────────────────────
    if (showSendSelector) {
        NearbyVideoSelectorDialog(
            videos = videos,
            onDismiss = { showSendSelector = false },
            onSelect = { video ->
                showSendSelector = false
                selectedVideoForShare = video
                viewModel.startShare(video)
            }
        )
    }

    // ── Dialog: QR Share Code ──────────────────────────────────────────────────
    qrCodeData?.let { qrData ->
        selectedVideoForShare?.let { video ->
            NearbySenderQrDialog(
                video = video,
                qrContent = qrData,
                onDismiss = {
                    viewModel.stopShare()
                    selectedVideoForShare = null
                }
            )
        }
    }

    // ── Dialog: QR Scanner (Receiver) ──────────────────────────────────────────
    if (showReceiverScanner) {
        Dialog(onDismissRequest = { showReceiverScanner = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                WatchPartyQrScannerScreen(
                    onBack = { showReceiverScanner = false },
                    onQrDetected = { qr ->
                        showReceiverScanner = false
                        viewModel.connectToQr(qr)
                    }
                )
            }
        }
    }
}

@Composable
private fun NearbyActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = NearbyBgCard,
        border = BorderStroke(1.dp, NearbyDivider)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NearbyTextPri)
                Text(subtitle, fontSize = 10.sp, color = NearbyTextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NearbyTransferCard(
    transfer: NearbyShareTransferState,
    onPauseToggle: () -> Unit,
    onResumeToggle: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NearbyBgCard,
        border = BorderStroke(1.dp, NearbyDivider)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (transfer.role == NearbyShareRole.SENDER) NearbyAccentP.copy(alpha = 0.15f)
                                else NearbyAccentG.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transfer.role == NearbyShareRole.SENDER) Icons.Default.FileUpload else Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = if (transfer.role == NearbyShareRole.SENDER) NearbyAccentP else NearbyAccentG,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transfer.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = NearbyTextPri,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (transfer.status) {
                                NearbyShareStatus.CONNECTING -> "Connecting to peer..."
                                NearbyShareStatus.TRANSFERRING -> "Active Transfer"
                                NearbyShareStatus.PAUSED -> "Paused"
                                NearbyShareStatus.COMPLETED -> "Finished successfully"
                                NearbyShareStatus.FAILED -> "Failed: ${transfer.error}"
                                else -> "Waiting..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (transfer.status == NearbyShareStatus.FAILED) Color.Red else NearbyTextSub
                        )
                    }
                }

                // Controls row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (transfer.status == NearbyShareStatus.TRANSFERRING || transfer.status == NearbyShareStatus.PAUSED) {
                        IconButton(
                            onClick = {
                                if (transfer.status == NearbyShareStatus.PAUSED) onResumeToggle()
                                else onPauseToggle()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (transfer.status == NearbyShareStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Toggle",
                                tint = NearbyTextPri
                            )
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = NearbyTextSub)
                    }
                }
            }

            if (transfer.status == NearbyShareStatus.TRANSFERRING || transfer.status == NearbyShareStatus.PAUSED || transfer.status == NearbyShareStatus.COMPLETED) {
                // Progress slider bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { transfer.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = NearbyAccentC,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                    Text(
                        text = transfer.formattedProgress,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NearbyTextPri
                    )
                }

                // Meta Row: speed, size, ETA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${transfer.formattedSpeed} (${android.text.format.Formatter.formatFileSize(LocalContext.current, transfer.bytesTransferred)} / ${android.text.format.Formatter.formatFileSize(LocalContext.current, transfer.fileSize)})",
                        fontSize = 10.sp,
                        color = NearbyTextSub
                    )
                    Text(
                        text = "ETA: ${transfer.formattedEta}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NearbyAccentC
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyVideoSelectorDialog(
    videos: List<Video>,
    onDismiss: () -> Unit,
    onSelect: (Video) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NearbyBgCard,
            border = BorderStroke(1.dp, NearbyDivider),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp)
                .padding(8.dp)
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
                    Text("Select Video to Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NearbyTextPri)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NearbyTextSub)
                    }
                }
                
                HorizontalDivider(color = NearbyDivider)

                if (videos.isEmpty()) {
                    Text("No local media found", color = NearbyTextSub, style = MaterialTheme.typography.bodyMedium)
                } else {
                    videos.forEach { video ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(video) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.02f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = NearbyAccentC)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NearbyTextPri, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(video.formattedSize, style = MaterialTheme.typography.bodySmall, color = NearbyTextSub)
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
private fun NearbySenderQrDialog(
    video: Video,
    qrContent: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(qrContent) { WatchPartyQrGenerator.generateQrBitmap(qrContent, 512) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NearbyBgCard,
            border = BorderStroke(1.dp, NearbyDivider),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Transmitter Portal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NearbyTextPri)
                        Text(video.title, style = MaterialTheme.typography.bodySmall, color = NearbyAccentC, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NearbyTextSub)
                    }
                }

                HorizontalDivider(color = NearbyDivider)

                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .size(220.dp)
                            .border(BorderStroke(4.dp, NearbyAccentC.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Transfer QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1E2A))
                            .border(BorderStroke(2.dp, NearbyAccentC.copy(alpha = 0.4f)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode, null, tint = NearbyTextSub, modifier = Modifier.size(80.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NearbyAccentC.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, NearbyAccentC.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Open the scanner on the receiving device and align the camera view with this code to pair and start downloading \"${video.title}\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = NearbyTextSub,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NearbyAccentG, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Portal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
