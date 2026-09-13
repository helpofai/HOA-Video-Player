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
package com.helpofai.videoplayer.core.media

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.File

/**
 * Ultra-high-performance Coil fetcher that intercepts video Uris and queries the
 * native Android MediaStore thumbnail cache directly via [android.content.ContentResolver.loadThumbnail].
 *
 * This bypasses slow CPU-based MediaMetadataRetriever video frame extraction,
 * delivering pre-rendered thumbnails in 1-2ms for buttery smooth 60/120fps scrolling.
 */
class MediaStoreVideoThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val scheme = data.scheme ?: return null
        if (scheme != "content" && scheme != "file") return null

        val uriStr = data.toString()
        val isVideoUri = uriStr.contains("video", ignoreCase = true) ||
            data.path?.let { p ->
                p.endsWith(".mp4", true) || p.endsWith(".mkv", true) ||
                p.endsWith(".webm", true) || p.endsWith(".avi", true) ||
                p.endsWith(".mov", true) || p.endsWith(".3gp", true) ||
                p.endsWith(".ts", true) || p.endsWith(".flv", true)
            } == true

        if (!isVideoUri) return null

        // 1. Check if smart thumbnail already exists in disk cache
        val id = try { ContentUris.parseId(data) } catch (_: Exception) { null }
        if (id != null) {
            val smartThumb = File(context.cacheDir, "smart_thumbnails/thumb_$id.jpg")
            if (smartThumb.exists() && smartThumb.length() > 0) {
                val bitmap = BitmapFactory.decodeFile(smartThumb.absolutePath)
                if (bitmap != null) {
                    return DrawableResult(
                        drawable = BitmapDrawable(context.resources, bitmap),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                }
            }
        }

        // 2. Android 10+ (API 29+): Instant OS hardware-accelerated MediaStore thumbnail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && scheme == "content") {
            try {
                val bitmap = context.contentResolver.loadThumbnail(data, Size(360, 202), null)
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } catch (_: Throwable) {}
        } else if (id != null && scheme == "content") {
            // Android 9 and below
            try {
                @Suppress("DEPRECATION")
                val bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver,
                    id,
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    null
                )
                if (bitmap != null) {
                    return DrawableResult(
                        drawable = BitmapDrawable(context.resources, bitmap),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                }
            } catch (_: Throwable) {}
        }

        // Return null to let Coil's VideoFrameDecoder act as fallback if not in MediaStore
        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val scheme = data.scheme
            if (scheme == "content" || scheme == "file") {
                val uriStr = data.toString()
                val isVideo = uriStr.contains("video", ignoreCase = true) ||
                    data.path?.let { p ->
                        p.endsWith(".mp4", true) || p.endsWith(".mkv", true) ||
                        p.endsWith(".webm", true) || p.endsWith(".avi", true) ||
                        p.endsWith(".mov", true) || p.endsWith(".3gp", true) ||
                        p.endsWith(".ts", true) || p.endsWith(".flv", true)
                    } == true
                if (isVideo) {
                    return MediaStoreVideoThumbnailFetcher(context, data, options)
                }
            }
            return null
        }
    }
}
