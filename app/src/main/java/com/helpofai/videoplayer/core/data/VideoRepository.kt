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
    suspend fun getVideosWithMetadata(): Flow<List<Video>> = withContext(Dispatchers.IO) {
        val allLocalVideos = mediaScanner.getVideos()
        
        videoDao.getAllMetadata().map { metadataList ->
            val metadataMap = metadataList.associateBy { it.path }
            
            allLocalVideos.map { video ->
                val meta = metadataMap[video.path]
                video.copy(
                    isFavorite = meta?.isFavorite ?: false,
                    lastPlayedPosition = meta?.lastPlayedPosition ?: 0L,
                    playCount = meta?.playCount ?: 0,
                    category = MediaSmartCategorizer.categorizeVideo(video.title, video.path, video.duration)
                )
            }.sortedByDescending { it.dateAdded }
        }
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
    
    suspend fun recordPlayback(path: String, position: Long) {
        var meta = videoDao.getMetadataByPath(path)
        if (meta == null) {
            meta = VideoMetadataEntity(path = path, lastPlayedPosition = position, playCount = 1, lastPlayedTimestamp = System.currentTimeMillis())
            videoDao.insertMetadata(meta)
        } else {
            val updatedPlayCount = if (position == 0L) meta.playCount + 1 else meta.playCount
            videoDao.updateMetadata(meta.copy(
                lastPlayedPosition = position,
                playCount = updatedPlayCount,
                lastPlayedTimestamp = System.currentTimeMillis()
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
