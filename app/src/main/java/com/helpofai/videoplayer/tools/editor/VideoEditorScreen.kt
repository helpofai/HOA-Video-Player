package com.helpofai.videoplayer.tools.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    mode: String,
    onNavigateBack: () -> Unit,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val error by viewModel.error.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Editor - $mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            if (selectedVideoUri == null) {
                Spacer(modifier = Modifier.height(40.dp))
                Icon(
                    Icons.Default.VideoFile, 
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select a video to edit",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { filePickerLauncher.launch("video/*") }
                ) {
                    Text("Browse Videos")
                }
            } else {
                Text(
                    text = "Selected Video: ${selectedVideoUri?.lastPathSegment}",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                when (mode) {
                    "Video to MP3" -> AudioExtractionControls(
                        uri = selectedVideoUri!!,
                        isProcessing = isProcessing,
                        onExtract = { viewModel.extractAudio(it) }
                    )
                    "Change Resolution" -> ResolutionChangeControls(
                        uri = selectedVideoUri!!,
                        isProcessing = isProcessing,
                        onChange = { uri, width, height -> viewModel.changeResolution(uri, width, height) }
                    )
                    "Make GIF" -> GifCreationControls(
                        uri = selectedVideoUri!!,
                        isProcessing = isProcessing,
                        onCreate = { uri, start, duration -> viewModel.makeGif(uri, start, duration) }
                    )
                    else -> Text("Unknown mode: $mode", color = Color.Red)
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(40.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(progressMessage, color = Color.White)
                }

                if (resultUri != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Success!", 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White
                            )
                            Text(
                                "Saved to Downloads folder.", 
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.clearResult(); selectedVideoUri = null }) {
                        Text("Edit Another Video")
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(error!!, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AudioExtractionControls(
    uri: Uri,
    isProcessing: Boolean,
    onExtract: (Uri) -> Unit
) {
    Button(
        onClick = { onExtract(uri) },
        enabled = !isProcessing,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Extract MP3")
    }
}

@Composable
fun ResolutionChangeControls(
    uri: Uri,
    isProcessing: Boolean,
    onChange: (Uri, Int, Int) -> Unit
) {
    var widthText by remember { mutableStateOf("1280") }
    var heightText by remember { mutableStateOf("720") }
    
    OutlinedTextField(
        value = widthText,
        onValueChange = { widthText = it },
        label = { Text("Width") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = heightText,
        onValueChange = { heightText = it },
        label = { Text("Height") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { 
            val w = widthText.toIntOrNull() ?: 1280
            val h = heightText.toIntOrNull() ?: 720
            onChange(uri, w, h)
        },
        enabled = !isProcessing,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Convert Resolution")
    }
}

@Composable
fun GifCreationControls(
    uri: Uri,
    isProcessing: Boolean,
    onCreate: (Uri, Long, Long) -> Unit
) {
    var startText by remember { mutableStateOf("0") }
    var durationText by remember { mutableStateOf("5") }
    
    OutlinedTextField(
        value = startText,
        onValueChange = { startText = it },
        label = { Text("Start Time (seconds)") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = durationText,
        onValueChange = { durationText = it },
        label = { Text("Duration (seconds)") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { 
            val startMs = (startText.toLongOrNull() ?: 0L) * 1000L
            val durationMs = (durationText.toLongOrNull() ?: 5L) * 1000L
            onCreate(uri, startMs, durationMs)
        },
        enabled = !isProcessing,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Create GIF")
    }
}
