package com.helpofai.videoplayer.core.scanner

import com.arthenica.ffmpegkit.FFmpegKit
import com.helpofai.videoplayer.core.data.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScannerSmartChapterGenerator @Inject constructor(
    private val repository: VideoRepository
) {

    // Scene detection threshold (0.0 to 1.0, where 1.0 is a complete change)
    suspend fun generateChapters(videoPath: String, threshold: Double = 0.4): Boolean = withContext(Dispatchers.IO) {
        try {
            // We use 'scenedetect' filter to find scene changes.
            // showinfo filter prints the timestamp of each selected frame to the log.
            val command = "-i \"$videoPath\" -filter:v \"select='gt(scene,$threshold)',showinfo\" -f null -"
            
            val session = FFmpegKit.execute(command)
            val logs = session.allLogsAsString
            
            // Example log format from showinfo:
            // [Parsed_showinfo_1 @ 0x13c72b250] n:   0 pts: 120120 pts_time:1.334667 pos:  ...
            val ptsTimeRegex = "pts_time:([0-9]+\\.?[0-9]*)".toRegex()
            
            val timestamps = mutableListOf<Long>()
            // Chapter 1 is always the beginning
            timestamps.add(0L)
            
            val matches = ptsTimeRegex.findAll(logs)
            for (match in matches) {
                val timeSec = match.groups[1]?.value?.toDoubleOrNull() ?: continue
                val timeMs = (timeSec * 1000).toLong()
                
                // Add if it's at least 30 seconds after the last chapter to avoid too many chapters during an action scene
                if (timestamps.isEmpty() || (timeMs - timestamps.last()) > 30_000) {
                    timestamps.add(timeMs)
                }
            }
            
            // If we found new chapters, save them to the DB as bookmarks
            if (timestamps.size > 1) {
                timestamps.forEachIndexed { index, timeMs ->
                    val label = "Chapter ${index + 1}"
                    repository.addBookmark(videoPath, timeMs, label)
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
