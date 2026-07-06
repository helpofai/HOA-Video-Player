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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
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
            VideoPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashReportScreen(
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
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            // Fallback to standard share
            val fallback = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "App Crash Report:\n\n$log")
            }, "Share Crash Report")
            startActivity(fallback)
        }
    }

    private fun reportOnGitHub(log: String) {
        // You can change this to your actual GitHub repo URL
        val githubUrl = "https://github.com/your-username/your-repo/issues/new?title=App+Crash&body=```\n${Uri.encode(log)}\n```"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
        startActivity(intent)
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportScreen(
    fullReport: String,
    onCopy: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onReportGitHub: () -> Unit,
    onRestart: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Crashed") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "We're sorry, but the app has encountered a critical error and needs to recover.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Log Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = fullReport,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
                
                Button(onClick = onSendWhatsApp, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) {
                    Text("WhatsApp")
                }
                
                Button(onClick = onReportGitHub, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) {
                    Text("GitHub")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Restart App")
            }
        }
    }
}
