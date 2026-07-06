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
package com.helpofai.videoplayer.core.data

import com.helpofai.videoplayer.core.database.VideoDao
import com.helpofai.videoplayer.core.database.entities.VideoMetadataEntity
import com.helpofai.videoplayer.core.media.MediaScanner
import com.helpofai.videoplayer.core.media.MediaSmartCategorizer
import com.helpofai.videoplayer.core.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val mediaScanner: MediaScanner,
    private val videoDao: VideoDao
) {
    fun getVideosWithMetadata(): Flow<List<Video>> = videoDao.getAllMetadata().map { metadataList ->
        val allLocalVideos = withContext(Dispatchers.IO) { mediaScanner.getVideos() }
        val metadataMap = metadataList.associateBy { it.path }
        allLocalVideos.map { video ->
            val meta = metadataMap[video.path]
            video.copy(
                isFavorite = meta?.isFavorite ?: false,
                lastPlayedPosition = meta?.lastPlayedPosition ?: 0L,
                playCount = meta?.playCount ?: 0,
                lastPlayedTimestamp = meta?.lastPlayedTimestamp ?: 0L,
                category = MediaSmartCategorizer.categorizeVideo(video.title, video.path, video.duration),
                playbackSpeed = meta?.playbackSpeed ?: 1.0f,
                subtitleTrackLanguage = meta?.subtitleTrackLanguage,
                audioTrackLanguage = meta?.audioTrackLanguage
            )
        }.sortedByDescending { it.dateAdded }
    }

    suspend fun toggleFavorite(path: String, isFavorite: Boolean) {
        var meta = videoDao.getMetadataByPath(path)
        if (meta == null) {
            meta = VideoMetadataEntity(path = path, isFavorite = isFavorite)
            videoDao.insertMetadata(meta)
        } else {
            videoDao.updateFavoriteStatus(path, isFavorite)
        }
    }
    
    suspend fun recordPlayback(
        path: String,
        position: Long,
        speed: Float = 1.0f,
        audioTrack: String? = null,
        subtitleTrack: String? = null,
        zoomLevel: Float = 1.0f
    ) {
        var meta = videoDao.getMetadataByPath(path)
        if (meta == null) {
            meta = VideoMetadataEntity(
                path = path, 
                lastPlayedPosition = position, 
                playCount = 1, 
                lastPlayedTimestamp = System.currentTimeMillis(),
                playbackSpeed = speed,
                audioTrackLanguage = audioTrack,
                subtitleTrackLanguage = subtitleTrack,
                zoomLevel = zoomLevel
            )
            videoDao.insertMetadata(meta)
        } else {
            val updatedPlayCount = if (position == 0L) meta.playCount + 1 else meta.playCount
            videoDao.updateMetadata(meta.copy(
                lastPlayedPosition = position,
                playCount = updatedPlayCount,
                lastPlayedTimestamp = System.currentTimeMillis(),
                playbackSpeed = speed,
                audioTrackLanguage = audioTrack,
                subtitleTrackLanguage = subtitleTrack,
                zoomLevel = zoomLevel
            ))
        }
    }
    suspend fun getMetadata(path: String): VideoMetadataEntity? {
        return videoDao.getMetadataByPath(path)
    }

    suspend fun updateABRepeat(path: String, start: Long?, end: Long?) {
        var meta = videoDao.getMetadataByPath(path)
        if (meta == null) {
            meta = VideoMetadataEntity(path = path, abRepeatStart = start, abRepeatEnd = end)
            videoDao.insertMetadata(meta)
        } else {
            videoDao.updateMetadata(meta.copy(abRepeatStart = start, abRepeatEnd = end))
        }
    }

    suspend fun updateExternalSubtitle(path: String, uri: String?) {
        var meta = videoDao.getMetadataByPath(path)
        if (meta == null) {
            meta = VideoMetadataEntity(path = path, externalSubtitleUri = uri)
            videoDao.insertMetadata(meta)
        } else {
            videoDao.updateMetadata(meta.copy(externalSubtitleUri = uri))
        }
    }

    suspend fun deleteVideo(video: Video): Boolean {
        return withContext(Dispatchers.IO) {
            val file = java.io.File(video.path)
            if (file.exists() && file.delete()) {
                videoDao.delete(VideoMetadataEntity(video.path)) // Actually wait, Entity PK is path!
                true
            } else {
                false
            }
        }
    }

    suspend fun renameVideo(video: Video, newName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = java.io.File(video.path)
            if (file.exists()) {
                val newFile = java.io.File(file.parent, "$newName.${file.extension}")
                if (file.renameTo(newFile)) {
                    // Update metadata in DB
                    val meta = videoDao.getMetadataByPath(video.path)
                    if (meta != null) {
                        videoDao.insertMetadata(meta.copy(path = newFile.absolutePath))
                        videoDao.delete(meta)
                    }
                    true
                } else false
            } else false
        }
    }

    fun getBookmarksForVideo(videoPath: String): Flow<List<com.helpofai.videoplayer.core.database.entities.BookmarkEntity>> {
        return videoDao.getBookmarksForVideo(videoPath)
    }

    suspend fun addBookmark(videoPath: String, timeMs: Long, label: String) {
        val entity = com.helpofai.videoplayer.core.database.entities.BookmarkEntity(
            videoPath = videoPath,
            timeMs = timeMs,
            label = label
        )
        videoDao.insertBookmark(entity)
    }

    suspend fun deleteBookmark(bookmark: com.helpofai.videoplayer.core.database.entities.BookmarkEntity) {
        videoDao.deleteBookmark(bookmark)
    }

    suspend fun clearAllWatchHistory() {
        videoDao.clearAllWatchHistory()
    }
}
