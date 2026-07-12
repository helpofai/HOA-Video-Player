package com.helpofai.videoplayer.feature.workspace.transfers

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.helpofai.videoplayer.feature.watch_party.qr.generator.WatchPartyQrGenerator
import com.helpofai.videoplayer.feature.watch_party.ui.qr_scanner.WatchPartyQrScannerScreen

private val TcBgDeep    = Color(0xFF090B10)
private val TcBgCard    = Color(0xFF111520)
private val TcAccentC   = Color(0xFF00CEC9)
private val TcAccentG   = Color(0xFF00B894)
private val TcTextPri   = Color(0xFFECF0F1)
private val TcTextSub   = Color(0xFF8E9CB0)
private val TcDivider   = Color(0xFF1E2535)

@Composable
fun TransfersChannelsView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Resolve network properties dynamically
    val wifiSsid = remember { TransfersNetworkHelper.getWifiSsid(context) }
    val localIp = remember { TransfersNetworkHelper.getLocalIpAddress() }
    val cellularName = remember { TransfersNetworkHelper.getCellularNetworkName(context) }
    val bluetoothName = remember { TransfersNetworkHelper.getBluetoothName() }
    val usbInfo = remember { TransfersNetworkHelper.getUsbStorageInfo(context) }

    val channelProfiles = remember(wifiSsid, localIp, cellularName, bluetoothName, usbInfo) {
        listOf(
            ChannelProfile(
                channelName = "Wi-Fi",
                title = "Local Wi-Fi Portal",
                status = "Connected",
                statusColor = Color(0xFF00CEC9),
                icon = Icons.Default.Wifi,
                details = "SSID: $wifiSsid  •  IP: $localIp",
                instruction = "Both devices on the same local subnet. Launch host portal web server.",
                qrContent = "vidplay://transfer?channel=wifi&host=$localIp&port=8082"
            ),
            ChannelProfile(
                channelName = "Cellular",
                title = "Cellular WAN Relay",
                status = "Active",
                statusColor = Color(0xFF00B894),
                icon = Icons.Default.CellTower,
                details = "Carrier: $cellularName  •  IP: Dynamic WAN IP",
                instruction = "Invite remote users over cellular using secure encrypted session keys.",
                qrContent = "vidplay://transfer?channel=cellular&session=rel-$localIp"
            ),
            ChannelProfile(
                channelName = "USB Host",
                title = "USB OTG Interface",
                status = if (usbInfo.startsWith("USB Storage")) "Mounted" else "Ready",
                statusColor = Color(0xFFFDCB6E),
                icon = Icons.Default.Usb,
                details = usbInfo,
                instruction = "Mount external flash storage partitions using compatible USB-C adapters.",
                qrContent = "vidplay://transfer?channel=usb&info=${android.net.Uri.encode(usbInfo)}"
            ),
            ChannelProfile(
                channelName = "Ethernet",
                title = "Ethernet LAN Link",
                status = "Standby",
                statusColor = Color(0xFF8E9CB0),
                icon = Icons.Default.SettingsEthernet,
                details = "Link Speed: --  •  IP Address: Unassigned",
                instruction = "Plug Category 6 LAN Ethernet cables into local network switch.",
                qrContent = "vidplay://transfer?channel=ethernet&host=$localIp&port=8082"
            ),
            ChannelProfile(
                channelName = "Hotspot",
                title = "Mobile Hotspot Hub",
                status = "Inactive",
                statusColor = Color(0xFF8E9CB0),
                icon = Icons.Default.Wifi,
                details = "Broadcast SSID: vidplay_hotspot  •  Connected Clients: 0",
                instruction = "Turn on mobile hotspot and invite receivers to connect directly.",
                qrContent = "vidplay://transfer?channel=hotspot&host=192.168.43.1&port=8082"
            ),
            ChannelProfile(
                channelName = "Bluetooth",
                title = "Bluetooth Peer Sync",
                status = "Ready",
                statusColor = Color(0xFF7C5CE7),
                icon = Icons.Default.Bluetooth,
                details = "Adapter: $bluetoothName  •  Discovery: Enabled",
                instruction = "Pair device with receiver. Best for metadata sharing & handshakes.",
                qrContent = "vidplay://transfer?channel=bluetooth&device=${android.net.Uri.encode(bluetoothName)}"
            )
        )
    }

    // Dialog state
    var activeDialog by remember { mutableStateOf<ConnectionDialog?>(null) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            activeDialog = null
            Toast.makeText(context, "Camera permission required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    val openScanner = { channel: String ->
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            activeDialog = ConnectionDialog.Scanner(channel)
        } else {
            activeDialog = ConnectionDialog.Scanner(channel)
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Device Connection Center",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TcTextPri
                )
                Text(
                    "All available channel profiles",
                    style = MaterialTheme.typography.bodySmall,
                    color = TcTextSub
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TcAccentC.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, TcAccentC.copy(alpha = 0.4f))
            ) {
                Text(
                    "6 channels",
                    fontSize = 10.sp,
                    color = TcAccentC,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = TcDivider)

        // Channel cards
        channelProfiles.forEach { profile ->
            ChannelProfileCard(
                profile = profile,
                onShowSenderQr = { activeDialog = ConnectionDialog.SenderQr(profile.channelName, profile.qrContent) },
                onScanReceiver = { openScanner(profile.channelName) }
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    when (val d = activeDialog) {
        is ConnectionDialog.SenderQr -> {
            SenderQrDialog(
                channelName = d.channelName,
                qrContent = d.qrContent,
                onDismiss = { activeDialog = null }
            )
        }
        is ConnectionDialog.Scanner -> {
            WatchPartyQrScannerScreen(
                onBack = { activeDialog = null },
                onQrDetected = { result ->
                    activeDialog = null
                    Toast.makeText(context, "✓ ${d.channelName} linked: $result", Toast.LENGTH_LONG).show()
                }
            )
        }
        null -> Unit
    }
}

private sealed class ConnectionDialog {
    data class SenderQr(val channelName: String, val qrContent: String) : ConnectionDialog()
    data class Scanner(val channelName: String) : ConnectionDialog()
}

private data class ChannelProfile(
    val channelName: String,
    val title: String,
    val status: String,
    val statusColor: Color,
    val icon: ImageVector,
    val details: String,
    val instruction: String,
    val qrContent: String
)

@Composable
private fun ChannelProfileCard(
    profile: ChannelProfile,
    onShowSenderQr: () -> Unit,
    onScanReceiver: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = TcBgCard,
        border = BorderStroke(1.dp, TcDivider)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(profile.statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            profile.icon,
                            contentDescription = null,
                            tint = profile.statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            profile.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TcTextPri
                        )
                        Text(
                            profile.details,
                            fontSize = 9.sp,
                            color = TcTextSub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = profile.statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, profile.statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = profile.status,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = profile.statusColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Setup instruction
            Text(
                profile.instruction,
                fontSize = 10.sp,
                color = TcTextSub,
                lineHeight = 14.sp
            )

            HorizontalDivider(color = TcDivider, thickness = 0.5.dp)

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sender QR
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onShowSenderQr() },
                    shape = RoundedCornerShape(10.dp),
                    color = TcAccentC.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, TcAccentC.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.QrCode, null, tint = TcAccentC, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Show QR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TcAccentC)
                    }
                }

                // Scan to receive
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onScanReceiver() },
                    shape = RoundedCornerShape(10.dp),
                    color = TcAccentG.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, TcAccentG.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = TcAccentG, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Scan QR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TcAccentG)
                    }
                }
            }
        }
    }
}

@Composable
private fun SenderQrDialog(
    channelName: String,
    qrContent: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(qrContent) {
        WatchPartyQrGenerator.generateQrBitmap(qrContent, 512)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TcBgCard,
            border = BorderStroke(1.dp, TcDivider),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Sender QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TcTextPri
                        )
                        Text(
                            channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TcAccentC
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = TcTextSub)
                    }
                }

                HorizontalDivider(color = TcDivider)

                // Real QR Code image
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.size(200.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1E2A))
                            .border(BorderStroke(2.dp, TcAccentC.copy(alpha = 0.4f)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode, null, tint = TcTextSub, modifier = Modifier.size(80.dp))
                    }
                }

                // Instructions
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TcAccentC.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, TcAccentC.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Scan this QR code with the receiver device to initiate a secure $channelName transfer session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TcTextSub,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TcAccentC,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
