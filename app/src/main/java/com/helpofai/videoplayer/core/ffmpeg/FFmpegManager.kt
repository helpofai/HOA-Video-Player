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
package com.helpofai.videoplayer.core.ffmpeg

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FFmpegManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 1. Trim Video
    suspend fun trimVideo(inputPath: String, outputPath: String, startMs: Long, endMs: Long): Boolean = withContext(Dispatchers.IO) {
        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0
        val command = "-ss $startSec -i \"$inputPath\" -t $durationSec -c copy \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 2. Extract Audio (MP3)
    suspend fun extractAudio(inputPath: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        val command = "-i \"$inputPath\" -vn -acodec libmp3lame -q:a 2 \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 3. Compress Video (H.264 / CRF 28)
    suspend fun compressVideo(inputPath: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        val command = "-i \"$inputPath\" -vcodec libx264 -crf 28 -preset fast \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 4. Convert Format
    suspend fun convertFormat(inputPath: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        val command = "-i \"$inputPath\" -c copy \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 5. Rotate Video
    suspend fun rotateVideo(inputPath: String, outputPath: String, transpose: Int): Boolean = withContext(Dispatchers.IO) {
        // transpose: 0 = 90CounterClockwise and Vertical Flip (default)
        // 1 = 90Clockwise
        // 2 = 90CounterClockwise
        // 3 = 90Clockwise and Vertical Flip
        val command = "-i \"$inputPath\" -vf \"transpose=$transpose\" -c:a copy \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 6. Reverse Video
    suspend fun reverseVideo(inputPath: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        val command = "-i \"$inputPath\" -vf reverse -af areverse \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 7. Change Resolution
    suspend fun changeResolution(inputPath: String, outputPath: String, width: Int, height: Int): Boolean = withContext(Dispatchers.IO) {
        val command = "-i \"$inputPath\" -vf scale=$width:$height -c:a copy \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 8. Extract Frames
    suspend fun extractFrames(inputPath: String, outputDir: String, fps: Int = 1): Boolean = withContext(Dispatchers.IO) {
        val command = "-i \"$inputPath\" -vf \"fps=$fps\" \"$outputDir/frame_%04d.jpg\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 9. Make GIF
    suspend fun makeGif(inputPath: String, outputPath: String, startMs: Long, durationMs: Long): Boolean = withContext(Dispatchers.IO) {
        val startSec = startMs / 1000.0
        val durationSec = durationMs / 1000.0
        val command = "-ss $startSec -t $durationSec -i \"$inputPath\" -vf \"fps=10,scale=320:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" -loop 0 \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }

    // 10. Merge Videos
    suspend fun mergeVideos(inputPaths: List<String>, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        if (inputPaths.isEmpty()) return@withContext false
        
        val tempListFile = File(context.cacheDir, "ffmpeg_concat_list.txt")
        tempListFile.printWriter().use { out ->
            inputPaths.forEach { path ->
                out.println("file '${path.replace("'", "'\\''")}'")
            }
        }
        
        val command = "-f concat -safe 0 -i \"${tempListFile.absolutePath}\" -c copy \"$outputPath\""
        val session = FFmpegKit.execute(command)
        
        tempListFile.delete()
        ReturnCode.isSuccess(session.returnCode)
    }

    // 11. Extract Screenshot
    suspend fun takeScreenshot(inputPath: String, outputPath: String, timeMs: Long): Boolean = withContext(Dispatchers.IO) {
        val timeSec = timeMs / 1000.0
        val command = "-ss $timeSec -i \"$inputPath\" -vframes 1 -q:v 2 \"$outputPath\""
        val session = FFmpegKit.execute(command)
        ReturnCode.isSuccess(session.returnCode)
    }
}
