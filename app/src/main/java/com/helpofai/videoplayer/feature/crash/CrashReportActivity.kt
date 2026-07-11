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
package com.helpofai.videoplayer.feature.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.theme.VideoPlayerTheme

class CrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val crashLog = intent.getStringExtra("CRASH_LOG") ?: "Unknown error occurred"
        val deviceInfo = intent.getStringExtra("DEVICE_INFO") ?: "Unknown device"
        val crashTime = intent.getStringExtra("CRASH_TIME") ?: "Unknown time"
        
        val fullReport = """
            --- Crash Report ---
            Time: $crashTime
            
            $deviceInfo
            
            Stacktrace:
            $crashLog
        """.trimIndent()
        
        setContent {
            VideoPlayerTheme(darkTheme = true) { // Always dark for professional logs
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B0E14) // Deep Space Dark
                ) {
                    CrashReportScreen(
                        crashLog = crashLog,
                        deviceInfo = deviceInfo,
                        crashTime = crashTime,
                        fullReport = fullReport,
                        onCopy = { copyToClipboard(fullReport) },
                        onSendWhatsApp = { sendToWhatsApp(fullReport) },
                        onReportGitHub = { reportOnGitHub(fullReport) },
                        onRestart = { restartApp() }
                    )
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Crash report copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun sendToWhatsApp(log: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, "App Crash Report:\n\n$log")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "App Crash Report:\n\n$log")
            }, "Share Crash Report")
            startActivity(fallback)
        }
    }

    private fun reportOnGitHub(log: String) {
        val githubUrl = "https://github.com/your-username/your-repo/issues/new?title=App+Crash&body=```\n${Uri.encode(log)}\n```"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
        startActivity(intent)
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportScreen(
    crashLog: String,
    deviceInfo: String,
    crashTime: String,
    fullReport: String,
    onCopy: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onReportGitHub: () -> Unit,
    onRestart: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    // Parse crash log for quick display
    val exceptionSummary = remember(crashLog) {
        val firstLine = crashLog.lineSequence().firstOrNull { it.isNotBlank() } ?: "UnknownException: Error"
        val parts = firstLine.split(":", limit = 2)
        val type = parts.getOrNull(0)?.trim()?.substringAfterLast('.') ?: "Exception"
        val desc = parts.getOrNull(1)?.trim() ?: "A critical error occurred"
        Pair(type, desc)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Error, 
                            contentDescription = null, 
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Application Diagnostics", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0D1117))
        ) {
            // Error Accent Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x33FF5252), Color.Transparent)
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = exceptionSummary.first,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exceptionSummary.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tab Rows
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF161B22),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stacktrace", color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Device Metadata", color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Gray) }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    // Monospace developer console with horizontal/vertical scrolling
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF010409))
                            .border(BorderStroke(1.dp, Color(0xFF30363D)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = crashLog,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFFE6EDF0)
                            )
                        }
                    }
                } else {
                    // System info layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161B22))
                            .border(BorderStroke(1.dp, Color(0xFF30363D)), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoSection("Time of Failure", crashTime)
                        InfoSection("Device Build", android.os.Build.MODEL)
                        InfoSection("Manufacturer", android.os.Build.MANUFACTURER)
                        InfoSection("Android Version", "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                        InfoSection("CPU ABI", android.os.Build.SUPPORTED_ABIS.joinToString(", "))
                        InfoSection("Hardware / Board", "${android.os.Build.HARDWARE} / ${android.os.Build.BOARD}")
                    }
                }
            }

            // Actions Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary Sharing Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Log")
                    }

                    OutlinedButton(
                        onClick = onSendWhatsApp,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Support")
                    }

                    OutlinedButton(
                        onClick = onReportGitHub,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                        border = BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Report Bug")
                    }
                }

                // Primary Action Button
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart Application", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InfoSection(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
    }
}
