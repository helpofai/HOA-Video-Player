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
                        // Keep a reference to the best, recycle others if necessary (not doing it here for simplicity of assignment)
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
