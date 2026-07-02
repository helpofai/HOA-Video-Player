package com.helpofai.videoplayer.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoPath: String,
    val timeMs: Long,
    val label: String
)
