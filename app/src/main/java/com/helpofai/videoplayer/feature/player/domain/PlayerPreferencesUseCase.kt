package com.helpofai.videoplayer.feature.player.domain

import com.helpofai.videoplayer.core.data.SettingsRepository
import com.helpofai.videoplayer.feature.learning.OfflineLearningEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerPreferencesUseCase @Inject constructor(
    private val learningEngine: OfflineLearningEngine,
    private val settingsRepository: SettingsRepository
) {
    val backgroundPlaybackEnabled: Flow<Boolean> = settingsRepository.backgroundPlayback
    val longPressBoostSpeed: Flow<Float> = settingsRepository.longPressBoostSpeed

    fun getPreferredPlaybackSpeed(): Float = learningEngine.getPreferredPlaybackSpeed()
    
    fun savePlaybackSpeed(speed: Float) {
        learningEngine.learnPlaybackSpeed(speed)
    }

    fun getPreferredSubtitleLanguage(): String? = learningEngine.getPreferredSubtitleLanguage()

    fun getPreferredAspectRatio(): String = learningEngine.getPreferredAspectRatio()

    fun getPreferredBrightness(): Float = learningEngine.getPreferredBrightness()
    
    fun saveBrightness(brightness: Float) {
        learningEngine.learnBrightness(brightness)
    }

    fun getPreferredVolume(): Float = learningEngine.getPreferredVolume()
    
    fun saveVolume(volume: Float) {
        learningEngine.learnVolume(volume)
    }
}
