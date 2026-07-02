package com.helpofai.videoplayer.feature.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.ffmpeg.FFmpegManager
import com.helpofai.videoplayer.core.playback.VideoPlayer
import com.helpofai.videoplayer.core.scanner.ScannerSmartChapterGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import javax.inject.Inject

import com.helpofai.videoplayer.core.playback.AudioEffectManager

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val videoPlayer: VideoPlayer,
    val audioEffectManager: AudioEffectManager,
    private val repository: VideoRepository,
    private val settingsRepository: com.helpofai.videoplayer.core.data.SettingsRepository,
    private val ffmpegManager: FFmpegManager,
    private val smartChapterGenerator: ScannerSmartChapterGenerator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var currentVideoPath: String? = null
        private set
        
    private val _currentPathFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        
    private val _playlist = kotlinx.coroutines.flow.MutableStateFlow<List<com.helpofai.videoplayer.core.model.Video>>(emptyList())
    val playlist = _playlist.asStateFlow()
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bookmarks = _currentPathFlow.flatMapLatest { path ->
        if (path != null) repository.getBookmarksForVideo(path)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _abRepeatStart = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val abRepeatStart = _abRepeatStart.asStateFlow()

    private val _abRepeatEnd = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val abRepeatEnd = _abRepeatEnd.asStateFlow()
    
    private var abRepeatJob: kotlinx.coroutines.Job? = null

    val backgroundPlaybackEnabled = settingsRepository.backgroundPlayback.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        val videoUriString = savedStateHandle.get<String>("videoUri")
        val videoPathString = savedStateHandle.get<String>("path")
        
        if (videoUriString != null) {
            val uri = Uri.parse(Uri.decode(videoUriString))
            currentVideoPath = videoPathString?.let { Uri.decode(it) } ?: uri.path
            _currentPathFlow.value = currentVideoPath
            
            viewModelScope.launch {
                var subtitleConfigs = currentVideoPath?.let { path ->
                    findSubtitlesForVideo(path)
                } ?: emptyList()

                val fileName = currentVideoPath?.let { java.io.File(it).name } ?: "Video"
                
                val meta = currentVideoPath?.let { repository.getMetadata(it) }
                _abRepeatStart.value = meta?.abRepeatStart
                _abRepeatEnd.value = meta?.abRepeatEnd
                
                if (meta?.externalSubtitleUri != null) {
                    val extUri = Uri.parse(meta.externalSubtitleUri)
                    val extConfig = MediaItem.SubtitleConfiguration.Builder(extUri)
                        .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("ext")
                        .setLabel("External Subtitle")
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .build()
                    subtitleConfigs = (subtitleConfigs + extConfig).toMutableList()
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setSubtitleConfigurations(subtitleConfigs)
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(fileName).build())
                    .build()

                videoPlayer.prepare(mediaItem)
                
                val speed = settingsRepository.defaultPlaybackSpeed.first()
                videoPlayer.player.setPlaybackSpeed(speed)
                
                val subtitleLang = settingsRepository.defaultSubtitleLanguage.first()
                if (subtitleLang != "Off") {
                    videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                        .buildUpon()
                        .setPreferredTextLanguage(subtitleLang)
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                        .build()
                } else {
                    videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                        .build()
                }
                
                // Fetch Playlist (videos in same folder)
                repository.getVideosWithMetadata().collect { allVideos ->
                    val currentParent = currentVideoPath?.let { java.io.File(it).parent }
                    if (currentParent != null) {
                        val folderVideos = allVideos.filter { java.io.File(it.path).parent == currentParent }
                            .sortedBy { it.title }
                        _playlist.value = folderVideos
                    }
                }
            }
        }
        
        startABRepeatLoop()
        observePlaybackState()
    }
    
    private fun startABRepeatLoop() {
        abRepeatJob?.cancel()
        abRepeatJob = viewModelScope.launch {
            while (isActive) {
                val start = _abRepeatStart.value
                val end = _abRepeatEnd.value
                if (start != null && end != null && start < end) {
                    if (videoPlayer.player.currentPosition >= end) {
                        videoPlayer.player.seekTo(start)
                    }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    private fun observePlaybackState() {
        viewModelScope.launch {
            videoPlayer.playbackState.collect { state ->
                if (state.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    playNextVideo()
                }
            }
        }
    }
    
    fun setABRepeatStart(timeMs: Long?) {
        _abRepeatStart.value = timeMs
        if (timeMs == null) {
            _abRepeatEnd.value = null
        }
        currentVideoPath?.let { path ->
            viewModelScope.launch {
                repository.updateABRepeat(path, _abRepeatStart.value, _abRepeatEnd.value)
            }
        }
    }

    fun setABRepeatEnd(timeMs: Long?) {
        _abRepeatEnd.value = timeMs
        currentVideoPath?.let { path ->
            viewModelScope.launch {
                repository.updateABRepeat(path, _abRepeatStart.value, _abRepeatEnd.value)
            }
        }
    }
    
    fun addExternalSubtitle(uri: Uri?) {
        if (uri == null) return
        val path = currentVideoPath ?: return
        
        viewModelScope.launch {
            repository.updateExternalSubtitle(path, uri.toString())
            
            val currentItem = videoPlayer.player.currentMediaItem ?: return@launch
            val currentPos = videoPlayer.player.currentPosition
            
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("ext")
                .setLabel("External Subtitle")
                .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                .build()
                
            val newConfigs = currentItem.localConfiguration?.subtitleConfigurations?.toMutableList() ?: mutableListOf()
            newConfigs.add(subtitleConfig)
            
            val newItem = currentItem.buildUpon()
                .setSubtitleConfigurations(newConfigs)
                .build()
                
            videoPlayer.player.setMediaItem(newItem, currentPos)
            videoPlayer.player.prepare()
            videoPlayer.player.play()
        }
    }
    
    fun playVideo(path: String) {
        viewModelScope.launch {
            // Record current playback position before switching
            currentVideoPath?.let { oldPath ->
                val position = videoPlayer.player.currentPosition
                repository.recordPlayback(oldPath, position)
            }
            
            currentVideoPath = path
            _currentPathFlow.value = path
            val uri = Uri.fromFile(java.io.File(path))
            
            var subtitleConfigs = findSubtitlesForVideo(path).toMutableList()
            
            val fileName = java.io.File(path).name
            
            val meta = repository.getMetadata(path)
            _abRepeatStart.value = meta?.abRepeatStart
            _abRepeatEnd.value = meta?.abRepeatEnd
            
            if (meta?.externalSubtitleUri != null) {
                val extUri = Uri.parse(meta.externalSubtitleUri)
                val extConfig = MediaItem.SubtitleConfiguration.Builder(extUri)
                    .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage("ext")
                    .setLabel("External Subtitle")
                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                    .build()
                subtitleConfigs.add(extConfig)
            }
            
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setSubtitleConfigurations(subtitleConfigs)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(fileName).build())
                .build()
                
            videoPlayer.prepare(mediaItem)
            
            val speed = settingsRepository.defaultPlaybackSpeed.first()
            videoPlayer.player.setPlaybackSpeed(speed)
            
            val subtitleLang = settingsRepository.defaultSubtitleLanguage.first()
            if (subtitleLang != "Off") {
                videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                    .buildUpon()
                    .setPreferredTextLanguage(subtitleLang)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                    .build()
            } else {
                videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                    .build()
            }
            
            videoPlayer.play()
        }
    }

    fun playNextVideo() {
        val currentPlaylist = _playlist.value
        val currentIndex = currentPlaylist.indexOfFirst { it.path == currentVideoPath }
        if (currentIndex != -1 && currentIndex < currentPlaylist.size - 1) {
            playVideo(currentPlaylist[currentIndex + 1].path)
        }
    }
    
    fun playPrevVideo() {
        val currentPlaylist = _playlist.value
        val currentIndex = currentPlaylist.indexOfFirst { it.path == currentVideoPath }
        if (currentIndex > 0) {
            playVideo(currentPlaylist[currentIndex - 1].path)
        }
    }
    
    fun reorderPlaylist(from: Int, to: Int) {
        val current = _playlist.value.toMutableList()
        val item = current.removeAt(from)
        current.add(to, item)
        _playlist.value = current
    }

    private suspend fun findSubtitlesForVideo(videoPath: String): List<MediaItem.SubtitleConfiguration> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val subtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()
        try {
            val videoFile = java.io.File(videoPath)
            val parentDir = videoFile.parentFile
            val videoBaseName = videoFile.nameWithoutExtension

            if (parentDir != null && parentDir.exists() && parentDir.isDirectory) {
                val subtitleFiles = parentDir.listFiles { file ->
                    file.isFile && file.name.startsWith(videoBaseName) && 
                    (file.name.endsWith(".srt", true) || 
                     file.name.endsWith(".vtt", true) ||
                     file.name.endsWith(".ass", true) ||
                     file.name.endsWith(".ssa", true))
                }

                subtitleFiles?.forEach { subFile ->
                    val extension = subFile.extension.lowercase()
                    val mimeType = when (extension) {
                        "srt" -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                        "vtt" -> androidx.media3.common.MimeTypes.TEXT_VTT
                        "ass", "ssa" -> androidx.media3.common.MimeTypes.TEXT_SSA
                        else -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                    }

                    // Attempt to extract language code if formatted like "video.en.srt"
                    val nameParts = subFile.nameWithoutExtension.split(".")
                    val lang = if (nameParts.size > 1) nameParts.last() else "und"

                    val config = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                        .setMimeType(mimeType)
                        .setLanguage(lang)
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .setLabel(subFile.name)
                        .build()
                    
                    subtitleConfigs.add(config)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        subtitleConfigs
    }

    override fun onCleared() {
        super.onCleared()
        // Save playback position
        currentVideoPath?.let { path ->
            val position = videoPlayer.player.currentPosition
            viewModelScope.launch {
                repository.recordPlayback(path, position)
            }
        }
        videoPlayer.release()
    }
    
    fun addBookmark(timeMs: Long, label: String) {
        currentVideoPath?.let { path ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.addBookmark(path, timeMs, label)
            }
        }
    }
    
    fun deleteBookmark(bookmark: com.helpofai.videoplayer.core.database.entities.BookmarkEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.deleteBookmark(bookmark)
        }
    }

    fun takeScreenshot(context: android.content.Context, onSuccess: (String) -> Unit) {
        val path = currentVideoPath ?: return
        val positionMs = videoPlayer.player.currentPosition
        viewModelScope.launch {
            val fileName = "Screenshot_${System.currentTimeMillis()}.jpg"
            val destFile = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName)
            val success = ffmpegManager.takeScreenshot(path, destFile.absolutePath, positionMs)
            if (success) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(destFile.absolutePath)
                }
            }
        }
    }

    fun generateAutoChapters(onStart: () -> Unit, onComplete: (Boolean) -> Unit) {
        val path = currentVideoPath ?: return
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onStart() }
            val success = smartChapterGenerator.generateChapters(path)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onComplete(success) }
        }
    }
}
