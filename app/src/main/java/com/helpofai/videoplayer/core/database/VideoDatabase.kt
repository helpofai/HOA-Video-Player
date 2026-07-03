package com.helpofai.videoplayer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.helpofai.videoplayer.core.database.entities.BookmarkEntity
import com.helpofai.videoplayer.core.database.entities.VideoMetadataEntity

@Database(entities = [VideoMetadataEntity::class, BookmarkEntity::class], version = 4, exportSchema = false)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}
