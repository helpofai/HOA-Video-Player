package com.helpofai.videoplayer.feature.watch_party.ui.connection_status

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.feature.watch_party.networking.WatchPartyConnectionPreferences
import com.helpofai.videoplayer.feature.watch_party.networking.WatchPartyNetworkStateManager

private val BgCard    = Color(0xFF111520)
private val DivColor  = Color(0xFF1E2535)
private val TextPri   = Color(0xFFECF0F1)
private val TextSub   = Color(0xFF8E9CB0)
private val GreenOk   = Color(0xFF00B894)
private val CyanAlt   = Color(0xFF00CEC9)
private val PurpleAcc = Color(0xFF7C5CE7)
private val WarnAmber = Color(0xFFFDCB6E)
private val RedErr    = Color(0xFFFF7675)

@Composable
fun WatchPartyConnectionStatusSection(modifier: Modifier = Modifier) {
    val context    = LocalContext.current
    val networkMgr = remember { WatchPartyNetworkStateManager.getInstance(context) }
    val prefs      = remember { WatchPartyConnectionPreferences.getInstance(context) }

    val isWifi    by networkMgr.isWifiConnected.collectAsState()
    val isHotspot by networkMgr.isHotspotEnabled.collectAsState()
    val localIp   by networkMgr.localIpAddress.collectAsState()
    val ssid      by networkMgr.wifiSsid.collectAsState()
    val mode      by networkMgr.networkMode.collectAsState()
    val isMetered by networkMgr.isMetered.collectAsState()

    var bgKeepAlive by remember { mutableStateOf(prefs.backgroundKeepAlive) }
    var autoWifi    by remember { mutableStateOf(prefs.autoWifiEnabled) }

    LaunchedEffect(Unit) { networkMgr.refreshState() }

    val isAnyNetworkActive = isWifi || isHotspot

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        color = BgCard,
        border = BorderStroke(1.dp, DivColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAnyNetworkActive) PulsingDot(GreenOk)
                    else Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RedErr))
                    Text("Connection Status", color = TextPri, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(onClick = { networkMgr.refreshState() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = TextSub, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Status chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NetworkStatusChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CellTower,
                    label = "Hotspot",
                    value = if (isHotspot) "Active" else "Off",
                    detail = if (isHotspot) "IP: $localIp" else "Not broadcasting",
                    active = isHotspot,
                    activeColor = PurpleAcc
                )
                NetworkStatusChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Wifi,
                    label = "Wi-Fi",
                    value = if (isWifi) "Connected" else "Disconnected",
                    detail = if (isWifi) ssid else "No network",
                    active = isWifi,
                    activeColor = CyanAlt
                )
            }

            // Mode badge
            if (isAnyNetworkActive) {
                Spacer(Modifier.height(10.dp))
                val modeText = when (mode) {
                    WatchPartyNetworkStateManager.NetworkMode.WIFI_HOTSPOT ->
                        "Hotspot Mode \u2014 Clients connect to your hotspot Wi-Fi"
                    WatchPartyNetworkStateManager.NetworkMode.WIFI_CLIENT ->
                        "Same Network Mode \u2014 Host & client on same Wi-Fi router"
                    else -> ""
                }
                val modeColor = when (mode) {
                    WatchPartyNetworkStateManager.NetworkMode.WIFI_HOTSPOT -> PurpleAcc
                    WatchPartyNetworkStateManager.NetworkMode.WIFI_CLIENT  -> CyanAlt
                    else -> TextSub
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(modeColor.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (mode == WatchPartyNetworkStateManager.NetworkMode.WIFI_HOTSPOT)
                            Icons.Default.Router else Icons.Default.Hub,
                        null, tint = modeColor, modifier = Modifier.size(16.dp)
                    )
                    Text(modeText, color = modeColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Metered Connection Warning Banner
            if (isAnyNetworkActive && isMetered) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarnAmber.copy(alpha = 0.08f))
                        .border(BorderStroke(1.dp, WarnAmber.copy(alpha = 0.15f)))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null, tint = WarnAmber, modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Metered Hotspot / Connection Detected",
                            color = WarnAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set this connection to 'Metered' in system Wi-Fi details to prevent clients from draining host's mobile data during watch party sessions.",
                            color = TextSub,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // No network onboarding
            AnimatedVisibility(visible = !isAnyNetworkActive) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WarnAmber.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, WarnAmber.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WifiOff, null, tint = WarnAmber, modifier = Modifier.size(18.dp))
                                Text("No Network Active", color = WarnAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Watch Party requires a local network so devices can communicate directly. " +
                                "No internet needed \u2014 just a shared Wi-Fi or hotspot.",
                                color = TextSub, fontSize = 11.sp, lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("How to activate:", color = TextPri, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(Modifier.height(6.dp))
                            OnboardingStep("1", "Connect to Wi-Fi",
                                "Both host and client must be on the same Wi-Fi network.",
                                Icons.Default.Wifi)
                            OnboardingStep("2", "Or enable Mobile Hotspot",
                                "Host turns on hotspot, clients connect to it via Wi-Fi settings.",
                                Icons.Default.CellTower)
                            OnboardingStep("3", "Return here",
                                "This section updates automatically when a network is detected.",
                                Icons.Default.CheckCircle)
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { networkMgr.refreshState() },
                                colors = ButtonDefaults.textButtonColors(contentColor = WarnAmber)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh Status", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Settings toggles
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DivColor, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            SettingToggleRow(
                icon = Icons.Default.SettingsPower,
                iconColor = GreenOk,
                title = "Stay connected in background",
                subtitle = "Connection persists when app is minimized or closed",
                checked = bgKeepAlive,
                trackColor = GreenOk,
                onCheckedChange = { bgKeepAlive = it; prefs.backgroundKeepAlive = it }
            )

            Spacer(Modifier.height(6.dp))

            SettingToggleRow(
                icon = Icons.Default.WifiFind,
                iconColor = CyanAlt,
                title = "Auto-scan for rooms on Wi-Fi",
                subtitle = "Automatically scan local network when Wi-Fi is connected",
                checked = autoWifi,
                trackColor = CyanAlt,
                onCheckedChange = { autoWifi = it; prefs.autoWifiEnabled = it }
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val scale by inf.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "ps"
    )
    Box(modifier = Modifier.size(10.dp).scale(scale).clip(CircleShape).background(color))
}

@Composable
private fun NetworkStatusChip(
    icon: ImageVector, label: String, value: String,
    detail: String, active: Boolean, activeColor: Color, modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (active) activeColor.copy(alpha = 0.07f) else Color(0xFF0D1018),
        border = BorderStroke(1.dp, if (active) activeColor.copy(alpha = 0.4f) else Color(0xFF1E2535))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = if (active) activeColor else TextSub, modifier = Modifier.size(16.dp))
                Text(label, color = TextSub, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Text(value, color = if (active) activeColor else RedErr, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = TextSub, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun OnboardingStep(number: String, title: String, subtitle: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(PurpleAcc.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Text(number, color = PurpleAcc, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, tint = PurpleAcc, modifier = Modifier.size(13.dp))
                Text(title, color = TextPri, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(subtitle, color = TextSub, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector, iconColor: Color, title: String, subtitle: String,
    checked: Boolean, trackColor: Color, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(14.dp))
                Text(title, color = TextPri, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(subtitle, color = TextSub, fontSize = 10.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = trackColor)
        )
    }
}
