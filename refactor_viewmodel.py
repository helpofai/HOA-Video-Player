import os
import re

file_path = r"C:\Users\rajib\Desktop\vidplay\app\src\main\java\com\helpofai\videoplayer\feature\player\PlayerViewModel.kt"
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update Constructor
old_constructor = """@HiltViewModel
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
) : ViewModel() {"""

new_constructor = """@HiltViewModel
class PlayerViewModel @Inject constructor(
    val videoPlayer: VideoPlayer,
    val audioEffectManager: AudioEffectManager,
    private val repository: VideoRepository,
    private val ffmpegManager: FFmpegManager,
    private val sceneDetectionEngine: SceneDetectionEngine,
    private val qualityAnalyzerEngine: com.helpofai.videoplayer.feature.qualityanalyzer.QualityAnalyzerEngine,
    val preferencesUseCase: com.helpofai.videoplayer.feature.player.domain.PlayerPreferencesUseCase,
    private val playbackManager: com.helpofai.videoplayer.core.playback.MediaPlaybackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {"""

content = content.replace(old_constructor, new_constructor)

# 2. Update backgroundPlaybackEnabled and longPressBoostSpeed
old_settings = """    val backgroundPlaybackEnabled = settingsRepository.backgroundPlayback.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val longPressBoostSpeed = settingsRepository.longPressBoostSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.0f)"""

new_settings = """    val backgroundPlaybackEnabled = preferencesUseCase.backgroundPlaybackEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val longPressBoostSpeed = preferencesUseCase.longPressBoostSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.0f)"""

content = content.replace(old_settings, new_settings)

# 3. Replace init block
old_init_regex = r"                var subtitleConfigs = currentVideoPath\?\.let \{ path ->.*?repository\.getVideosWithMetadata\(\)\.collect \{ allVideos ->"
new_init = """                val meta = currentVideoPath?.let { repository.getMetadata(it) }
                _videoMetadata.value = meta
                _abRepeatStart.value = meta?.abRepeatStart
                _abRepeatEnd.value = meta?.abRepeatEnd
                
                if (meta != null && meta.lastPlayedPosition > 0L) {
                    val duration = meta.totalDuration
                    if (duration == 0L || meta.lastPlayedPosition < duration - 5000L) {
                        resumePosition = meta.lastPlayedPosition
                    }
                }
                
                if (currentVideoPath != null) {
                    playbackManager.prepareVideo(
                        path = currentVideoPath!!,
                        uri = uri,
                        meta = meta,
                        resumePosition = resumePosition,
                        preferredSpeed = preferencesUseCase.getPreferredPlaybackSpeed(),
                        preferredSubtitleLang = preferencesUseCase.getPreferredSubtitleLanguage(),
                        zoomLevel = meta?.zoomLevel ?: 1.0f
                    )
                }

                repository.getVideosWithMetadata().collect { allVideos ->"""

content = re.sub(old_init_regex, new_init, content, flags=re.DOTALL)

# 4. Replace playVideo block
old_playvideo_regex = r"            var subtitleConfigs = findSubtitlesForVideo\(path\)\.toMutableList\(\).*?videoPlayer\.play\(\)"
new_playvideo = """            val meta = repository.getMetadata(path)
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
                resumePosition = resumePosition,
                preferredSpeed = meta?.playbackSpeed ?: preferencesUseCase.getPreferredPlaybackSpeed(),
                preferredSubtitleLang = meta?.subtitleTrackLanguage ?: preferencesUseCase.getPreferredSubtitleLanguage(),
                zoomLevel = meta?.zoomLevel ?: 1.0f
            )
            
            _zoomLevel.value = meta?.zoomLevel ?: 1.0f
            
            videoPlayer.play()"""

content = re.sub(old_playvideo_regex, new_playvideo, content, flags=re.DOTALL)

# 5. Remove findSubtitlesForVideo
content = re.sub(r"    private suspend fun findSubtitlesForVideo\(videoPath: String\): List<MediaItem\.SubtitleConfiguration> = kotlinx\.coroutines\.withContext\(kotlinx\.coroutines\.Dispatchers\.IO\) \{.*?    \}\n\n", "", content, flags=re.DOTALL)

# 6. Update onCleared
old_oncleared = """    override fun onCleared() {
        super.onCleared()
        // Save playback position using NonCancellable so the coroutine survives viewModelScope cancellation
        currentVideoPath?.let { path ->
            val position = videoPlayer.player.currentPosition
            val speed = videoPlayer.player.playbackParameters.speed
            val audioLang = videoPlayer.player.trackSelectionParameters.preferredAudioLanguages.firstOrNull()
            val subLang = videoPlayer.player.trackSelectionParameters.preferredTextLanguages.firstOrNull()
            viewModelScope.launch(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
                repository.recordPlayback(path, position, speed, audioLang, subLang, lastZoomLevel)
            }
        }
        videoPlayer.release()
    }"""
    
new_oncleared = """    override fun onCleared() {
        super.onCleared()
        currentVideoPath?.let { path ->
            viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
                playbackManager.recordPlaybackState(path, lastZoomLevel)
            }
        }
        videoPlayer.release()
    }"""

content = content.replace(old_oncleared, new_oncleared)

# 7. Update recordPlayback in playVideo
old_record_playback = """            currentVideoPath?.let { oldPath ->
                val position = videoPlayer.player.currentPosition
                val speed = videoPlayer.player.playbackParameters.speed
                val audioLang = videoPlayer.player.trackSelectionParameters.preferredAudioLanguages.firstOrNull()
                val subLang = videoPlayer.player.trackSelectionParameters.preferredTextLanguages.firstOrNull()
                repository.recordPlayback(oldPath, position, speed, audioLang, subLang, lastZoomLevel)
            }"""

new_record_playback = """            currentVideoPath?.let { oldPath ->
                playbackManager.recordPlaybackState(oldPath, lastZoomLevel)
            }"""
            
content = content.replace(old_record_playback, new_record_playback)


with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated PlayerViewModel.kt!")
