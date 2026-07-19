package com.helpofai.videoplayer.feature.watch_party.ui.qr_scanner

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.helpofai.videoplayer.feature.watch_party.qr.scanner.WatchPartyQrScannerEngine
import java.util.concurrent.Executors

private val BgDeep       = Color.Transparent
private val TextPrimary  = Color(0xFFECF0F1)

@Composable
fun WatchPartyQrScannerScreen(
    onBack: () -> Unit,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { 
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        
        val scannerEngine = WatchPartyQrScannerEngine(onQrDetected)
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), scannerEngine::processImage)
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            
            // Configure tap-to-focus for maximum clarity
            previewView.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(event.x, event.y)
                    val action = androidx.camera.core.FocusMeteringAction.Builder(point, androidx.camera.core.FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    camera.cameraControl.startFocusAndMetering(action)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Toolbar Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Scan QR Code",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView }
            )

            // Scanning visual overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(240.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF00CEC9))
                ) {}
            }
        }
    }
}

// Extension to clean layout code
private fun Modifier.fillStaffordModifier(): Modifier = this.fillMaxSize()
