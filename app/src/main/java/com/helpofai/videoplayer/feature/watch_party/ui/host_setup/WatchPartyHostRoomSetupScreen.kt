/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) — Watch Party: Host Room Setup Screen
|--------------------------------------------------------------------------
| Sub-page opened when user taps "Host Room" on the Watch Party page.
| The video to stream is automatically taken from the currently playing
| video in PlayerScreen (via WatchPartySessionManager.currentStreamingVideo).
| NO video picker dialog is shown here.
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.feature.watch_party.ui.host_setup

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager
import com.helpofai.videoplayer.feature.watch_party.streaming.WatchPartyLocalStreamingServer
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID

// Color palette
private val BgDeep       = Color(0xFF090B10)
private val BgCard       = Color(0xFF111520)
private val AccentPurple = Color(0xFF7C5CE7)
private val AccentCyan   = Color(0xFF00CEC9)
private val AccentGreen  = Color(0xFF00B894)
private val TextPrimary  = Color(0xFFECF0F1)
private val TextSub      = Color(0xFF8E9CB0)
private val DivColor     = Color(0xFF1E2535)
private val WarnAmber    = Color(0xFFFDCB6E)

/**
 * WatchPartyHostRoomSetupScreen
 *
 * Called when user taps "Host Room" from the Watch Party main page.
 * Streams the video that is ALREADY PLAYING in PlayerScreen (signalled
 * via WatchPartySessionManager.currentStreamingVideo).
 *
 * @param onBack        Navigate back to Watch Party main page
 * @param onRoomCreated Called once room is successfully started
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WatchPartyHostRoomSetupScreen(
    onBack: () -> Unit,
    onRoomCreated: () -> Unit
) {
    BackHandler { onBack() }
    
    val context        = LocalContext.current
    val sessionMgr     = remember { WatchPartySessionManager.getInstance() }
    val streamingVideo by sessionMgr.currentStreamingVideo.collectAsState()
    val activeSession  by sessionMgr.activeSession.collectAsState()
    val streamingServer = remember { WatchPartyLocalStreamingServer(context) }

    // Room config state
    val autoRoomId = remember { "ROOM-" + UUID.randomUUID().toString().take(6).uppercase() }
    var roomName      by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var usePassword   by remember { mutableStateOf(false) }
    var maxGuests     by remember { mutableStateOf(8f) }
    var showPassword  by remember { mutableStateOf(false) }

    // Client permission toggles
    var allowPlayPause       by remember { mutableStateOf(false) }
    var allowSeek            by remember { mutableStateOf(false) }
    var allowVolume          by remember { mutableStateOf(true) }
    var allowNextPrev        by remember { mutableStateOf(false) }
    var allowGestures        by remember { mutableStateOf(true) }
    var allowReactions       by remember { mutableStateOf(true) }
    var allowSubtitleToggle  by remember { mutableStateOf(false) }
    var allowAudioTrack      by remember { mutableStateOf(false) }
    var allowFolderQueue     by remember { mutableStateOf(false) }

    val localIp = remember { getLocalIpForSetup() }
    val deviceName = remember {
        val brand = android.os.Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
        val model = android.os.Build.MODEL ?: "Device"
        "$brand $model"
    }

    val isVideoReady   = streamingVideo != null
    val isRoomNameValid = roomName.isNotBlank()

    // If room already created, go to dashboard
    LaunchedEffect(activeSession) {
        if (activeSession != null) onRoomCreated()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0D1018), Color.Transparent))
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("Host Room Setup", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Configure your watch party room", color = TextSub, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Video Source Banner
                if (isVideoReady) {
                    SetupCard(borderColor = AccentGreen.copy(alpha = 0.4f), bgColor = Color(0xFF0D2218)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, null, tint = AccentGreen, modifier = Modifier.size(26.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Video Ready to Stream", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    streamingVideo!!.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("From currently playing video \u2022 Watch Party tick is ON", color = TextSub, fontSize = 10.sp)
                            }
                            Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    SetupCard(borderColor = WarnAmber.copy(alpha = 0.4f), bgColor = Color(0xFF201A0A)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarnAmber.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MovieFilter, null, tint = WarnAmber, modifier = Modifier.size(26.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "No Video Selected for Streaming",
                                    color = WarnAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "To stream a video, open a video in the main player, then enable the \"Watch Party\" toggle in the player's Tools panel. Once enabled, come back here.",
                                    color = TextSub,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HowToStep(number = "1", text = "Open any video")
                                    Icon(Icons.Default.ChevronRight, null, tint = TextSub, modifier = Modifier.size(14.dp))
                                    HowToStep(number = "2", text = "Tools \u2192 Watch Party \u2713")
                                    Icon(Icons.Default.ChevronRight, null, tint = TextSub, modifier = Modifier.size(14.dp))
                                    HowToStep(number = "3", text = "Come back here")
                                }
                            }
                        }
                    }
                }

                // Room Identity
                SectionHeader("Room Identity", Icons.Default.MeetingRoom)

                SetupCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Room ID (Auto-Generated)", color = TextSub, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0D1018))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(autoRoomId, color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 2.sp)
                                Icon(Icons.Default.Lock, null, tint = TextSub, modifier = Modifier.size(14.dp))
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Room Name", color = TextSub, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                placeholder = { Text("e.g. Movie Night \uD83C\uDFAC", color = TextSub, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = DivColor,
                                    cursorColor = AccentPurple,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Security
                SectionHeader("Security", Icons.Default.Security)

                SetupCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Require Password", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Guests must enter password to join", color = TextSub, fontSize = 10.sp)
                            }
                            Switch(
                                checked = usePassword,
                                onCheckedChange = { usePassword = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentPurple)
                            )
                        }

                        if (usePassword) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password", color = TextSub, fontSize = 12.sp) },
                                placeholder = { Text("Enter room password", color = TextSub, fontSize = 12.sp) },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextSub)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = DivColor,
                                    cursorColor = AccentPurple,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Capacity
                SectionHeader("Capacity", Icons.Default.People)

                SetupCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Maximum Guests", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("${maxGuests.toInt()} guests", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Slider(
                            value = maxGuests,
                            onValueChange = { maxGuests = it },
                            valueRange = 1f..20f,
                            steps = 18,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentPurple,
                                activeTrackColor = AccentPurple,
                                inactiveTrackColor = DivColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1", color = TextSub, fontSize = 10.sp)
                            Text("10", color = TextSub, fontSize = 10.sp)
                            Text("20", color = TextSub, fontSize = 10.sp)
                        }
                    }
                }

                // Client Permissions
                SectionHeader("Client Permissions", Icons.Default.AdminPanelSettings)

                SetupCard {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Choose what controls your guests can use in the player:",
                            color = TextSub,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PermissionToggle(Icons.Default.PlayArrow, "Play / Pause",
                            "Guests can pause and resume playback", allowPlayPause, AccentGreen) { allowPlayPause = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.FastForward, "Seek / Scrub",
                            "Guests can jump to any position in the video", allowSeek, AccentCyan) { allowSeek = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.VolumeUp, "Volume Control",
                            "Guests can adjust their own local volume", allowVolume, AccentPurple) { allowVolume = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.SkipNext, "Next / Previous Video",
                            "Guests can change the video in the queue", allowNextPrev, WarnAmber) { allowNextPrev = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.TouchApp, "Gesture Controls",
                            "Swipe gestures for brightness, volume, seek", allowGestures, AccentCyan) { allowGestures = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.EmojiEmotions, "Emoji Reactions",
                            "Guests can send floating emoji reactions", allowReactions, Color(0xFFFF7675)) { allowReactions = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.Subtitles, "Subtitle Toggle",
                            "Guests can enable/disable subtitles", allowSubtitleToggle, AccentGreen) { allowSubtitleToggle = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.AudioFile, "Audio Track Switch",
                            "Guests can change the audio language track", allowAudioTrack, AccentPurple) { allowAudioTrack = it }
                        PermDivider()
                        PermissionToggle(Icons.Default.QueueMusic, "View Folder Queue",
                            "Guests can see other videos in the same folder", allowFolderQueue, AccentCyan) { allowFolderQueue = it }
                    }
                }

                // Network Info
                SetupCard(bgColor = Color(0xFF0D1018)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Wifi, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Host Network: $localIp", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("Clients must be on the same Wi-Fi / hotspot network", color = TextSub, fontSize = 10.sp)
                        }
                    }
                }

                // Create Room Button
                val canCreate = isRoomNameValid && (!usePassword || password.isNotBlank())

                Button(
                    onClick = {
                        val video = sessionMgr.currentStreamingVideo.value
                        sessionMgr.isClientMode = false
                        if (video != null) {
                            streamingServer.startStreamingServer(video, 8080)
                            sessionMgr.setStreamingVideo(video)
                        }
                        sessionMgr.createSession(
                            name = roomName,
                            hostIp = localIp,
                            hostDeviceName = deviceName,
                            video = video,
                            securityToken = if (usePassword) password else "hoa-${System.currentTimeMillis()}",
                            usePassword = usePassword,
                            password = password,
                            allowPlayPause = allowPlayPause,
                            allowSeek = allowSeek,
                            allowVolume = allowVolume,
                            allowNextPrev = allowNextPrev,
                            allowGestures = allowGestures,
                            allowReactions = allowReactions,
                            allowSubtitleToggle = allowSubtitleToggle,
                            allowAudioTrack = allowAudioTrack,
                            allowFolderQueue = allowFolderQueue,
                            maxUsers = maxGuests.toInt()
                        )
                        Toast.makeText(context, "Watch Party Room \"$roomName\" is live!", Toast.LENGTH_SHORT).show()
                        onRoomCreated()
                    },
                    enabled = canCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple,
                        contentColor = Color.White,
                        disabledContainerColor = AccentPurple.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(Icons.Default.Wifi, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isVideoReady) "Create Room & Start Streaming" else "Create Room (No Video Yet)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (!isVideoReady) {
                    Text(
                        "\u26A0 Open a video in the player and enable Watch Party to start streaming",
                        color = WarnAmber,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// Reusable sub-components

@Composable
private fun SetupCard(
    bgColor: Color = BgCard,
    borderColor: Color = DivColor,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(icon, null, tint = AccentPurple, modifier = Modifier.size(16.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun PermissionToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color = AccentPurple,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = if (checked) 0.15f else 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (checked) accentColor else TextSub, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (checked) TextPrimary else TextSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSub.copy(alpha = 0.7f), fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextSub,
                uncheckedTrackColor = DivColor
            )
        )
    }
}

@Composable
private fun PermDivider() {
    HorizontalDivider(color = DivColor, thickness = 0.5.dp)
}

@Composable
private fun HowToStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(AccentPurple.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = AccentPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, color = TextSub, fontSize = 9.sp)
    }
}

private fun getLocalIpForSetup(): String {
    return try {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .flatMap { Collections.list(it.inetAddresses) }
            .firstOrNull { !it.isLoopbackAddress && (it.hostAddress?.indexOf(':') ?: -1) < 0 }
            ?.hostAddress ?: "192.168.1.100"
    } catch (e: Exception) {
        "192.168.1.100"
    }
}
