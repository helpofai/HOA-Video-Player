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

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerIntelligentThumbnailEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Extracts multiple frames from a video, scores them based on sharpness and contrast (variance),
     * and saves the highest-scoring frame as the thumbnail.
     */
    suspend fun generateBestThumbnail(videoPath: String, videoId: Long): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: return@withContext null

            // Extract 5 candidate frames across the video
            val fractions = listOf(0.15, 0.35, 0.5, 0.7, 0.85)
            var bestBitmap: Bitmap? = null
            var bestScore = -1.0

            for (fraction in fractions) {
                val timeUs = (durationMs * fraction * 1000).toLong()
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val score = calculateFrameScore(bitmap)
                    if (score > bestScore) {
                        bestScore = score
                        // Recycle previous best bitmap before replacing
                        if (bestBitmap != null && bestBitmap !== bitmap) {
                            bestBitmap?.recycle()
                        }
                        bestBitmap = bitmap
                    }
                }
            }
            bestBitmap?.let { bmp ->
                val thumbnailsDir = File(context.cacheDir, "smart_thumbnails")
                if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()

                val thumbFile = File(thumbnailsDir, "thumb_$videoId.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                return@withContext thumbFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        null
    }

    /**
     * A simple heuristic: calculate variance of pixel intensities to estimate sharpness & contrast.
     * Sharp images with good composition tend to have higher variance than blurry, dark, or plain frames.
     */
    private fun calculateFrameScore(bitmap: Bitmap): Double {
        // Scale down for faster processing
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        
        val pixels = IntArray(64 * 64)
        scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)

        var sum = 0.0
        var sqSum = 0.0

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            
            sum += luminance
            sqSum += (luminance * luminance)
        }

        val n = (64 * 64).toDouble()
        val mean = sum / n
        val variance = (sqSum / n) - (mean * mean)
        
        scaled.recycle()
        return variance
    }
}