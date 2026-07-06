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
package com.helpofai.videoplayer.core.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.MediaMetadata as Media3Metadata
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.database.entities.VideoMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlaybackManager @Inject constructor(
    val videoPlayer: VideoPlayer,
    private val repository: VideoRepository
) {
    suspend fun prepareVideo(
        path: String,
        uri: Uri,
        meta: VideoMetadataEntity?,
        resumePosition: Long,
        preferredSpeed: Float,
        preferredSubtitleLang: String?,
        zoomLevel: Float
    ) = withContext(Dispatchers.Main) {
        val subtitleConfigs = findSubtitlesForVideo(path).toMutableList()

        if (meta?.externalSubtitleUri != null) {
            val extUri = Uri.parse(meta.externalSubtitleUri)
            val extConfig = MediaItem.SubtitleConfiguration.Builder(extUri)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("ext")
                .setLabel("External Subtitle")
                .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                .build()
            subtitleConfigs.add(extConfig)
        }

        val fileName = File(path).name
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setSubtitleConfigurations(subtitleConfigs)
            .setMediaMetadata(Media3Metadata.Builder().setTitle(fileName).build())
            .build()

        videoPlayer.prepare(mediaItem)

        if (resumePosition > 0L) {
            videoPlayer.seekTo(resumePosition)
        }

        videoPlayer.setPlaybackSpeed(preferredSpeed)

        if (preferredSubtitleLang != "Off" && preferredSubtitleLang != null) {
            videoPlayer.player.trackSelectionParameters = videoPlayer.player.trackSelectionParameters
                .buildUpon()
                .setPreferredTextLanguage(preferredSubtitleLang)
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                .build()
        } else if (preferredSubtitleLang == "Off" || preferredSubtitleLang == null) {
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
    }

    suspend fun recordPlaybackState(path: String, lastZoomLevel: Float) {
        val position = videoPlayer.player.currentPosition
        val speed = videoPlayer.player.playbackParameters.speed
        val audioLang = videoPlayer.player.trackSelectionParameters.preferredAudioLanguages.firstOrNull()
        val subLang = videoPlayer.player.trackSelectionParameters.preferredTextLanguages.firstOrNull()
        
        withContext(Dispatchers.IO) {
            repository.recordPlayback(path, position, speed, audioLang, subLang, lastZoomLevel)
        }
    }

    private suspend fun findSubtitlesForVideo(videoPath: String): List<MediaItem.SubtitleConfiguration> = withContext(Dispatchers.IO) {
        val subtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()
        try {
            val videoFile = File(videoPath)
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
                        "srt" -> MimeTypes.APPLICATION_SUBRIP
                        "vtt" -> MimeTypes.TEXT_VTT
                        "ass", "ssa" -> MimeTypes.TEXT_SSA
                        else -> MimeTypes.APPLICATION_SUBRIP
                    }

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
}
