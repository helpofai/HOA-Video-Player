package com.helpofai.videoplayer.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.helpofai.videoplayer.core.database.entities.VideoMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM video_metadata")
    fun getAllMetadata(): Flow<List<VideoMetadataEntity>>

    @Query("SELECT * FROM video_metadata WHERE path = :path")
    suspend fun getMetadataByPath(path: String): VideoMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: VideoMetadataEntity)

    @Update
    suspend fun updateMetadata(metadata: VideoMetadataEntity)
    
    @Query("UPDATE video_metadata SET isFavorite = :isFavorite WHERE path = :path")
    suspend fun updateFavoriteStatus(path: String, isFavorite: Boolean)

    @Query("UPDATE video_metadata SET lastPlayedPosition = :position, lastPlayedTimestamp = :timestamp WHERE path = :path")
    suspend fun updatePlaybackPosition(path: String, position: Long, timestamp: Long = System.currentTimeMillis())

    @androidx.room.Delete
    suspend fun delete(metadata: VideoMetadataEntity)

    @Query("UPDATE video_metadata SET lastPlayedPosition = 0, lastPlayedTimestamp = 0")
    suspend fun clearAllWatchHistory()

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE videoPath = :videoPath ORDER BY timeMs ASC")
    fun getBookmarksForVideo(videoPath: String): Flow<List<com.helpofai.videoplayer.core.database.entities.BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: com.helpofai.videoplayer.core.database.entities.BookmarkEntity)

    @androidx.room.Delete
    suspend fun deleteBookmark(bookmark: com.helpofai.videoplayer.core.database.entities.BookmarkEntity)
}
