package com.arthenica.ffmpegkit

/**
 * Mock class to replace the deprecated and removed FFmpegKit Maven Central dependency.
 * To actually use FFmpeg, download the AAR from: https://github.com/arthenica/ffmpeg-kit/releases
 * place it in the app/libs/ folder, and remove this package.
 */
class FFmpegKit {
    companion object {
        fun execute(command: String): Session {
            // Mock execution
            return Session()
        }
    }
}

class Session {
    val returnCode = ReturnCode()
    val allLogsAsString = "Mock logs"
}

class ReturnCode {
    companion object {
        fun isSuccess(returnCode: ReturnCode): Boolean {
            return true
        }
    }
}
