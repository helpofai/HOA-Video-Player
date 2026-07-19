package com.helpofai.videoplayer.feature.watch_party.ui.qr_preview

import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Palette
private val BgDeep       = Color.Transparent
private val BgCard       = Color(0xFF111520)
private val AccentPurple = Color(0xFF7C5CE7)
private val AccentCyan   = Color(0xFF00CEC9)
private val AccentGreen  = Color(0xFF00B894)
private val TextPrimary  = Color(0xFFECF0F1)
private val TextSub      = Color(0xFF8E9CB0)
private val DivColor     = Color(0xFF1E2535)
private val WarnAmber    = Color(0xFFFDCB6E)

@Composable
fun WatchPartyRoomPreviewScreen(
    roomData: String,
    onJoin: (
        roomId: String,
        roomName: String,
        hostIp: String,
        port: String,
        token: String,
        videoTitle: String?,
        videoDuration: Long?,
        videoPath: String?,
        videoSize: Long?
    ) -> Unit,
    onClose: () -> Unit
) {
    // Parse Uri to extract variables (scheme: vidplay://join?roomId=...&hostIp=...&port=...&token=...)
    val parsedUri = remember(roomData) {
        try {
            Uri.parse(roomData)
        } catch (e: Exception) {
            null
        }
    }

    val roomId = parsedUri?.getQueryParameter("roomId") ?: "ROOM-UNKNOWN"
    val rawRoomName = parsedUri?.getQueryParameter("roomName") ?: "Active Party Room"
    val roomName = try { java.net.URLDecoder.decode(rawRoomName, "UTF-8") } catch(e: Exception) { rawRoomName }
    val hostIp = parsedUri?.getQueryParameter("hostIp") ?: "192.168.1.1"
    val hostPort = parsedUri?.getQueryParameter("port") ?: "8080"
    val securityToken = parsedUri?.getQueryParameter("token") ?: "open"

    val rawVideoTitle = parsedUri?.getQueryParameter("videoTitle")
    val videoTitle = rawVideoTitle?.let { try { java.net.URLDecoder.decode(it, "UTF-8") } catch(e: Exception) { it } }
    val videoDuration = parsedUri?.getQueryParameter("videoDuration")?.toLongOrNull()
    val rawVideoPath = parsedUri?.getQueryParameter("videoPath")
    val videoPath = rawVideoPath?.let { try { java.net.URLDecoder.decode(it, "UTF-8") } catch(e: Exception) { it } }
    val videoSize = parsedUri?.getQueryParameter("videoSize")?.toLongOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(16.dp)
    ) {
        // Toolbar Title
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "Room Preview Details",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        // Detailed scrollable check list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, DivColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Connection Parameters", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    DetailRow("Room ID", roomId)
                    DetailDivider()
                    DetailRow("Host IP Address", "$hostIp:$hostPort")
                    DetailDivider()
                    DetailRow("Pairing Status", "Ready to Connect")
                    DetailDivider()
                    DetailRow("Security Encryption", if (securityToken != "open") "Protected via Token" else "No Password")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, DivColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Session Features & Stream Quality", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    FeatureCheckRow("Adaptive Stream Quality", true)
                    FeatureCheckRow("Audio Track Language Switches", true)
                    FeatureCheckRow("Dynamic Closed Captions/Subtitles", true)
                    FeatureCheckRow("Low-Latency Playback Sync (mDNS)", true)
                    FeatureCheckRow("Floating Emoji Interactions", true)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, DivColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Legal and Safety Policy", color = WarnAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Connecting to this party sharing stream requires standard network connection. Local content sharing only shares the host video cache and does not collect personal device files.",
                        color = TextSub,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, DivColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Cancel", fontSize = 14.sp)
            }

            Button(
                onClick = { onJoin(roomId, roomName, hostIp, hostPort, securityToken, videoTitle, videoDuration, videoPath, videoSize) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f).height(50.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Join Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSub, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(color = DivColor, thickness = 0.5.dp)
}

@Composable
private fun FeatureCheckRow(featureName: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(featureName, color = TextSub, fontSize = 12.sp)
        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (enabled) AccentGreen else Color.Red,
            modifier = Modifier.size(16.dp)
        )
    }
}
