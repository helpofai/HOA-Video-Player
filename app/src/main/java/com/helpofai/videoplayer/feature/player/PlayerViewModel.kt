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
package com.helpofai.videoplayer.feature.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.ffmpeg.FFmpegManager
import com.helpofai.videoplayer.core.playback.VideoPlayer
import com.helpofai.videoplayer.core.playback.AudioEffectManager
import com.helpofai.videoplayer.feature.scenedetection.SceneDetectionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import javax.inject.Inject
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val videoPlayer: VideoPlayer,
    val audioEffectManager: AudioEffectManager,
    private val repository: VideoRepository,
    private val ffmpegManager: FFmpegManager,
    private val sceneDetectionEngine: SceneDetectionEngine,
    private val qualityAnalyzerEngine: com.helpofai.videoplayer.feature.qualityanalyzer.QualityAnalyzerEngine,
    val preferencesUseCase: com.helpofai.videoplayer.feature.player.domain.PlayerPreferencesUseCase,
    private val playbackManager: com.helpofai.videoplayer.core.playback.MediaPlaybackManager,
    private val mediaAnalyzer: com.helpofai.videoplayer.core.playback.diagnostics.MediaAnalyzer,
    private val audioQualityAnalyzer: com.helpofai.videoplayer.core.playback.diagnostics.AudioQualityAnalyzer,
    val subtitleStyleManager: com.helpofai.videoplayer.core.media.SubtitleStyleManager,
    val videoEnhancementManager: com.helpofai.videoplayer.core.playback.diagnostics.VideoEnhancementManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val watchPartySessionManager = com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance()
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
        
    private val _currentPathFlow = MutableStateFlow<String?>(null)
    val currentPathFlow: StateFlow<String?> = _currentPathFlow.asStateFlow()
        
    private val _videoMetadata = MutableStateFlow<com.helpofai.videoplayer.core.database.entities.VideoMetadataEntity?>(null)
    val videoMetadata = _videoMetadata.asStateFlow()
        
    private val _playlist = MutableStateFlow<List<com.helpofai.videoplayer.core.model.Video>>(emptyList())
    val playlist = _playlist.asStateFlow()

    // Watch Party Permissions Flow
    val isPlayPauseAllowed = watchPartySessionManager.activeSession.map { session ->
        if (session == null || !watchPartySessionManager.isClientMode) true else session.allowPlayPause
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSeekAllowed = watchPartySessionManager.activeSession.map { session ->
        if (session == null || !watchPartySessionManager.isClientMode) true else session.allowSeek
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVolumeAllowed = watchPartySessionManager.activeSession.map { session ->
        if (session == null || !watchPartySessionManager.isClientMode) true else session.allowVolume
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isAudioTrackAllowed = watchPartySessionManager.activeSession.map { session ->
        if (session == null || !watchPartySessionManager.isClientMode) true else session.allowAudioTrack
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSubtitleToggleAllowed = watchPartySessionManager.activeSession.map { session ->
        if (session == null || !watchPartySessionManager.isClientMode) true else session.allowSubtitleToggle
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isGesturesAllowed = watchPartySessionManager.activeSession.map { session ->
        if (session == null || !watchPartySessionManager.isClientMode) true else session.allowGestures
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Holds the real video title for watch party clients (since path is "http_stream")
    private val _watchPartyVideoTitle = MutableStateFlow<String?>(null)
    val watchPartyVideoTitle = _watchPartyVideoTitle.asStateFlow()

    var isManuallyPaused: Boolean = false
    private var lastClientActionTime: Long = 0L

    fun onClientPlayPauseToggle(play: Boolean) {
        lastClientActionTime = System.currentTimeMillis()
        isManuallyPaused = !play
        val session = watchPartySessionManager.activeSession.value
        if (session != null) {
            watchPartySessionManager.sendPlaybackControl(play, videoPlayer.player.currentPosition)
        }
        if (play) videoPlayer.play() else videoPlayer.pause()
    }

    fun onClientSeekRequest(pos: Long) {
        lastClientActionTime = System.currentTimeMillis()
        val session = watchPartySessionManager.activeSession.value
        if (session != null) {
            watchPartySessionManager.sendPlaybackControl(videoPlayer.player.isPlaying, pos)
        }
        videoPlayer.seekTo(pos)
    }

    fun seekTo(pos: Long) {
        if (watchPartySessionManager.isClientMode) {
            onClientSeekRequest(pos)
        } else {
            videoPlayer.player.seekTo(pos)
        }
    }

    fun seekForward() {
        val newPos = videoPlayer.player.currentPosition + 10000L
        seekTo(newPos)
    }

    fun seekBack() {
        val newPos = (videoPlayer.player.currentPosition - 10000L).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun togglePlayPause() {
        val play = !videoPlayer.player.playWhenReady
        isManuallyPaused = !play
        if (watchPartySessionManager.isClientMode) {
            onClientPlayPauseToggle(play)
        } else {
            if (play) videoPlayer.play() else videoPlayer.pause()
        }
    }

    private val _mediaCompatibilityReport = MutableStateFlow<com.helpofai.videoplayer.core.playback.diagnostics.MediaAnalyzer.MediaCompatibilityReport?>(null)
    val mediaCompatibilityReport = _mediaCompatibilityReport.asStateFlow()

    private val _audioQualityReport = MutableStateFlow<com.helpofai.videoplayer.core.playback.diagnostics.AudioQualityAnalyzer.AudioQualityReport?>(null)
    val audioQualityReport = _audioQualityReport.asStateFlow()
    
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

    val backgroundPlaybackEnabled = preferencesUseCase.backgroundPlaybackEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val longPressBoostSpeed = preferencesUseCase.longPressBoostSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.0f)

    init {
        val videoUriString = savedStateHandle.get<String>("videoUri")
        val videoPathString = savedStateHandle.get<String>("path")
        
        if (videoUriString != null) {
            val originalUri = Uri.parse(Uri.decode(videoUriString))
            val watchPartySessionManager = com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance()
            watchPartySessionManager.setFullPlayerActive(true)
            videoPlayer.player.volume = 1f
            val activeSession = watchPartySessionManager.activeSession.value
            
            val isClient = watchPartySessionManager.isClientMode && activeSession != null
            // Client always streams from host HTTP server; host plays its local file
            val uri = if (isClient) {
                if (originalUri.scheme == "http" && originalUri.path?.contains("/video") == true) {
                    originalUri
                } else {
                    val hostVideo = activeSession.video
                    val videoId = hostVideo?.id ?: 9999L
                    Uri.parse("http://${activeSession.hostIp}:${activeSession.port}/video?v=$videoId&t=${System.currentTimeMillis()}")
                }
            } else {
                originalUri
            }

            val title = if (isClient) {
                activeSession.video?.title ?: "Watch Party"
            } else {
                null
            }            
            currentVideoPath = if (isClient) "http_stream" else (videoPathString?.let { Uri.decode(it) } ?: uri.path)
            _currentPathFlow.value = currentVideoPath

            // For client mode, show the real video title from the host session
            if (isClient && activeSession.video != null) {
                _watchPartyVideoTitle.value = activeSession.video.title
            }
            
            viewModelScope.launch {
                val finalUri = uri
                val finalPath = currentVideoPath ?: "http_stream"
                
                val meta = if (isClient) null else (finalPath.takeIf { it != "http_stream" }?.let { repository.getMetadata(it) })
                _videoMetadata.value = meta
                _abRepeatStart.value = meta?.abRepeatStart
                _abRepeatEnd.value = meta?.abRepeatEnd
                
                if (meta != null && meta.lastPlayedPosition > 0L) {
                    val duration = meta.totalDuration
                    if (duration == 0L || meta.lastPlayedPosition < duration - 5000L) {
                        resumePosition = meta.lastPlayedPosition
                    }
                }
                
                if (!isClient) {
                    playbackManager.prepareVideo(
                        path = finalPath,
                        uri = finalUri,
                        meta = meta,
                        resumePosition = 0L,
                        preferredSpeed = preferencesUseCase.getPreferredPlaybackSpeed(),
                        preferredSubtitleLang = preferencesUseCase.getPreferredSubtitleLanguage(),
                        zoomLevel = meta?.zoomLevel ?: 1.0f,
                        playWhenReady = true
                    )
                }

                // Client-side: Watch Party active session video change observer
                if (isClient) {
                    launch {
                        var lastVideoId: String? = null
                        watchPartySessionManager.activeSession.collect { session ->
                            val currentVideo = session?.video
                            if (currentVideo != null) {
                                _watchPartyVideoTitle.value = currentVideo.title
                                val currentVideoId = currentVideo.id.toString()
                                if (currentVideoId != lastVideoId) {
                                    lastVideoId = currentVideoId
                                    val streamUri = Uri.parse("http://${session.hostIp}:${session.port}/video?v=${currentVideoId}&t=${System.currentTimeMillis()}")
                                    playbackManager.prepareVideo(
                                        path = "http_stream",
                                        uri = streamUri,
                                        meta = null,
                                        resumePosition = session.currentPositionMs,
                                        preferredSpeed = 1.0f,
                                        preferredSubtitleLang = null,
                                        zoomLevel = 1.0f,
                                        playWhenReady = session.isPlaying
                                    )
                                }
                            } else {
                                _watchPartyVideoTitle.value = "Waiting for host..."
                                lastVideoId = null
                                videoPlayer.pause()
                            }
                        }
                    }
                }

                // Host-Player Sync Binding: push play position to session every second
                launch {
                    while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                        val session = watchPartySessionManager.activeSession.value
                        if (session != null && !watchPartySessionManager.isClientMode) {
                            watchPartySessionManager.updatePlaybackState(
                                positionMs = videoPlayer.player.currentPosition,
                                isPlaying = videoPlayer.player.isPlaying
                            )
                        }
                        kotlinx.coroutines.delay(1000)
                    }
                }

                // Auto-sync playing video when in Host Sync Mode
                launch {
                    combine(
                        watchPartySessionManager.isSyncModeEnabled,
                        _currentPathFlow,
                        _playlist
                    ) { enabled, path, list -> Triple(enabled, path, list) }
                    .collect { (enabled, path, list) ->
                        if (enabled && !watchPartySessionManager.isClientMode && path != null && path != "http_stream") {
                            val currentVideo = list.firstOrNull { it.path == path } ?: com.helpofai.videoplayer.core.model.Video(
                                id = path.hashCode().toLong(),
                                uri = android.net.Uri.fromFile(java.io.File(path)),
                                title = java.io.File(path).name,
                                duration = 0L,
                                size = 0L,
                                dateAdded = 0L,
                                path = path
                            )
                            if (watchPartySessionManager.currentStreamingVideo.value?.path != path) {
                                watchPartySessionManager.setStreamingVideo(currentVideo)
                            }
                        }
                    }
                }

                // Client synchronization handler: sync play state and seek position dynamically
                launch {
                    combine(
                        watchPartySessionManager.activeSession,
                        videoPlayer.playbackState
                    ) { session, exoState -> Pair(session, exoState) }
                    .collect { (session, exoState) ->
                        if (session != null && watchPartySessionManager.isClientMode) {
                            val state = exoState.playbackState
                            val isIdle = state == androidx.media3.common.Player.STATE_IDLE
                            val isEnded = state == androidx.media3.common.Player.STATE_ENDED
                            if (isIdle || isEnded) return@collect

                            if (session.isPlaying && !exoState.isPlaying) {
                                videoPlayer.play()
                            } else if (!session.isPlaying && exoState.isPlaying) {
                                videoPlayer.pause()
                            }

                            val isActionTimeout = System.currentTimeMillis() - lastClientActionTime > 2500L
                            if (isActionTimeout) {
                                val drift = kotlin.math.abs(exoState.currentPosition - session.currentPositionMs)
                                if (drift > 1500L) {
                                    videoPlayer.seekTo(session.currentPositionMs)
                                }
                            }
                        }
                    }
                }

                // Host-side: Observe client control requests (only when allowPlayPause and allowSeek permissions are granted)
                if (!isClient) {
                    launch {
                        watchPartySessionManager.playbackCommands.collect { cmd ->
                            val activeSession = watchPartySessionManager.activeSession.value
                            if (activeSession != null) {
                                if (activeSession.allowPlayPause) {
                                    if (cmd.isPlaying && !videoPlayer.player.isPlaying) {
                                        videoPlayer.play()
                                    } else if (!cmd.isPlaying && videoPlayer.player.isPlaying) {
                                        videoPlayer.pause()
                                    }
                                }
                                if (activeSession.allowSeek) {
                                    val drift = kotlin.math.abs(videoPlayer.player.currentPosition - cmd.positionMs)
                                    if (drift > 1500L) {
                                        videoPlayer.player.seekTo(cmd.positionMs)
                                    }
                                }
                            }
                        }
                    }
                }

                // Trigger lightweight offline diagnostics (skip for HTTP stream clients)
                if (!isClient) {
                    launch(Dispatchers.IO) {
                        val report = mediaAnalyzer.analyzeMedia(finalUri, finalPath)
                        _mediaCompatibilityReport.value = report
                        _audioQualityReport.value = audioQualityAnalyzer.analyzeAudioTrack(
                            codec = report.audioCodec,
                            channels = report.audioChannels,
                            sampleRate = report.audioSampleRate,
                            bitrateBps = report.totalBitrateBps
                        )
                        videoEnhancementManager.applyPreset(videoEnhancementManager.config.value.preset, report)
                    }
                }

                launch {
                    if (isClient) {
                        combine(
                            watchPartySessionManager.activeSession,
                            repository.getVideosWithMetadata()
                        ) { session, allVideos -> Pair(session, allVideos) }
                        .collect { (session, allVideos) ->
                            if (session != null) {
                                if (session.allowFolderQueue) {
                                    val hostFolder = session.video?.path?.let { java.io.File(it).parentFile?.name }
                                    val folderVideos = allVideos.filter { 
                                        (java.io.File(it.path).parentFile?.name ?: "") == hostFolder 
                                    }.sortedBy { it.title }
                                    _playlist.value = folderVideos
                                } else {
                                    val currentStreamVideo = session.video?.let { v ->
                                        val streamUri = Uri.parse("http://${session.hostIp}:${session.port}/video?v=${v.id}&t=${System.currentTimeMillis()}")
                                        v.copy(uri = streamUri, path = "http_stream")
                                    }
                                    _playlist.value = if (currentStreamVideo != null) listOf(currentStreamVideo) else emptyList()
                                }
                            }
                        }
                    } else {
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
            }
        }
        
        startABRepeatLoop()
        startMidRollAdChecker()
        observePlaybackState()
    }
    
    fun onResumeAccepted() {
        if (!videoPlayer.isReleased && resumePosition > 0L) {
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
                if (!videoPlayer.isReleased) {
                    val start = _abRepeatStart.value
                    val end = _abRepeatEnd.value
                    if (start != null && end != null && start < end && (start > 0 || end - start > 5000)) {
                        if (videoPlayer.player.currentPosition >= end) {
                            videoPlayer.player.seekTo(start)
                        }
                    }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    private fun startMidRollAdChecker() {
        viewModelScope.launch {
            while (isActive) {
                if (!videoPlayer.isReleased && videoPlayer.player.isPlaying) {
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
                if (watchPartySessionManager.isClientMode) return@collect
                if (state.playbackState == androidx.media3.common.Player.STATE_READY && !hasShownResumePromptForCurrentVideo) {
                    hasShownResumePromptForCurrentVideo = true
                    if (resumePosition > 0L) {
                        showResumePrompt = true
                        
                        // Auto-hide after 8 seconds
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(8000)
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
        isManuallyPaused = false
        viewModelScope.launch {
            // Record current playback position before switching
            currentVideoPath?.let { oldPath ->
                playbackManager.recordPlaybackState(oldPath, lastZoomLevel)
            }
            
            currentVideoPath = path
            _currentPathFlow.value = path
            val uri = Uri.fromFile(java.io.File(path))
            
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
            
            playbackManager.prepareVideo(
                path = path,
                uri = uri,
                meta = meta,
                resumePosition = 0L, // Start playing from 0:00
                preferredSpeed = meta?.playbackSpeed ?: preferencesUseCase.getPreferredPlaybackSpeed(),
                preferredSubtitleLang = meta?.subtitleTrackLanguage ?: preferencesUseCase.getPreferredSubtitleLanguage(),
                zoomLevel = meta?.zoomLevel ?: 1.0f
            )
            
            // Trigger lightweight offline diagnostics once
            launch(Dispatchers.IO) {
                val report = mediaAnalyzer.analyzeMedia(uri, path)
                _mediaCompatibilityReport.value = report
                _audioQualityReport.value = audioQualityAnalyzer.analyzeAudioTrack(
                    codec = report.audioCodec,
                    channels = report.audioChannels,
                    sampleRate = report.audioSampleRate,
                    bitrateBps = report.totalBitrateBps
                )
                videoEnhancementManager.applyPreset(videoEnhancementManager.config.value.preset, report)
            }
            
            _zoomLevel.value = meta?.zoomLevel ?: 1.0f
            
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



    private var isMinimizing = false

    fun minimizePlayer() {
        isMinimizing = true
        
        // Prepare mini-player data
        val videoUriString = savedStateHandle.get<String>("videoUri")
        val videoUri = if (videoUriString != null) android.net.Uri.parse(android.net.Uri.decode(videoUriString)) else android.net.Uri.EMPTY
        val videoPath = savedStateHandle.get<String>("path")?.let { android.net.Uri.decode(it) } ?: videoUri.path ?: ""
        val videoTitle = _watchPartyVideoTitle.value ?: java.io.File(videoPath).name
        
        val currentVideo = com.helpofai.videoplayer.core.model.Video(
            id = videoPath.hashCode().toLong(),
            uri = videoUri,
            title = videoTitle,
            duration = videoPlayer.player.duration,
            size = 0L,
            dateAdded = 0L,
            path = videoPath
        )
        com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance().showMiniPlayer(currentVideo)
        
        // Continue playing in the background
        videoPlayer.play()
    }

    override fun onCleared() {
        super.onCleared()
        com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance().setFullPlayerActive(false)
        val path = currentVideoPath
        if (path != null) {
            viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
                playbackManager.recordPlaybackState(path, lastZoomLevel)
            }
        }
        
        if (!isMinimizing) {
            // Stop and release player normally
            videoPlayer.release()
            com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance().dismissMiniPlayer()
        }
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
        val positionMs = if (!videoPlayer.isReleased) videoPlayer.player.currentPosition else 0L
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
