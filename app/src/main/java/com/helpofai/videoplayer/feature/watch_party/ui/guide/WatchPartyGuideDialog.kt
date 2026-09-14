/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) — Watch Party: Visual Guide & Instructions Dialog
|--------------------------------------------------------------------------
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| Comprehensive in-app visual documentation and step-by-step guide
| for Watch Party room creation, joining, streaming, and playback sync.
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.feature.watch_party.ui.guide

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helpofai.videoplayer.core.theme.ToolIconPalette
import com.helpofai.videoplayer.core.theme.frostedGlass

private val BgDeep       = Color(0xFF0C101A)
private val BgCard       = Color(0xFF131825)
private val AccentPurple = Color(0xFF7C5CE7)
private val AccentCyan   = Color(0xFF00CEC9)
private val AccentGreen  = Color(0xFF00B894)
private val AccentPink   = Color(0xFFFD79A8)
private val AccentAmber  = Color(0xFFFDCB6E)
private val TextPri      = Color(0xFFECF0F1)
private val TextSec      = Color(0xFF8E9CB0)
private val BorderCol    = Color(0xFF1E2638)

@Composable
fun WatchPartyGuideDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Host Room", "Join Room", "Sync Play", "Troubleshooting")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = BgDeep,
            border = BorderStroke(1.dp, BorderCol),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF192033), Color(0xFF0E1321))
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                .background(
                                    Brush.linearGradient(listOf(AccentPurple, AccentCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Watch Party Guide",
                                color = TextPri,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Visual documentation & step-by-step instructions",
                                color = TextSec,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = TextPri, modifier = Modifier.size(18.dp))
                    }
                }

                // Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F1422),
                    contentColor = AccentCyan,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentCyan,
                            height = 3.dp
                        )
                    },
                    divider = { HorizontalDivider(color = BorderCol, thickness = 0.5.dp) }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) AccentCyan else TextSec
                                )
                            }
                        )
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> GuideOverviewSection()
                        1 -> GuideHostSection()
                        2 -> GuideJoinSection()
                        3 -> GuideSyncSection()
                        4 -> GuideTroubleshootingSection()
                    }
                }

                // Bottom bar
                Surface(
                    color = Color(0xFF0A0D17),
                    border = BorderStroke(1.dp, BorderCol),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTab > 0) {
                            TextButton(onClick = { selectedTab-- }) {
                                Text("Previous", color = TextSec, fontSize = 12.sp)
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        if (selectedTab < tabTitles.size - 1) {
                            Button(
                                onClick = { selectedTab++ },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Next", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Got it, let's watch!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 1: OVERVIEW
// -------------------------------------------------------------------------
@Composable
private fun GuideOverviewSection() {
    GuideCard(
        title = "What is Watch Party?",
        icon = Icons.Default.Bolt,
        accentColor = AccentPurple
    ) {
        Text(
            "Watch Party connects multiple nearby devices together over local Wi-Fi or Personal Hotspot so everyone watches the same video in real time with synchronized play, pause, seek, and interactive chat \u2014 with ZERO internet data consumption!",
            color = TextPri,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }

    GuideCard(
        title = "How It Works (Architecture)",
        icon = Icons.Default.Hub,
        accentColor = AccentCyan
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ArchitectureStep(
                number = "1",
                title = "Local Network (No Internet Required)",
                desc = "Host and Guests connect to the same Wi-Fi router or one device turns on a Personal Hotspot that others connect to."
            )
            ArchitectureStep(
                number = "2",
                title = "Host Video HTTP Server (Port 9980)",
                desc = "Host streams the selected video file locally with HTTP 206 Partial Content Range support so clients seek without lag."
            )
            ArchitectureStep(
                number = "3",
                title = "Bidirectional TCP Tunnel (Port 9990)",
                desc = "Sends millisecond-level position synchronization, play/pause commands, chat messages, and live emoji reactions."
            )
            ArchitectureStep(
                number = "4",
                title = "UDP Multicast Discovery (Port 8079)",
                desc = "Clients discover active party rooms on the subnet automatically without needing to type IP addresses."
            )
        }
    }

    GuideCard(
        title = "Key Capabilities",
        icon = Icons.Default.Star,
        accentColor = AccentAmber
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BulletItem("High Speed Local Streaming (40-100+ Mbps)", AccentGreen)
            BulletItem("Microsecond Drift Compensation (< 1500ms auto-seek)", AccentCyan)
            BulletItem("Granular Host Permissions (Play/Pause, Scrub, Volume, Queue)", AccentPurple)
            BulletItem("Dynamic QR Code & Deep Link Sharing (hoavideo://join)", AccentPink)
            BulletItem("In-Player Mini Preview & Floating Reaction Emojis", AccentAmber)
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 2: HOW TO HOST A ROOM
// -------------------------------------------------------------------------
@Composable
private fun GuideHostSection() {
    GuideCard(
        title = "Step-by-Step: Hosting a Room",
        icon = Icons.Default.Person,
        accentColor = AccentPurple
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepRow(
                step = "1",
                title = "Tap 'Host Room' on the Watch Party tab",
                desc = "Opens the Host Room Setup configuration screen."
            )
            StepRow(
                step = "2",
                title = "Select Video to Stream",
                desc = "Choose any video from your library using the 'Select Video' button, or open any video directly in the player."
            )
            StepRow(
                step = "3",
                title = "Configure Room Name & Security",
                desc = "Name your room (e.g. 'Action Night \uD83C\uDF7F'). Optionally set an access password to restrict entrance."
            )
            StepRow(
                step = "4",
                title = "Set Guest Permissions",
                desc = "Decide whether guests can Play/Pause, Seek the timeline, Adjust volume, or change audio tracks."
            )
            StepRow(
                step = "5",
                title = "Tap 'Create Room & Start Streaming'",
                desc = "The local HTTP streaming server and TCP control tunnel start instantly. Your dynamic QR code is ready!"
            )
        }
    }

    GuideCard(
        title = "Inviting Guests",
        icon = Icons.Default.QrCode2,
        accentColor = AccentCyan
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Once your room is active, invite friends using any of these methods:",
                color = TextPri,
                fontSize = 12.sp
            )
            InviteMethodRow(
                icon = Icons.Default.QrCode,
                title = "Show QR Code",
                desc = "Have friends open Watch Party \u2192 'Join Room' \u2192 'Scan QR' with their camera."
            )
            InviteMethodRow(
                icon = Icons.Default.ContentCopy,
                title = "Copy Join Link",
                desc = "Tap 'Copy Join Link' and send via WhatsApp/Telegram (opens via deep link)."
            )
            InviteMethodRow(
                icon = Icons.Default.CellTower,
                title = "Local Network Discovery",
                desc = "Guests on the same Wi-Fi will see your room pop up automatically in 'Available Rooms'!"
            )
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 3: HOW TO JOIN A ROOM
// -------------------------------------------------------------------------
@Composable
private fun GuideJoinSection() {
    GuideCard(
        title = "3 Easy Ways to Join",
        icon = Icons.Default.GroupAdd,
        accentColor = AccentCyan
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MethodCard(
                badge = "Method A (Fastest)",
                title = "Scan Host's QR Code",
                desc = "1. Tap 'Join Room' \u2192 'Scan Host QR Code'\n2. Point your camera at host's screen\n3. Review room details & tap 'Join Now'",
                color = AccentCyan
            )
            MethodCard(
                badge = "Method B (Automatic)",
                title = "Network Discovered Rooms",
                desc = "1. Connect to the same Wi-Fi/Hotspot as host\n2. Open 'Join Room' \u2014 see active host under 'Available Rooms'\n3. Tap 'Request Join' to connect instantly",
                color = AccentGreen
            )
            MethodCard(
                badge = "Method C (Direct Link)",
                title = "Tap Shared Link",
                desc = "Tap any hoavideo://join link sent by the host to launch the app directly into room preview.",
                color = AccentPurple
            )
        }
    }

    GuideCard(
        title = "Starting Playback on Client",
        icon = Icons.Default.PlayCircle,
        accentColor = AccentGreen
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "After joining, the Client Dashboard shows connection status and video title. Tap the green 'Watch Stream' button to launch the full synchronized video player!",
                color = TextPri,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 4: PLAYBACK & SYNCHRONIZATION
// -------------------------------------------------------------------------
@Composable
private fun GuideSyncSection() {
    GuideCard(
        title = "Host: Synchronized Mode in Player",
        icon = Icons.Default.CellTower,
        accentColor = AccentPurple
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "When you create a party room, a 'Synchronized Mode' card appears directly on the Now Playing screen:",
                color = TextPri,
                fontSize = 12.sp
            )
            BulletItem("Required for Streaming: Turn ON Synchronized Mode to broadcast video and sync playback with guests.", AccentGreen)
            BulletItem("Private Local Playback: When turned OFF, videos play strictly locally on your device without streaming.", AccentAmber)
            BulletItem("Live Glance Badge: A discreet badge displays current sync state and member count when player controls hide.", AccentCyan)
        }
    }

    GuideCard(
        title = "How Playback Stays in Sync",
        icon = Icons.Default.Sync,
        accentColor = AccentGreen
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BulletItem("Real-time Play/Pause: When the host pauses or resumes, all client players respond within milliseconds.", AccentGreen)
            BulletItem("Auto Drift Correction: If client network lags and falls > 1.5 seconds behind, the player auto-seeks to match host position.", AccentCyan)
            BulletItem("HTTP 206 Partial Streaming: Videos are streamed in lightweight chunks on-demand, not downloaded in advance.", AccentPurple)
            BulletItem("Interactive Reaction Overlay: Tap emoji buttons in the player to float hearts, fire, and smiles across everyone's screen!", AccentPink)
        }
    }

    GuideCard(
        title = "Permission Controls",
        icon = Icons.Default.Security,
        accentColor = AccentAmber
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "The host retains full control over the session at all times:",
                color = TextPri,
                fontSize = 12.sp
            )
            PermissionRow("Play / Pause", "Allow guests to pause for bathroom/snack breaks")
            PermissionRow("Timeline Scrubbing", "Allow guests to fast-forward or rewind")
            PermissionRow("Kick / Ban", "Host can remove disruptive devices at any moment")
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 5: TROUBLESHOOTING & TIPS
// -------------------------------------------------------------------------
@Composable
private fun GuideTroubleshootingSection() {
    GuideCard(
        title = "Best Connection Setup: Personal Hotspot",
        icon = Icons.Default.WifiTethering,
        accentColor = AccentPurple
    ) {
        Text(
            "For the fastest, zero-lag experience anywhere (park, travel, offline):\n" +
            "1. Turn on Portable Hotspot on Host phone (no mobile data needed!)\n" +
            "2. Connect other phones to that Hotspot Wi-Fi\n" +
            "3. Create Watch Party \u2014 streaming speeds reach 50+ Mbps with 0 internet!",
            color = TextPri,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }

    GuideCard(
        title = "Common Issues & Solutions",
        icon = Icons.Default.HelpOutline,
        accentColor = AccentAmber
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TroubleItem(
                q = "QR Scanner doesn't recognize the code?",
                a = "Ensure good lighting and hold the phone steady 15-25 cm away. You can also join via 'Network Discovered Rooms'!"
            )
            TroubleItem(
                q = "Host room not appearing in Discovered Rooms?",
                a = "Verify both phones are connected to the exact same Wi-Fi SSID or Hotspot. Ensure AP Isolation is disabled on public Wi-Fi routers."
            )
            TroubleItem(
                q = "Video buffering on guest phone?",
                a = "Keep devices within 10 meters of each other. Hotspot mode generally provides 3x lower latency than crowded home routers."
            )
        }
    }
}

// -------------------------------------------------------------------------
// REUSABLE SUBCOMPONENTS
// -------------------------------------------------------------------------

@Composable
private fun GuideCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Text(title, color = TextPri, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            HorizontalDivider(color = BorderCol, thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
private fun ArchitectureStep(number: String, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AccentCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(desc, color = TextSec, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun BulletItem(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text, color = TextSec, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun StepRow(step: String, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AccentPurple.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = AccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPri, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(desc, color = TextSec, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun InviteMethodRow(icon: ImageVector, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(desc, color = TextSec, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MethodCard(badge: String, title: String, desc: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(badge.uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            Text(title, color = TextPri, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(desc, color = TextSec, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun PermissionRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPri, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            Text(desc, color = TextSec, fontSize = 10.sp)
        }
        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TroubleItem(q: String, a: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Q: $q", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("A: $a", color = TextSec, fontSize = 11.sp, lineHeight = 15.sp)
    }
}
