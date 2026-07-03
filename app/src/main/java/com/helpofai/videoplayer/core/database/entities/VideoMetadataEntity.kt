package com.helpofai.videoplayer.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_metadata")
data class VideoMetadataEntity(
    @PrimaryKey val path: String, // Path is unique
    val isFavorite: Boolean = false,
    val lastPlayedPosition: Long = 0L,
    val totalDuration: Long = 0L,
    val playCount: Int = 0,
    val isHidden: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val abRepeatStart: Long? = null,
    val abRepeatEnd: Long? = null,
    val externalSubtitleUri: String? = null,
    val playbackSpeed: Float = 1.0f,
    val subtitleTrackLanguage: String? = null,
    val audioTrackLanguage: String? = null,
    val zoomLevel: Float = 1.0f
)
