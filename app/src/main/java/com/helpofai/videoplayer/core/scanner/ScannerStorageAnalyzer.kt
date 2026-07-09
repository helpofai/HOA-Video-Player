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
package com.helpofai.videoplayer.core.scanner

import com.helpofai.videoplayer.core.model.Video
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScannerStorageAnalyzer @Inject constructor() {
    
    data class StorageReport(
        val totalVideos: Int,
        val totalSize: Long,
        val exactDuplicates: List<List<Video>>,
        val suspectedCorrupted: List<Video>,
        val unusedLargeVideos: List<Video> // Large videos not played in a long time
    )
    
    suspend fun analyze(videos: List<Video>): StorageReport = withContext(Dispatchers.IO) {
        val totalVideos = videos.size
        val totalSize = videos.sumOf { it.size }
        
        // 1. Duplicate Detection (Size and Duration matching, plus Hash of first 1MB)
        val exactDuplicates = mutableListOf<List<Video>>()
        val potentialDuplicates = videos.groupBy { "${it.size}_${it.duration}_${it.width}_${it.height}" }
            .filter { it.value.size > 1 }
            .filter { (_, group) ->
                group.map { it.path.lowercase() }.distinct().size > 1
            }
            
        for ((_, group) in potentialDuplicates) {
            val hashGroups = group.groupBy { getFileHashPrefix(it.path) }
            for ((hash, duplicateSet) in hashGroups) {
                if (hash.isNotEmpty() && duplicateSet.size > 1) {
                    exactDuplicates.add(duplicateSet)
                }
            }
        }
        
        // 2. Suspected Corrupted Detection
        val suspectedCorrupted = videos.filter { 
            !File(it.path).exists() || (it.size > 1024 && it.duration == 0L) 
        }
        
        // 3. Unused Large Videos (> 100MB, playCount == 0)
        val unusedLargeVideos = videos.filter { 
            it.size > 100 * 1024 * 1024L && it.playCount == 0 && it.lastPlayedPosition == 0L 
        }.sortedByDescending { it.size }
        
        StorageReport(
            totalVideos = totalVideos,
            totalSize = totalSize,
            exactDuplicates = exactDuplicates,
            suspectedCorrupted = suspectedCorrupted,
            unusedLargeVideos = unusedLargeVideos
        )
    }
    
    private fun getFileHashPrefix(path: String): String {
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return ""
            
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(1024 * 1024) // Read up to 1MB to be fast
                val read = stream.read(buffer)
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}