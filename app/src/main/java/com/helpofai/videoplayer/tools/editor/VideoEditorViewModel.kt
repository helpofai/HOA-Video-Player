package com.helpofai.videoplayer.tools.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpofai.videoplayer.core.ffmpeg.FFmpegManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    private val ffmpegManager: FFmpegManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage

    private val _resultUri = MutableStateFlow<Uri?>(null)
    val resultUri: StateFlow<Uri?> = _resultUri
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun extractAudio(sourceUri: Uri) {
        processVideo(sourceUri, "Extracting Audio (MP3)...", "mp3") { inputPath, outputPath ->
            ffmpegManager.extractAudio(inputPath, outputPath)
        }
    }

    fun changeResolution(sourceUri: Uri, width: Int, height: Int) {
        processVideo(sourceUri, "Changing Resolution...", "mp4") { inputPath, outputPath ->
            ffmpegManager.changeResolution(inputPath, outputPath, width, height)
        }
    }

    fun makeGif(sourceUri: Uri, startMs: Long, durationMs: Long) {
        processVideo(sourceUri, "Creating GIF...", "gif") { inputPath, outputPath ->
            ffmpegManager.makeGif(inputPath, outputPath, startMs, durationMs)
        }
    }

    private fun processVideo(
        sourceUri: Uri, 
        taskName: String, 
        extension: String, 
        action: suspend (String, String) -> Boolean
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progressMessage.value = taskName
            _error.value = null
            _resultUri.value = null

            try {
                // 1. Copy source Uri to a temp file
                val tempInput = File(context.cacheDir, "input_temp_${System.currentTimeMillis()}.mp4")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempInput.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 2. Define output path in public Downloads folder for easy access
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val outDir = File(downloadsDir, "VidPlay")
                if (!outDir.exists()) outDir.mkdirs()
                
                val outputFile = File(outDir, "VidPlay_${System.currentTimeMillis()}.$extension")

                // 3. Execute FFmpeg
                val success = action(tempInput.absolutePath, outputFile.absolutePath)

                // Clean up temp
                if (tempInput.exists()) tempInput.delete()

                if (success) {
                    _resultUri.value = Uri.fromFile(outputFile)
                } else {
                    _error.value = "Failed to process video. Check if the video format is supported."
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
            } finally {
                _isProcessing.value = false
                _progressMessage.value = ""
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearResult() {
        _resultUri.value = null
    }
}
