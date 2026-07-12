package com.helpofai.videoplayer.feature.nearbyshare

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.helpofai.videoplayer.core.data.VideoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class NearbyShareClient(
    private val context: Context,
    private val videoRepository: VideoRepository
) {
    private var clientJob: Job? = null
    private val _transferState = MutableStateFlow<NearbyShareTransferState?>(null)
    val transferState = _transferState.asStateFlow()

    private val isPaused = AtomicBoolean(false)
    private val isCancelled = AtomicBoolean(false)
    private var currentSocket: Socket? = null

    fun startReceive(
        hostIp: String,
        port: Int,
        clientDeviceName: String = "Receiver Device"
    ) {
        stopReceive()
        isPaused.set(false)
        isCancelled.set(false)

        _transferState.value = NearbyShareTransferState(
            id = System.currentTimeMillis().toString(),
            fileName = "Connecting...",
            fileSize = 0,
            bytesTransferred = 0,
            speedBytesPerSecond = 0,
            status = NearbyShareStatus.CONNECTING,
            role = NearbyShareRole.RECEIVER,
            peerDeviceName = hostIp
        )

        clientJob = CoroutineScope(Dispatchers.IO).launch {
            var socket: Socket? = null
            var fileOutputStream: FileOutputStream? = null
            var inputStream: InputStream? = null
            try {
                // Connect
                socket = Socket(hostIp, port).apply {
                    soTimeout = 30000
                    tcpNoDelay = true
                    receiveBufferSize = 1024 * 1024
                }
                currentSocket = socket

                val outputStream = socket.getOutputStream()
                val socketReader = socket.getInputStream().bufferedReader()

                // Establish download folder
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "VidPlay")
                if (!appDir.exists()) appDir.mkdirs()

                // Handshake with offset 0 initially to fetch metadata
                val handshake = "REQUEST_TRANSFER|0|$clientDeviceName\n"
                outputStream.write(handshake.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                // Read metadata
                val metadataLine = socketReader.readLine() ?: throw Exception("Invalid handshake from server")
                if (!metadataLine.startsWith("METADATA")) {
                    throw Exception("Unexpected metadata: $metadataLine")
                }
                
                val parts = metadataLine.split("|")
                val title = parts.getOrNull(1) ?: "shared_video.mp4"
                val size = parts.getOrNull(2)?.toLongOrNull() ?: 0L

                // Check if a partially downloaded file already exists
                val targetFile = File(appDir, title)
                var offset = 0L
                if (targetFile.exists() && targetFile.length() < size) {
                    offset = targetFile.length()
                    
                    // Reconnect and send correct offset for resume!
                    socket.close()
                    socket = Socket(hostIp, port).apply {
                        soTimeout = 30000
                        tcpNoDelay = true
                        receiveBufferSize = 1024 * 1024
                    }
                    currentSocket = socket
                    val newOut = socket.getOutputStream()
                    val newHandshake = "REQUEST_TRANSFER|$offset|$clientDeviceName\n"
                    newOut.write(newHandshake.toByteArray(Charsets.UTF_8))
                    newOut.flush()
                    
                    // Re-consume metadata line
                    val newReader = socket.getInputStream().bufferedReader()
                    newReader.readLine()
                }

                _transferState.value = NearbyShareTransferState(
                    id = System.currentTimeMillis().toString(),
                    fileName = title,
                    fileSize = size,
                    bytesTransferred = offset,
                    speedBytesPerSecond = 0,
                    status = NearbyShareStatus.TRANSFERRING,
                    role = NearbyShareRole.RECEIVER,
                    peerDeviceName = hostIp,
                    filePath = targetFile.absolutePath
                )

                fileOutputStream = FileOutputStream(targetFile, offset > 0)
                inputStream = socket.getInputStream()

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalReceived = offset
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesReceivedInInterval = 0L

                while (isActive && !isCancelled.get()) {
                    if (isPaused.get()) {
                        _transferState.value = _transferState.value?.copy(status = NearbyShareStatus.PAUSED)
                        break
                    }

                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break // EOF

                    fileOutputStream.write(buffer, 0, bytesRead)
                    totalReceived += bytesRead
                    bytesReceivedInInterval += bytesRead

                    val now = System.currentTimeMillis()
                    val interval = now - lastSpeedCalcTime
                    if (interval >= 1000) {
                        val speed = (bytesReceivedInInterval * 1000) / interval
                        _transferState.value = _transferState.value?.copy(
                            bytesTransferred = totalReceived,
                            speedBytesPerSecond = speed,
                            status = NearbyShareStatus.TRANSFERRING
                        )
                        bytesReceivedInInterval = 0
                        lastSpeedCalcTime = now
                    }
                }

                fileOutputStream.flush()

                if (totalReceived >= size && size > 0) {
                    _transferState.value = _transferState.value?.copy(
                        bytesTransferred = size,
                        speedBytesPerSecond = 0,
                        status = NearbyShareStatus.COMPLETED
                    )
                    // Index file so it shows in media libraries
                    MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null) { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            videoRepository.refreshVideos()
                        }
                    }
                } else if (isPaused.get()) {
                    _transferState.value = _transferState.value?.copy(status = NearbyShareStatus.PAUSED, speedBytesPerSecond = 0)
                } else {
                    _transferState.value = _transferState.value?.copy(
                        status = NearbyShareStatus.FAILED,
                        error = "Transfer incomplete or cancelled"
                    )
                }
            } catch (e: Exception) {
                if (!isCancelled.get() && !isPaused.get()) {
                    _transferState.value = _transferState.value?.copy(
                        status = NearbyShareStatus.FAILED,
                        error = e.localizedMessage ?: "Connection error"
                    )
                }
            } finally {
                try { fileOutputStream?.close() } catch (e: Exception) {}
                try { inputStream?.close() } catch (e: Exception) {}
                try { socket?.close() } catch (e: Exception) {}
                currentSocket = null
            }
        }
    }

    fun pauseTransfer() {
        isPaused.set(true)
        try {
            currentSocket?.close()
        } catch (e: Exception) {}
    }

    fun resumeTransfer(hostIp: String, port: Int, clientDeviceName: String) {
        startReceive(hostIp, port, clientDeviceName)
    }

    fun stopReceive() {
        isCancelled.set(true)
        try {
            currentSocket?.close()
        } catch (e: Exception) {}
        currentSocket = null
        clientJob?.cancel()
        clientJob = null
    }
}
