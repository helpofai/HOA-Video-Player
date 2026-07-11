package com.helpofai.videoplayer.feature.watch_party.streaming

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket

/**
 * A lightweight HTTP/1.1 video streaming server that runs on the host device.
 *
 * - Listens on [VIDEO_STREAM_PORT] (8080)
 * - Serves GET /video with full Range request support (HTTP 206 Partial Content)
 *   so ExoPlayer on the client can seek freely without downloading the whole file.
 * - Handles multiple simultaneous client connections.
 */
class WatchPartyVideoStreamServer {

    companion object {
        const val VIDEO_STREAM_PORT = 8080
        private val instance = WatchPartyVideoStreamServer()
        fun getInstance(): WatchPartyVideoStreamServer = instance
    }

    private var serverJob: Job? = null
    private var videoFile: File? = null
    private var videoUri: Uri? = null
    private var appContext: Context? = null

    /** Start serving [file]. Stops any previously running server first. */
    fun start(file: File, port: Int = VIDEO_STREAM_PORT) {
        stop()
        videoFile = file
        android.util.Log.d("VideoStreamServer", "Starting server for file on port $port")
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                while (isActive) {
                    val client = serverSocket.accept().apply {
                        try {
                            tcpNoDelay = true
                            sendBufferSize = 1024 * 1024
                            receiveBufferSize = 1024 * 1024
                        } catch (e: Exception) {}
                    }
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoStreamServer", "Server socket exception: ${e.message}")
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    /** Start serving video via content or file [uri] using the [context]. */
    fun start(context: Context, uri: Uri, port: Int = VIDEO_STREAM_PORT) {
        stop()
        appContext = context.applicationContext
        videoUri = uri
        android.util.Log.d("VideoStreamServer", "Starting server for URI on port $port")
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                while (isActive) {
                    val client = serverSocket.accept().apply {
                        try {
                            tcpNoDelay = true
                            sendBufferSize = 1024 * 1024
                            receiveBufferSize = 1024 * 1024
                        } catch (e: Exception) {}
                    }
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoStreamServer", "Server socket exception: ${e.message}")
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        videoFile = null
        videoUri = null
        appContext = null
    }

    val isRunning: Boolean get() = serverJob?.isActive == true

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30_000
            val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            val output = java.io.BufferedOutputStream(socket.getOutputStream(), 128 * 1024)

            // Read HTTP request headers
            val requestLine = input.readLine() ?: return
            android.util.Log.d("VideoStreamServer", "Request: $requestLine")
            val headers = mutableMapOf<String, String>()
            var line: String
            while (input.readLine().also { line = it ?: "" } != null && line.isNotEmpty()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    headers[line.substring(0, colonIdx).trim().lowercase()] =
                        line.substring(colonIdx + 1).trim()
                }
            }

            val file = videoFile
            val uri = videoUri
            val context = appContext

            if (file == null && (uri == null || context == null)) {
                android.util.Log.w("VideoStreamServer", "No video loaded, returning 503")
                sendError(output, 503, "No video loaded")
                return
            }

            var fileSize: Long = 0L
            val mimeType: String

            if (uri != null && context != null) {
                try {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                        val len = afd.length
                        fileSize = if (len >= 0) len else {
                            afd.parcelFileDescriptor?.statSize ?: 0L
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("VideoStreamServer", "openAssetFileDescriptor failed, trying fallback: ${e.message}")
                    try {
                        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                if (sizeIndex != -1) {
                                    fileSize = cursor.getLong(sizeIndex)
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        android.util.Log.e("VideoStreamServer", "Fallback size query failed: ${ex.message}")
                    }
                }
                
                if (fileSize <= 0 && uri.scheme == "file") {
                    uri.path?.let { p ->
                        val f = File(p)
                        if (f.exists()) {
                            fileSize = f.length()
                        }
                    }
                }

                if (fileSize <= 0) {
                    android.util.Log.e("VideoStreamServer", "Video file size resolved to 0 or negative for URI: $uri")
                    sendError(output, 404, "Video file not found or empty")
                    return
                }
                mimeType = context.contentResolver.getType(uri) ?: getMimeType(uri.toString())
            } else {
                val f = file!!
                if (!f.exists()) {
                    android.util.Log.e("VideoStreamServer", "Video file does not exist: ${f.absolutePath}")
                    sendError(output, 404, "Video file not found")
                    return
                }
                fileSize = f.length()
                mimeType = getMimeType(f.name)
            }

            android.util.Log.d("VideoStreamServer", "Serving video: mime=$mimeType, size=$fileSize")
            val rangeHeader = headers["range"]
            val isHead = requestLine.startsWith("HEAD", ignoreCase = true)

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Partial content (Range request) — needed for seeking
                val rangePart = rangeHeader.removePrefix("bytes=")
                val dashIdx = rangePart.indexOf('-')
                val startStr = rangePart.substring(0, dashIdx).trim()
                val endStr = rangePart.substring(dashIdx + 1).trim()

                val start = if (startStr.isEmpty()) 0L else startStr.toLongOrNull() ?: 0L
                val end = if (endStr.isEmpty()) fileSize - 1 else (endStr.toLongOrNull() ?: fileSize - 1)
                val clampedEnd = minOf(end, fileSize - 1)
                val contentLength = clampedEnd - start + 1

                android.util.Log.d("VideoStreamServer", "Range request: bytes $start-$clampedEnd/$fileSize. Content-Length: $contentLength")
                val responseHeaders = StringBuilder()
                responseHeaders.append("HTTP/1.1 206 Partial Content\r\n")
                responseHeaders.append("Content-Type: $mimeType\r\n")
                responseHeaders.append("Content-Length: $contentLength\r\n")
                responseHeaders.append("Content-Range: bytes $start-$clampedEnd/$fileSize\r\n")
                responseHeaders.append("Accept-Ranges: bytes\r\n")
                responseHeaders.append("Connection: close\r\n")
                responseHeaders.append("\r\n")
                output.write(responseHeaders.toString().toByteArray(Charsets.UTF_8))

                if (!isHead) {
                    if (uri != null && context != null) {
                        streamUriPortion(context, uri, output, start, contentLength)
                    } else {
                        streamFilePortion(file!!, output, start, contentLength)
                    }
                }
            } else {
                // Full content response
                android.util.Log.d("VideoStreamServer", "Full content request: Content-Length: $fileSize")
                val responseHeaders = StringBuilder()
                responseHeaders.append("HTTP/1.1 200 OK\r\n")
                responseHeaders.append("Content-Type: $mimeType\r\n")
                responseHeaders.append("Content-Length: $fileSize\r\n")
                responseHeaders.append("Accept-Ranges: bytes\r\n")
                responseHeaders.append("Connection: close\r\n")
                responseHeaders.append("\r\n")
                output.write(responseHeaders.toString().toByteArray(Charsets.UTF_8))

                if (!isHead) {
                    if (uri != null && context != null) {
                        streamUriPortion(context, uri, output, 0L, fileSize)
                    } else {
                        streamFilePortion(file!!, output, 0L, fileSize)
                    }
                }
            }

            output.flush()
        } catch (e: Exception) {
            android.util.Log.e("VideoStreamServer", "Error handling client socket: ${e.message}")
        } finally {
            try { socket.close() } catch (e: Exception) {}
        }
    }

    private fun streamFilePortion(file: File, output: OutputStream, start: Long, length: Long) {
        try {
            val destChannel = java.nio.channels.Channels.newChannel(output)
            RandomAccessFile(file, "r").use { raf ->
                val srcChannel = raf.channel
                var transferred = 0L
                while (transferred < length) {
                    val count = srcChannel.transferTo(start + transferred, length - transferred, destChannel)
                    if (count <= 0) break
                    transferred += count
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoStreamServer", "NIO file transfer failed, falling back to buffer copy: ${e.message}")
            // Fallback to traditional buffer copy
            val buffer = ByteArray(128 * 1024) // 128KB chunks
            var remaining = length
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }
    }

    private fun streamUriPortion(context: Context, uri: Uri, output: OutputStream, start: Long, length: Long) {
        var pfd: android.os.ParcelFileDescriptor? = null
        var dupPfd: android.os.ParcelFileDescriptor? = null
        var fis: java.io.FileInputStream? = null
        var success = false

        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                dupPfd = pfd.dup()
                fis = java.io.FileInputStream(dupPfd.fileDescriptor)
                val srcChannel = fis.channel
                val destChannel = java.nio.channels.Channels.newChannel(output)
                var transferred = 0L
                while (transferred < length) {
                    val count = srcChannel.transferTo(start + transferred, length - transferred, destChannel)
                    if (count <= 0) break
                    transferred += count
                }
                success = true
            }
        } catch (e: Exception) {
            android.util.Log.w("VideoStreamServer", "Failed to stream URI via NIO channel, falling back to buffer copy: ${e.message}")
        } finally {
            try { fis?.close() } catch(e: Exception){}
            try { dupPfd?.close() } catch(e: Exception){}
            try { pfd?.close() } catch(e: Exception){}
        }

        if (!success) {
            // Fallback to sequential read and skip
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    var skipped = 0L
                    while (skipped < start) {
                        val skipAmt = inputStream.skip(start - skipped)
                        if (skipAmt <= 0) break
                        skipped += skipAmt
                    }
                    
                    val buffer = ByteArray(128 * 1024) // 128KB chunks
                    var remaining = length
                    while (remaining > 0) {
                        val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                        val read = inputStream.read(buffer, 0, toRead)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoStreamServer", "Fallback stream failed: ${e.message}", e)
            }
        }
    }

    private fun sendError(output: OutputStream, code: Int, message: String) {
        val body = "<html><body><h1>$code $message</h1></body></html>"
        val response = "HTTP/1.1 $code $message\r\nContent-Type: text/html\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".mp4", true)  -> "video/mp4"
            fileName.endsWith(".mkv", true)  -> "video/x-matroska"
            fileName.endsWith(".webm", true) -> "video/webm"
            fileName.endsWith(".avi", true)  -> "video/x-msvideo"
            fileName.endsWith(".mov", true)  -> "video/quicktime"
            fileName.endsWith(".ts", true)   -> "video/mp2t"
            fileName.endsWith(".flv", true)  -> "video/x-flv"
            fileName.endsWith(".3gp", true)  -> "video/3gpp"
            else -> "video/mp4"
        }
    }
}
