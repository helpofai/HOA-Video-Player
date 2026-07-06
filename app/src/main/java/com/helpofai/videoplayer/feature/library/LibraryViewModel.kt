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
package com.helpofai.videoplayer.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpofai.videoplayer.core.data.SettingsRepository
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.ffmpeg.FFmpegManager
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.scanner.ScannerStorageAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val label: String) {
    DATE_ADDED_DESC("Date Added (Newest)"), 
    DATE_ADDED_ASC("Date Added (Oldest)"), 
    NAME_ASC("Name (A-Z)"), 
    NAME_DESC("Name (Z-A)"), 
    SIZE_DESC("Size (Largest)"), 
    SIZE_ASC("Size (Smallest)"), 
    DURATION_DESC("Duration (Longest)"), 
    DURATION_ASC("Duration (Shortest)")
}

enum class FilterOption(val label: String) {
    ALL("All Videos"), 
    FAVORITES("Favorites"), 
    UNWATCHED("Unwatched"), 
    SHORT_VIDEOS("Short Clips (<5m)"), 
    LONG_VIDEOS("Long Videos (>30m)")
}

data class LibraryState(
    val allVideos: List<Video> = emptyList(),
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false,
    val sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    val filterOption: FilterOption = FilterOption.ALL,
    val folderViewMode: String = "list"
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val ffmpegManager: FFmpegManager,
    private val storageAnalyzer: ScannerStorageAnalyzer,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()
    
    private val _storageReport = MutableStateFlow<ScannerStorageAnalyzer.StorageReport?>(null)
    val storageReport = _storageReport.asStateFlow()

    private var loadJob: Job? = null
    
    init {
        viewModelScope.launch {
            settingsRepository.folderViewMode.collect { mode ->
                _state.update { it.copy(folderViewMode = mode) }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) {
            loadVideos()
        }
    }

    private fun applySortAndFilter(all: List<Video>, sort: SortOption, filter: FilterOption): List<Video> {
        var result = all
        
        result = when (filter) {
            FilterOption.ALL -> result
            FilterOption.FAVORITES -> result.filter { it.isFavorite }
            FilterOption.UNWATCHED -> result.filter { it.playCount == 0 }
            FilterOption.SHORT_VIDEOS -> result.filter { it.duration in 1..299999 } // < 5 mins
            FilterOption.LONG_VIDEOS -> result.filter { it.duration >= 1800000 } // >= 30 mins
        }
        
        result = when (sort) {
            SortOption.DATE_ADDED_DESC -> result.sortedByDescending { it.dateAdded }
            SortOption.DATE_ADDED_ASC -> result.sortedBy { it.dateAdded }
            SortOption.NAME_ASC -> result.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> result.sortedByDescending { it.title.lowercase() }
            SortOption.SIZE_DESC -> result.sortedByDescending { it.size }
            SortOption.SIZE_ASC -> result.sortedBy { it.size }
            SortOption.DURATION_DESC -> result.sortedByDescending { it.duration }
            SortOption.DURATION_ASC -> result.sortedBy { it.duration }
        }
        
        return result
    }

    fun updateSortOption(option: SortOption) {
        _state.update { 
            it.copy(
                sortOption = option,
                videos = applySortAndFilter(it.allVideos, option, it.filterOption)
            )
        }
    }

    fun updateFilterOption(option: FilterOption) {
        _state.update { 
            it.copy(
                filterOption = option,
                videos = applySortAndFilter(it.allVideos, it.sortOption, option)
            )
        }
    }

    fun loadVideos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repository.getVideosWithMetadata().collectLatest { videos ->
                    _state.update {
                        it.copy(
                            allVideos = videos,
                            videos = applySortAndFilter(videos, it.sortOption, it.filterOption),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Error loading videos", e)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFavorite(video: Video) {
        viewModelScope.launch {
            repository.toggleFavorite(video.path, !video.isFavorite)
        }
    }

    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            if (repository.deleteVideo(video)) {
                loadVideos()
            }
        }
    }

    fun renameVideo(video: Video, newName: String) {
        viewModelScope.launch {
            if (repository.renameVideo(video, newName)) {
                loadVideos()
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
            _storageReport.value = storageAnalyzer.analyze(_state.value.allVideos)
        }
    }
    
    fun clearStorageReport() {
        _storageReport.value = null
    }

    fun updateFolderViewMode(mode: String) {
        _state.update { it.copy(folderViewMode = mode) }
        viewModelScope.launch {
            settingsRepository.setFolderViewMode(mode)
        }
    }
}
