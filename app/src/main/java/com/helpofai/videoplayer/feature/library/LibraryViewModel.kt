package com.helpofai.videoplayer.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.ffmpeg.FFmpegManager
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.scanner.ScannerStorageAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val ffmpegManager: FFmpegManager,
    private val storageAnalyzer: ScannerStorageAnalyzer
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()
    
    private val _storageReport = MutableStateFlow<ScannerStorageAnalyzer.StorageReport?>(null)
    val storageReport = _storageReport.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) {
            loadVideos()
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repository.getVideosWithMetadata().collectLatest { videos ->
                    _state.update { it.copy(videos = videos, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFavorite(video: Video) {
        viewModelScope.launch {
            repository.toggleFavorite(video.path, !video.isFavorite)
            // No need to manually update state, as it will flow from DB
        }
    }

    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            if (repository.deleteVideo(video)) {
                loadVideos() // Rescan media to update the list
            }
        }
    }

    fun renameVideo(video: Video, newName: String) {
        viewModelScope.launch {
            if (repository.renameVideo(video, newName)) {
                loadVideos() // Rescan media to update the list
            }
        }
    }
    
    fun mergeVideos(context: android.content.Context, video1: Video, video2: Video) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val outputPath = java.io.File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES),
                "Merged_${System.currentTimeMillis()}.mp4"
            ).absolutePath
            
            val success = ffmpegManager.mergeVideos(listOf(video1.path, video2.path), outputPath)
            
            if (success) {
                loadVideos()
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun analyzeStorage() {
        viewModelScope.launch {
            _storageReport.value = storageAnalyzer.analyze(_state.value.videos)
        }
    }
    
    fun clearStorageReport() {
        _storageReport.value = null
    }
}
