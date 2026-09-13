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
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.scanner

import android.content.Context
import com.helpofai.videoplayer.core.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Ultra-fast in-memory registry of available smart thumbnails.
 * Eliminates all blocking java.io.File.exists() checks on the Compose UI thread.
 */
object ThumbnailCacheRegistry {
    private val generatedThumbnailIds = ConcurrentHashMap.newKeySet<Long>()

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dir = File(context.cacheDir, "smart_thumbnails")
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        val name = file.name
                        if (name.startsWith("thumb_") && name.endsWith(".jpg")) {
                            name.substringAfter("thumb_").substringBefore(".jpg").toLongOrNull()?.let { id ->
                                generatedThumbnailIds.add(id)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun hasThumbnail(videoId: Long): Boolean = generatedThumbnailIds.contains(videoId)

    fun registerThumbnail(videoId: Long) {
        generatedThumbnailIds.add(videoId)
    }

    fun getThumbnailModel(context: Context, video: Video): Any {
        return if (hasThumbnail(video.id)) {
            File(context.cacheDir, "smart_thumbnails/thumb_${video.id}.jpg")
        } else {
            video.uri
        }
    }
}
