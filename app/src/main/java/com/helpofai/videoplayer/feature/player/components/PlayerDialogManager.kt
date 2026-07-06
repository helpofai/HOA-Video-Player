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
package com.helpofai.videoplayer.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.database.entities.BookmarkEntity
import com.helpofai.videoplayer.feature.qualityanalyzer.QualityReport
import com.helpofai.videoplayer.feature.player.components.FeedbackType
import com.helpofai.videoplayer.feature.player.PlayerViewModel
import com.helpofai.videoplayer.feature.player.TrackSelectorBottomSheet
import com.helpofai.videoplayer.feature.player.VideoInfoDialog
import com.helpofai.videoplayer.feature.player.AudioEqualizerSheet
import com.helpofai.videoplayer.feature.player.PlaybackSpeedSheet

enum class PlayerDialogType {
    TRACK_SELECTOR_AUDIO,
    TRACK_SELECTOR_SUBTITLE,
    DECODER_SELECTOR,
    VIDEO_INFO,
    EQUALIZER,
    SPEED_DIAL,
    VIDEO_ADJUSTMENTS,
    QUALITY_SHEET,
    MORE_POPUP,
    AD_POPUP,
    BOOKMARKS_SHEET
}

@Composable
fun PlayerDialogManager(
    activeDialog: PlayerDialogType?,
    onDismissRequest: () -> Unit,
    viewModel: PlayerViewModel,
    // Decoder
    decoderMode: String,
    onDecoderModeSelect: (String) -> Unit,
    // Video Adjustments
    resizeMode: Int,
    onResizeModeSelected: (Int) -> Unit,
    brightness: Float,
    onBrightnessChanged: (Float) -> Unit,
    isMirrored: Boolean,
    onMirrorToggled: (Boolean) -> Unit,
    isFlipped: Boolean,
    onFlipToggled: (Boolean) -> Unit,
    rotationZ: Float,
    onRotationChanged: (Float) -> Unit,
    // Quality Report
    qualityReport: QualityReport?,
    isAnalyzingQuality: Boolean,
    // More Popup
    playlist: List<Video>,
    currentVideoPath: String,
    onVideoSelect: (String) -> Unit,
    onReorderPlaylist: (Int, Int) -> Unit,
    onShowDialog: (PlayerDialogType) -> Unit,
    // Ad Popup
    isPlaying: Boolean,
    // Bookmarks Sheet
    bookmarks: List<BookmarkEntity>,
    onSeekTo: (Long) -> Unit,
    isGeneratingChapters: Boolean,
    onGenerateAutoChapters: (onStart: () -> Unit, onComplete: (Boolean) -> Unit) -> Unit,
    // Subtitles
    onLoadExternalSubtitle: () -> Unit,
    onFeedbackEvent: (FeedbackEvent) -> Unit
) {
    if (activeDialog == null) return

    when (activeDialog) {
        PlayerDialogType.TRACK_SELECTOR_AUDIO -> {
            TrackSelectorBottomSheet(
                player = viewModel.videoPlayer.player,
                initialTab = 0,
                onDismissRequest = onDismissRequest,
                onLoadExternalSubtitle = onLoadExternalSubtitle
            )
        }
        PlayerDialogType.TRACK_SELECTOR_SUBTITLE -> {
            TrackSelectorBottomSheet(
                player = viewModel.videoPlayer.player,
                initialTab = 1,
                onDismissRequest = onDismissRequest,
                onLoadExternalSubtitle = onLoadExternalSubtitle
            )
        }
        PlayerDialogType.DECODER_SELECTOR -> {
            DecoderSelectorSheet(
                currentDecoder = decoderMode,
                onDecoderSelect = onDecoderModeSelect,
                onDismissRequest = onDismissRequest
            )
        }
        PlayerDialogType.VIDEO_INFO -> {
            VideoInfoDialog(
                player = viewModel.videoPlayer.player,
                videoPath = currentVideoPath,
                onDismissRequest = onDismissRequest
            )
        }
        PlayerDialogType.EQUALIZER -> {
            AudioEqualizerSheet(
                audioEffectManager = viewModel.audioEffectManager,
                onDismissRequest = onDismissRequest
            )
        }
        PlayerDialogType.SPEED_DIAL -> {
            PlaybackSpeedSheet(
                currentSpeed = viewModel.videoPlayer.player.playbackParameters.speed,
                onSpeedSelected = { speed ->
                    viewModel.videoPlayer.player.playbackParameters = viewModel.videoPlayer.player.playbackParameters.withSpeed(speed)
                    onFeedbackEvent(FeedbackEvent(FeedbackType.SPEED, Icons.Default.PlayArrow, "${speed}x"))
                    onDismissRequest()
                },
                onDismissRequest = onDismissRequest
            )
        }
        PlayerDialogType.VIDEO_ADJUSTMENTS -> {
            VideoAdjustmentsSheet(
                currentResizeMode = resizeMode,
                onResizeModeSelected = onResizeModeSelected,
                currentBrightness = brightness,
                onBrightnessChanged = onBrightnessChanged,
                isMirrored = isMirrored,
                onMirrorToggled = onMirrorToggled,
                isFlipped = isFlipped,
                onFlipToggled = onFlipToggled,
                rotationZ = rotationZ,
                onRotationChanged = onRotationChanged,
                onDismissRequest = onDismissRequest
            )
        }
        PlayerDialogType.QUALITY_SHEET -> {
            com.helpofai.videoplayer.feature.qualityanalyzer.components.QualityReportSheet(
                report = qualityReport,
                isAnalyzing = isAnalyzingQuality,
                onDismissRequest = {
                    onDismissRequest()
                    viewModel.clearQualityReport()
                }
            )
        }
        PlayerDialogType.MORE_POPUP -> {
            PlayerMorePopup(
                videos = playlist,
                currentVideoPath = currentVideoPath,
                onVideoSelect = onVideoSelect,
                onReorderPlaylist = onReorderPlaylist,
                onBookmarksClick = { onShowDialog(PlayerDialogType.BOOKMARKS_SHEET) },
                onQualityAnalyzerClick = {
                    onShowDialog(PlayerDialogType.QUALITY_SHEET)
                    viewModel.analyzeVideoQuality()
                },
                onDismissRequest = onDismissRequest
            )
        }
        PlayerDialogType.AD_POPUP -> {
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable {
                            viewModel.videoPlayer.play()
                            onDismissRequest()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Paused",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Show Native Ad
                        com.helpofai.videoplayer.core.ads.NativeAdCard(
                            modifier = Modifier.width(320.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        IconButton(
                            onClick = {
                                viewModel.videoPlayer.play()
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        PlayerDialogType.BOOKMARKS_SHEET -> {
            com.helpofai.videoplayer.feature.scenedetection.components.SceneSelectionSheet(
                videoPath = currentVideoPath,
                bookmarks = bookmarks,
                onSeekTo = onSeekTo,
                onGenerateAutoChapters = {
                    onGenerateAutoChapters(
                        { /* onStart */ },
                        { success ->
                            if (success) {
                                onFeedbackEvent(FeedbackEvent(FeedbackType.INFO, Icons.Default.AutoAwesome, "Scenes Detected"))
                            } else {
                                onFeedbackEvent(FeedbackEvent(FeedbackType.INFO, Icons.Default.AutoAwesome, "No Scenes Found"))
                            }
                        }
                    )
                },
                isGeneratingChapters = isGeneratingChapters,
                onDismissRequest = onDismissRequest
            )
        }
    }
}
