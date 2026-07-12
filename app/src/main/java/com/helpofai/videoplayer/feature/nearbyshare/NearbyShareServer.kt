package com.helpofai.videoplayer.feature.nearbyshare

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class NearbyShareServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val _transferState = MutableStateFlow<NearbyShareTransferState?>(null)
    val transferState = _transferState.asStateFlow()

    private val isPaused = AtomicBoolean(false)
    private val isCancelled = AtomicBoolean(false)

    fun startServer(video: Uri, title: String, size: Long, port: Int = 5050, deviceName: String = "Sender Device") {
        stopServer()
        isPaused.set(false)
        isCancelled.set(false)

        _transferState.value = NearbyShareTransferState(
            id = System.currentTimeMillis().toString(),
            fileName = title,
            fileSize = size,
            bytesTransferred = 0,
            speedBytesPerSecond = 0,
            status = NearbyShareStatus.ADVERTISING,
            role = NearbyShareRole.SENDER,
            peerDeviceName = ""
        )

        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                android.util.Log.d("NearbyShareServer", "Server started on port $port")

                while (isActive && !isCancelled.get()) {
                    val clientSocket = serverSocket?.accept() ?: break
                    clientSocket.use { socket ->
                        _transferState.value = _transferState.value?.copy(
                            status = NearbyShareStatus.CONNECTING,
                            peerDeviceName = socket.inetAddress.hostAddress ?: "Unknown Receiver"
                        )
                        handleClient(socket, video, title, size)
                    }
                }
            } catch (e: Exception) {
                if (!isCancelled.get()) {
                    _transferState.value = _transferState.value?.copy(
                        status = NearbyShareStatus.FAILED,
                        error = e.localizedMessage ?: "Server failed to start"
                    )
                }
            } finally {
                stopServer()
            }
        }
    }

    private suspend fun handleClient(socket: Socket, videoUri: Uri, title: String, size: Long) {
        withContext(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                socket.soTimeout = 30000
                val reader = socket.getInputStream().bufferedReader()
                outputStream = socket.getOutputStream()

                // 1. Handshake
                val handshakeLine = reader.readLine() ?: return@withContext
                if (!handshakeLine.startsWith("REQUEST_TRANSFER")) {
                    return@withContext
                }

                // Format: REQUEST_TRANSFER|<offset>|<clientDeviceName>
                val parts = handshakeLine.split("|")
                val offset = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                val clientName = parts.getOrNull(2) ?: "Remote Client"

                _transferState.value = _transferState.value?.copy(
                    status = NearbyShareStatus.TRANSFERRING,
                    peerDeviceName = clientName,
                    bytesTransferred = offset
                )

                // 2. Send Metadata back
                val metadataLine = "METADATA|$title|$size\n"
                outputStream.write(metadataLine.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                // 3. Open video file and stream
                inputStream = context.contentResolver.openInputStream(videoUri) ?: throw Exception("Cannot open file stream")
                if (offset > 0) {
                    inputStream.skip(offset)
                }

                val buffer = ByteArray(64 * 1024) // 64KB chunks
                var bytesRead: Int
                var totalSent = offset
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSentInInterval = 0L

                while (isActive && !isCancelled.get()) {
                    // Handle Pause state
                    while (isPaused.get() && !isCancelled.get() && isActive) {
                        _transferState.value = _transferState.value?.copy(status = NearbyShareStatus.PAUSED)
                        delay(200)
                    }

                    if (isCancelled.get() || !isActive) break

                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break // EOF

                    outputStream.write(buffer, 0, bytesRead)
                    totalSent += bytesRead
                    bytesSentInInterval += bytesRead

                    val now = System.currentTimeMillis()
                    val interval = now - lastSpeedCalcTime
                    if (interval >= 1000) {
                        val speed = (bytesSentInInterval * 1000) / interval
                        _transferState.value = _transferState.value?.copy(
                            bytesTransferred = totalSent,
                            speedBytesPerSecond = speed,
                            status = NearbyShareStatus.TRANSFERRING
                        )
                        bytesSentInInterval = 0
                        lastSpeedCalcTime = now
                    }
                }

                outputStream.flush()
                if (totalSent >= size) {
                    _transferState.value = _transferState.value?.copy(
                        bytesTransferred = size,
                        speedBytesPerSecond = 0,
                        status = NearbyShareStatus.COMPLETED
                    )
                } else if (isCancelled.get()) {
                    _transferState.value = _transferState.value?.copy(
                        status = NearbyShareStatus.FAILED,
                        error = "Transfer cancelled"
                    )
                }
            } catch (e: Exception) {
                if (!isCancelled.get()) {
                    _transferState.value = _transferState.value?.copy(
                        status = NearbyShareStatus.FAILED,
                        error = e.localizedMessage ?: "Connection error"
                    )
                }
            } finally {
                try { inputStream?.close() } catch (e: Exception) {}
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    fun pauseTransfer() {
        isPaused.set(true)
    }

    fun resumeTransfer() {
        isPaused.set(false)
    }

    fun stopServer() {
        isCancelled.set(true)
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }
}
