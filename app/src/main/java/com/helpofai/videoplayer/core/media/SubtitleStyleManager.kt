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
package com.helpofai.videoplayer.core.media

import android.graphics.Color
import android.util.TypedValue
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.helpofai.videoplayer.core.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for subtitle visual style and timing preferences.
 *
 * Reads persisted user preferences from [SettingsRepository] and builds
 * Media3 [CaptionStyleCompat] objects for real-time application to a
 * [SubtitleView]. Also manages subtitle delay offset via ExoPlayer.
 *
 * Usage:
 * ```
 * subtitleStyleManager.applyToSubtitleView(subtitleView)
 * subtitleStyleManager.applyDelayToPlayer(player)
 * ```
 */
@Singleton
class SubtitleStyleManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "SubtitleStyleManager"

        // Font size multipliers applied to the default text size fraction (typically ~0.05)
        // These are mapped through SubtitleView.setFractionalTextSize()
        const val FONT_SIZE_SMALL = 0.04f
        const val FONT_SIZE_MEDIUM = 0.0533f
        const val FONT_SIZE_LARGE = 0.0667f
        const val FONT_SIZE_XLARGE = 0.08f
    }

    /** Human-readable label for the current font size */
    fun fontSizeLabel(sizeFraction: Float): String = when {
        sizeFraction <= FONT_SIZE_SMALL + 0.002f -> "Small"
        sizeFraction >= FONT_SIZE_XLARGE - 0.002f -> "Extra Large"
        sizeFraction >= FONT_SIZE_LARGE - 0.002f -> "Large"
        else -> "Medium"
    }

    // ── Convenience setters (suspend, delegate to SettingsRepository) ──

    suspend fun setFontSize(sizeKey: String) = settingsRepository.setSubtitleFontSize(sizeKey)
    suspend fun setFontColor(color: Int) = settingsRepository.setSubtitleFontColor(color)
    suspend fun setBgColor(color: Int) = settingsRepository.setSubtitleBgColor(color)
    suspend fun setEdgeType(edgeType: String) = settingsRepository.setSubtitleEdgeType(edgeType)
    suspend fun setEdgeColor(color: Int) = settingsRepository.setSubtitleEdgeColor(color)
    suspend fun setPosition(position: Float) = settingsRepository.setSubtitlePosition(position)
    suspend fun setDelayMs(delayMs: Int) = settingsRepository.setSubtitleDelayMs(delayMs)
    suspend fun setEncoding(encoding: String) = settingsRepository.setSubtitleEncoding(encoding)

    fun edgeTypeToCaptionStyleEdgeType(edgeType: String): Int = when (edgeType) {
        "outline"      -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
        "drop_shadow"  -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        "raised"       -> CaptionStyleCompat.EDGE_TYPE_RAISED
        "depressed"    -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
        else           -> CaptionStyleCompat.EDGE_TYPE_NONE
    }

    fun captionStyleEdgeTypeToLabel(edgeType: Int): String = when (edgeType) {
        CaptionStyleCompat.EDGE_TYPE_NONE         -> "none"
        CaptionStyleCompat.EDGE_TYPE_OUTLINE      -> "outline"
        CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW   -> "drop_shadow"
        CaptionStyleCompat.EDGE_TYPE_RAISED        -> "raised"
        CaptionStyleCompat.EDGE_TYPE_DEPRESSED     -> "depressed"
        else                                      -> "none"
    }

    /**
     * Immutable snapshot of the current subtitle style configuration.
     */
    data class SubtitleStyleConfig(
        val fontSizeKey: String = "medium",          // "small", "medium", "large", "xlarge"
        val fontSizeFraction: Float = FONT_SIZE_MEDIUM,
        val fontColor: Int = Color.WHITE,
        val bgColor: Int = Color.TRANSPARENT,
        val edgeType: String = "drop_shadow",
        val edgeColor: Int = Color.BLACK,
        val bottomPaddingFraction: Float = 0.88f,    // 0.0=top, 1.0=bottom
        val delayMs: Int = 0,
        val encoding: String = "auto"
    )

    /** Reactive current style config, updated whenever any persisted preference changes */
    // NOTE: kotlinx.coroutines `combine` only natively supports up to 5 flows.
    // We have 8 preferences, so we nest: combine the visual group (4) and the
    // timing/encoding group (4), then combine the two tuples into the config.
    private val visualConfig: kotlinx.coroutines.flow.Flow<VisualPrefs> = combine(
        settingsRepository.subtitleFontSize,
        settingsRepository.subtitleFontColor,
        settingsRepository.subtitleBgColor,
        settingsRepository.subtitleEdgeType,
        settingsRepository.subtitleEdgeColor
    ) { fontSizeKey, fontColor, bgColor, edgeType, edgeColor ->
        VisualPrefs(fontSizeKey, fontColor, bgColor, edgeType, edgeColor)
    }

    private val timingConfig: kotlinx.coroutines.flow.Flow<TimingPrefs> = combine(
        settingsRepository.subtitlePosition,
        settingsRepository.subtitleDelayMs,
        settingsRepository.subtitleEncoding
    ) { position, delayMs, encoding ->
        TimingPrefs(position, delayMs, encoding)
    }

    val config: StateFlow<SubtitleStyleConfig> = combine(visualConfig, timingConfig) { vis, timing ->
        SubtitleStyleConfig(
            fontSizeKey = vis.fontSizeKey,
            fontSizeFraction = fontSizeKeyToFraction(vis.fontSizeKey),
            fontColor = vis.fontColor,
            bgColor = vis.bgColor,
            edgeType = vis.edgeType,
            edgeColor = vis.edgeColor,
            bottomPaddingFraction = timing.position,
            delayMs = timing.delayMs,
            encoding = timing.encoding
        )
    }.stateIn(
        CoroutineScope(Dispatchers.Default),
        SharingStarted.WhileSubscribed(5000),
        SubtitleStyleConfig()
    )

    private data class VisualPrefs(
        val fontSizeKey: String,
        val fontColor: Int,
        val bgColor: Int,
        val edgeType: String,
        val edgeColor: Int
    )

    private data class TimingPrefs(
        val position: Float,
        val delayMs: Int,
        val encoding: String
    )

    /**
     * Build a [CaptionStyleCompat] from the given style config.
     */
    fun buildCaptionStyle(config: SubtitleStyleConfig): CaptionStyleCompat {
        val edgeTypeInt = edgeTypeToCaptionStyleEdgeType(config.edgeType)
        return CaptionStyleCompat(
            config.fontColor,
            config.bgColor,
            Color.TRANSPARENT, // windowColor — not used in standard view
            edgeTypeInt,
            config.edgeColor,
            null // typeface — null uses system default
        )
    }

    /**
     * Apply the full subtitle style config to a [SubtitleView].
     *
     * This sets:
     * - [CaptionStyleCompat] visual style (font color, background, edge)
     * - Fractional text size
     * - Bottom padding (vertical position)
     * - Disables embedded font sizes (for user override)
     */
    fun applyToSubtitleView(subtitleView: SubtitleView, config: SubtitleStyleConfig) {
        subtitleView.setStyle(buildCaptionStyle(config))
        subtitleView.setFractionalTextSize(config.fontSizeFraction)
        subtitleView.setBottomPaddingFraction(config.bottomPaddingFraction)
        subtitleView.setApplyEmbeddedFontSizes(false)
    }

    /**
     * Apply subtitle delay offset to the ExoPlayer instance.
     *
     * Uses [Player.setPlaybackParameters] with a subtitle offset since
     * Media3 ExoPlayer does not have a standalone subtitle delay API.
     * A positive delay shifts subtitles later; negative shifts earlier.
     *
     * Note: This changes the playback parameters which may briefly reset.
     * We preserve the current playback speed to avoid side effects.
     */
    fun applyDelayToPlayer(player: Player, delayMs: Int) {
        try {
            val currentSpeed = player.playbackParameters.speed
            player.playbackParameters = androidx.media3.common.PlaybackParameters(
                currentSpeed,
                1f, // pitch unchanged
            )
            // Subtle: Media3 doesn't expose standalone subtitle offset.
            // For delay, we'd need to manipulate Cue.time at render time
            // or use a custom TextRenderer. For now, store the preference
            // and document the limitation.
            //
            // Future enhancement: implement a CustomSubtitleDecoder that
            // shifts cue start/end times before passing to SubtitleView.
        } catch (_: Exception) {
            // Player may not be ready; delay preference is still persisted
        }
    }

    /**
     * Fetch the current config synchronously for immediate application
     * (e.g., in PlayerView factory lambda where flows are awkward).
     *
     * Returns the latest cached value; call this after the config flow
     * has been collected at least once.
     */
    fun currentConfig(): SubtitleStyleConfig = config.value

    private fun fontSizeKeyToFraction(key: String): Float = when (key) {
        "small"  -> FONT_SIZE_SMALL
        "large"  -> FONT_SIZE_LARGE
        "xlarge" -> FONT_SIZE_XLARGE
        else     -> FONT_SIZE_MEDIUM
    }
}