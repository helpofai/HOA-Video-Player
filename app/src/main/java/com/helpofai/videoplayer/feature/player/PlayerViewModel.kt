package com.helpofai.videoplayer.feature.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.ffmpeg.FFmpegManager
import com.helpofai.videoplayer.core.playback.VideoPlayer
import com.helpofai.videoplayer.feature.scenedetection.SceneDetectionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import javax.inject.Inject

import com.helpofai.videoplayer.core.playback.AudioEffectManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val videoPlayer: VideoPlayer,
    val audioEffectManager: AudioEffectManager,
    private val repository: VideoRepository,
    private val settingsRepository: com.helpofai.videoplayer.core.data.SettingsRepository,
    private val ffmpegManager: FFmpegManager,
    private val sceneDetectionEngine: SceneDetectionEngine,
    private val qualityAnalyzerEngine: com.helpofai.videoplayer.feature.qualityanalyzer.QualityAnalyzerEngine,
    val learningEngine: com.helpofai.videoplayer.feature.learning.OfflineLearningEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Legacy Resume Prompt state
    var showResumePrompt by mutableStateOf(false)
        private set
    var resumePosition: Long = 0L
        private set
    var lastSavedPosition: Long = 0L
        private set
        
    // Mid-Roll Ads State
    private val shownAdsMap = mutableSetOf<Int>()
    private val _showMidRollAdEvent = MutableSharedFlow<Unit>()
    val showMidRollAdEvent = _showMidRollAdEvent.asSharedFlow()

    var currentVideoPath: String? = null
        private set
        
    private val _currentPathFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        
    private val _videoMetadata = kotlinx.coroutines.flow.MutableStateFlow<com.helpofai.videoplayer.core.database.entities.VideoMetadataEntity?>(null)
    val videoMetadata = _videoMetadata.asStateFlow()
        
    private val _playlist = kotlinx.coroutines.flow.MutableStateFlow<List<com.helpofai.videoplayer.core.model.Video>>(emptyList())
    val playlist = _playlist.asStateFlow()
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bookmarks = _currentPathFlow.flatMapLatest { path ->
        if (path != null) repository.getBookmarksForVideo(path)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _abRepeatStart = MutableStateFlow<Long?>(null)
    val abRepeatStart = _abRepeatStart.asStateFlow()
    
    private val _abRepeatEnd = MutableStateFlow<Long?>(null)
    val abRepeatEnd = _abRepeatEnd.asStateFlow()
    
    private val _zoomLevel = MutableStateFlow(1.0f)
    val zoomLevel = _zoomLevel.asStateFlow()
    
    var lastZoomLevel: Float = 1.0f
    
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
                _videoMetadata.value = meta
                _abRepeatStart.value = meta?.abRepeatStart
                _abRepeatEnd.value = meta?.abRepeatEnd
                
                if (meta != null && meta.lastPlayedPosition > 0L) {
                    val duration = meta.totalDuration
                    // Do not prompt if watched till the very end (within 5 seconds)
                    if (duration == 0L || meta.lastPlayedPosition < duration - 5000L) {
                        resumePosition = meta.lastPlayedPosition
                        // We will show the prompt in observePlaybackState when the video is actually ready
                    }
                }
                
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
                
                val speed = learningEngine.getPreferredPlaybackSpeed()
                videoPlayer.player.setPlaybackSpeed(speed)
                
                val subtitleLang = learningEngine.getPreferredSubtitleLanguage()
                if (subtitleLang != null && subtitleLang != "Off") {
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
        startMidRollAdChecker()
        observePlaybackState()
    }
    
    fun onResumeAccepted() {
        if (resumePosition > 0L) {
            videoPlayer.player.seekTo(resumePosition)
        }
        showResumePrompt = false
    }

    fun onResumeDismissed() {
        showResumePrompt = false
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
    
    private fun startMidRollAdChecker() {
        viewModelScope.launch {
            while (isActive) {
                if (videoPlayer.player.isPlaying) {
                    val duration = videoPlayer.player.duration
                    if (duration > 30 * 60 * 1000L) { // > 30 mins
                        val currentPos = videoPlayer.player.currentPosition
                        val milestone50 = (duration * 0.5).toLong()
                        val milestone90 = (duration * 0.9).toLong()
                        
                        if (currentPos >= milestone50 && !shownAdsMap.contains(50)) {
                            shownAdsMap.add(50)
                            _showMidRollAdEvent.emit(Unit)
                        } else if (currentPos >= milestone90 && !shownAdsMap.contains(90)) {
                            shownAdsMap.add(90)
                            _showMidRollAdEvent.emit(Unit)
                        }
                    }
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    private var hasShownResumePromptForCurrentVideo = false
    
    private fun observePlaybackState() {
        viewModelScope.launch {
            videoPlayer.playbackState.collect { state ->
                if (state.playbackState == androidx.media3.common.Player.STATE_READY && !hasShownResumePromptForCurrentVideo) {
                    hasShownResumePromptForCurrentVideo = true
                    if (resumePosition > 0L) {
                        showResumePrompt = true
                        
                        // Auto-hide after 10 seconds
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(10000)
                            if (showResumePrompt) {
                                showResumePrompt = false
                            }
                        }
                    }
                } else if (state.playbackState == androidx.media3.common.Player.STATE_ENDED) {
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
                val speed = videoPlayer.player.playbackParameters.speed
                val audioLang = videoPlayer.player.trackSelectionParameters.preferredAudioLanguages.firstOrNull()
                val subLang = videoPlayer.player.trackSelectionParameters.preferredTextLanguages.firstOrNull()
                repository.recordPlayback(oldPath, position, speed, audioLang, subLang, lastZoomLevel)
            }
            
            currentVideoPath = path
            _currentPathFlow.value = path
            val uri = Uri.fromFile(java.io.File(path))
            
            var subtitleConfigs = findSubtitlesForVideo(path).toMutableList()
            
            val fileName = java.io.File(path).name
            
            val meta = repository.getMetadata(path)
            _videoMetadata.value = meta
            _abRepeatStart.value = meta?.abRepeatStart
            _abRepeatEnd.value = meta?.abRepeatEnd
            
            showResumePrompt = false
            hasShownResumePromptForCurrentVideo = false
            resumePosition = 0L
            
            if (meta != null && meta.lastPlayedPosition > 0L) {
                val duration = meta.totalDuration
                if (duration == 0L || meta.lastPlayedPosition < duration - 5000L) {
                    resumePosition = meta.lastPlayedPosition
                }
            }
            
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
            
            if (meta != null && meta.lastPlayedPosition > 0L) {
                videoPlayer.player.seekTo(meta.lastPlayedPosition)
            }
            
            _zoomLevel.value = meta?.zoomLevel ?: 1.0f
            
            val speed = meta?.playbackSpeed ?: learningEngine.getPreferredPlaybackSpeed()
            videoPlayer.player.setPlaybackSpeed(speed)
            
            val subtitleLang = meta?.subtitleTrackLanguage ?: learningEngine.getPreferredSubtitleLanguage()
            if (subtitleLang != "Off" && subtitleLang != null) {
                videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                    .buildUpon()
                    .setPreferredTextLanguage(subtitleLang)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                    .build()
            } else if (subtitleLang == "Off" || subtitleLang == null) {
                videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                    .build()
            }
            
            if (meta?.audioTrackLanguage != null) {
                videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguage(meta.audioTrackLanguage)
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
            val speed = videoPlayer.player.playbackParameters.speed
            val audioLang = videoPlayer.player.trackSelectionParameters.preferredAudioLanguages.firstOrNull()
            val subLang = videoPlayer.player.trackSelectionParameters.preferredTextLanguages.firstOrNull()
            
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch {
                repository.recordPlayback(path, position, speed, audioLang, subLang, lastZoomLevel)
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
            val success = sceneDetectionEngine.generateScenes(path)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onComplete(success) }
        }
    }

    private val _qualityReport = MutableStateFlow<com.helpofai.videoplayer.feature.qualityanalyzer.QualityReport?>(null)
    val qualityReport = _qualityReport.asStateFlow()
    
    private val _isAnalyzingQuality = MutableStateFlow(false)
    val isAnalyzingQuality = _isAnalyzingQuality.asStateFlow()

    fun analyzeVideoQuality() {
        val path = currentVideoPath ?: return
        viewModelScope.launch {
            _isAnalyzingQuality.value = true
            _qualityReport.value = null
            
            val report = qualityAnalyzerEngine.analyze(path)
            
            _qualityReport.value = report
            _isAnalyzingQuality.value = false
        }
    }
    
    fun clearQualityReport() {
        _qualityReport.value = null
    }
}
