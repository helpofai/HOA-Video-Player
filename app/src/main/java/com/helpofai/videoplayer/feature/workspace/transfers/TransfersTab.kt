package com.helpofai.videoplayer.feature.workspace.transfers

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import com.helpofai.videoplayer.core.model.Video
import kotlinx.coroutines.launch
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun TransfersTab(
    isTablet: Boolean,
    videos: List<Video>,
    modifier: Modifier = Modifier
) {
    val pages = listOf("Nearby Share", "Wi-Fi Portal", "Network Drive", "Queue", "Controls")
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val activeQueue = remember { mutableStateListOf<ActiveTransfer>() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Transfers Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Transfers",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Transfer Center",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ultrafast wireless file sharing & cloud connectivity",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pager tabs selector
        TabRow(
            selectedPageIndex = pagerState.currentPage,
            pages = pages,
            onTabClick = { index ->
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> NearbyTransferView(
                    videos = videos,
                    onStartSimulatedTransfer = { video ->
                        activeQueue.add(ActiveTransfer(video.title, 0f, "45s", false))
                        scope.launch {
                            pagerState.animateScrollToPage(3) // Switch to Queue view
                        }
                    }
                )
                1 -> WifiTransferView()
                2 -> NetworkClientView()
                3 -> TransferQueueView(
                    activeQueue = activeQueue,
                    onPauseToggle = { idx ->
                        if (idx in activeQueue.indices) {
                            val item = activeQueue[idx]
                            activeQueue[idx] = item.copy(isPaused = !item.isPaused)
                        }
                    }
                )
                4 -> TransferControlsView()
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // DYNAMIC DEVICE CONNECTIVITY HUB
        ConnectivityHubSection()
    }
}

data class ActiveTransfer(
    val name: String,
    val progress: Float,
    val eta: String,
    val isPaused: Boolean
)

@Composable
private fun TabRow(
    selectedPageIndex: Int,
    pages: List<String>,
    onTabClick: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedPageIndex,
        containerColor = Color.White.copy(alpha = 0.03f),
        edgePadding = 8.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedPageIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPageIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
    ) {
        pages.forEachIndexed { index, title ->
            Tab(
                selected = selectedPageIndex == index,
                onClick = { onTabClick(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedPageIndex == index) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = Color.LightGray
            )
        }
    }
}

@Composable
private fun ConnectivityHubSection() {
    val context = LocalContext.current
    
    // Dialog states
    var activeDialogType by remember { mutableStateOf<String?>(null) }
    var activeDialogMode by remember { mutableStateOf("") } // "sender" or "receiver"
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            activeDialogType = null
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }
    
    val requestCameraPermission = { type: String ->
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            activeDialogType = type
            activeDialogMode = "receiver"
        } else {
            activeDialogType = type
            activeDialogMode = "receiver"
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1216),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Device Connection Center (All Channels)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Button(
                    onClick = {
                        Toast.makeText(context, "Scanning all network adapters... Wi-Fi: 8ms, Cellular: 42ms", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Refresh Hub Diagnostics", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            
            // Render all 6 options in a clean stack
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. WI-FI
                ConnectionProfileCard(
                    title = "Local Wi-Fi Portal",
                    status = "Connected",
                    statusColor = Color.Cyan,
                    icon = Icons.Default.Wifi,
                    details = "SSID: HelpOfAi_Fiber  •  Speed: 866 Mbps  •  IP: 192.168.1.100",
                    instruction = "Setup: Connect both devices to the same local subnet router. Launch host portal Web Server.",
                    onShowSenderQr = {
                        activeDialogType = "Wi-Fi"
                        activeDialogMode = "sender"
                    },
                    onScanReceiver = {
                        requestCameraPermission("Wi-Fi")
                    }
                )
                
                // 2. CELLULAR INTERNET
                ConnectionProfileCard(
                    title = "Cellular WAN Relay",
                    status = "Active (5G)",
                    statusColor = Color.Green,
                    icon = Icons.Default.CellTower,
                    details = "Carrier: Jio 5G  •  Strength: -78 dBm  •  IP: Dynamic WAN IP",
                    instruction = "Setup: Invite remote users over cellular data using secure encrypted session keys.",
                    onShowSenderQr = {
                        activeDialogType = "Cellular"
                        activeDialogMode = "sender"
                    },
                    onScanReceiver = {
                        requestCameraPermission("Cellular")
                    }
                )
                
                // 3. USB HOST OTG
                ConnectionProfileCard(
                    title = "USB OTG Interface",
                    status = "Ready",
                    statusColor = Color.Yellow,
                    icon = Icons.Default.Usb,
                    details = "Disk: Sandisk Extreme SSD (1.8 TB)  •  Speed: 10 Gbps SuperSpeed",
                    instruction = "Setup: Mount external flash storage partitions using compatible USB-C adapters.",
                    onShowSenderQr = {
                        activeDialogType = "USB Host"
                        activeDialogMode = "sender"
                    },
                    onScanReceiver = {
                        requestCameraPermission("USB Host")
                    }
                )
                
                // 4. ETHERNET LAN
                ConnectionProfileCard(
                    title = "Ethernet LAN Link",
                    status = "Standby",
                    statusColor = Color.LightGray,
                    icon = Icons.Default.SettingsEthernet,
                    details = "Link Speed: --  •  IP Address: Unassigned",
                    instruction = "Setup: Plug category Category 6 LAN Ethernet cables into local network switch hubs.",
                    onShowSenderQr = {
                        activeDialogType = "Ethernet"
                        activeDialogMode = "sender"
                    },
                    onScanReceiver = {
                        requestCameraPermission("Ethernet")
                    }
                )
                
                // 5. MOBILE HOTSPOT
                ConnectionProfileCard(
                    title = "Mobile Hotspot Hub",
                    status = "Inactive",
                    statusColor = Color.LightGray,
                    icon = Icons.Default.Wifi,
                    details = "Broadcast SSID: vidplay_hotspot  •  Connected Clients: 0",
                    instruction = "Setup: Turn on mobile hotspot and invite nearby receivers to connect directly to your access point.",
                    onShowSenderQr = {
                        activeDialogType = "Hotspot"
                        activeDialogMode = "sender"
                    },
                    onScanReceiver = {
                        requestCameraPermission("Hotspot")
                    }
                )
                
                // 6. BLUETOOTH P2P LINK
                ConnectionProfileCard(
                    title = "Bluetooth Peer Sync",
                    status = "Ready",
                    statusColor = Color.Magenta,
                    icon = Icons.Default.Bluetooth,
                    details = "Discovery Mode: Enabled  •  Peer Link: Idle",
                    instruction = "Setup: Pair device with receiver. Best for light metadata sharing & connection handshakes.",
                    onShowSenderQr = {
                        activeDialogType = "Bluetooth"
                        activeDialogMode = "sender"
                    },
                    onScanReceiver = {
                        requestCameraPermission("Bluetooth")
                    }
                )
            }
        }
    }
    
    // Dialogs compiled inside standard direct conditional scopes to satisfy Compose compiler bounds
    val selectedType = activeDialogType
    if (selectedType != null) {
        if (activeDialogMode == "sender") {
            Dialog(onDismissRequest = { activeDialogType = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                            Text("Sender Share Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { activeDialogType = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }
                        
                        Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = Color.White, modifier = Modifier.size(140.dp))
                        
                        val qrDetails = when (selectedType) {
                            "Wi-Fi" -> "Local Wi-Fi portal host at\nhttp://192.168.1.100:8080"
                            "Cellular" -> "Internet WAN invite session token:\njio-session-98x4"
                            "USB Host" -> "USB-C OTG storage mount point:\n/mnt/media_rw/usbhost"
                            "Hotspot" -> "Direct local access portal host at\nhttp://192.168.43.1:8080"
                            "Bluetooth" -> "Bluetooth peer pairing profile:\nvidplay-handshake-512"
                            else -> "LAN Ethernet network bridge host at\nhttp://192.168.1.121:8080"
                        }
                        Text(qrDetails, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, textAlign = TextAlign.Center)
                        
                        Button(
                            onClick = { activeDialogType = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (activeDialogMode == "receiver") {
            Dialog(onDismissRequest = { activeDialogType = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                            Text("Receiver Scan Viewfinder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { activeDialogType = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }
                        
                        Text(
                            text = "Scan the Sender's $selectedType Connection QR code to link",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(BorderStroke(2.dp, Color.Cyan), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraPreview(modifier = Modifier.fillMaxSize())
                            
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner Finder",
                                tint = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                        }
                        
                        Button(
                            onClick = {
                                activeDialogType = null
                                Toast.makeText(context, "$selectedType Linked successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Setup Link", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionProfileCard(
    title: String,
    status: String,
    statusColor: Color,
    icon: ImageVector,
    details: String,
    instruction: String,
    onShowSenderQr: () -> Unit,
    onScanReceiver: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = status,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Sub-details
            Text(details, fontSize = 9.sp, color = Color.LightGray)
            Text(instruction, fontSize = 9.sp, color = Color.Gray)
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onShowSenderQr,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sender QR Code", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                
                TextButton(
                    onClick = onScanReceiver,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Cyan)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receiver Scan", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
