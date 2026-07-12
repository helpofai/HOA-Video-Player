package com.helpofai.videoplayer.feature.workspace.transfers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class TransfersFileServer private constructor() {
    companion object {
        const val PORT = 8082
        private val instance = TransfersFileServer()
        fun getInstance(): TransfersFileServer = instance
    }

    private var serverJob: Job? = null
    private var activeServerSocket: ServerSocket? = null
    private val _sharedFiles = mutableListOf<String>()
    val sharedFiles: List<String> get() = _sharedFiles
    private var appContext: Context? = null

    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    fun shareFile(path: String) {
        if (!_sharedFiles.contains(path)) {
            _sharedFiles.add(path)
        }
    }

    fun unshareFile(path: String) {
        _sharedFiles.remove(path)
    }

    fun start() {
        if (serverJob?.isActive == true) return
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(PORT))
                }
                activeServerSocket = serverSocket
                while (isActive) {
                    val client = serverSocket.accept()
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
                if (activeServerSocket === serverSocket) {
                    activeServerSocket = null
                }
            }
        }
    }

    fun stop() {
        try {
            activeServerSocket?.close()
        } catch (e: Exception) {}
        activeServerSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    val isRunning: Boolean get() = serverJob?.isActive == true

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30_000
            val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            val output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val url = parts[1]

            if (method != "GET") {
                sendError(output, 405, "Method Not Allowed")
                return
            }

            if (url == "/" || url.startsWith("/index.html")) {
                serveIndexPage(output)
            } else if (url.startsWith("/download")) {
                val queryParams = url.substringAfter('?', "")
                val pathParam = queryParams.split("&").firstOrNull { it.startsWith("path=") }?.substringAfter("path=", "")
                if (pathParam.isNullOrEmpty()) {
                    sendError(output, 400, "Missing path parameter")
                    return
                }
                val decodedPath = URLDecoder.decode(pathParam, "UTF-8")
                if (!_sharedFiles.contains(decodedPath)) {
                    sendError(output, 403, "File is not shared")
                    return
                }
                serveFile(decodedPath, output)
            } else {
                sendError(output, 404, "Not Found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { socket.close() } catch (e: Exception) {}
        }
    }

    private fun serveIndexPage(output: OutputStream) {
        val html = StringBuilder()
        html.append("<!DOCTYPE html><html><head>")
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
        html.append("<title>HOA Video Player Web Share Portal</title>")
        html.append("<style>")
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #090b10; color: #ecf0f1; padding: 24px; margin: 0; }")
        html.append(".container { max-width: 800px; margin: 0 auto; background: #111520; padding: 32px; border-radius: 16px; border: 1px solid #1e2535; box-shadow: 0 8px 32px rgba(0,0,0,0.3); }")
        html.append("h1 { color: #00cec9; margin-top: 0; font-size: 28px; }")
        html.append("p { color: #8e9cb0; font-size: 14px; margin-bottom: 24px; }")
        html.append(".file-list { list-style: none; padding: 0; margin: 0; }")
        html.append(".file-item { display: flex; align-items: center; justify-content: space-between; padding: 16px; background: rgba(255,255,255,0.02); margin-bottom: 12px; border-radius: 8px; border: 1px solid #1e2535; }")
        html.append(".file-name { font-weight: bold; color: #ecf0f1; font-size: 15px; }")
        html.append(".file-meta { font-size: 11px; color: #8e9cb0; margin-top: 4px; }")
        html.append(".download-btn { padding: 8px 16px; background: #00cec9; color: #090b10; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 13px; transition: background 0.2s; }")
        html.append(".download-btn:hover { background: #00b894; }")
        html.append(".empty-state { text-align: center; padding: 40px; color: #8e9cb0; }")
        html.append("</style></head><body>")
        html.append("<div class='container'>")
        html.append("<h1>HOA Web Share Portal</h1>")
        html.append("<p>Download files directly from your phone over the local network.</p>")
        html.append("<ul class='file-list'>")

        val safeContext = appContext
        if (safeContext == null) {
            html.append("<li class='empty-state'>Server is loading context.</li>")
        } else if (_sharedFiles.isEmpty()) {
            html.append("<li class='empty-state'>No files are currently shared from the app. Use the File Explorer to share files.</li>")
        } else {
            _sharedFiles.forEach { path ->
                val name: String
                val size: Long
                if (path.startsWith("content://")) {
                    val doc = DocumentFile.fromSingleUri(safeContext, Uri.parse(path))
                    name = doc?.name ?: "Unknown Document"
                    size = doc?.length() ?: 0L
                } else {
                    val file = File(path)
                    name = file.name
                    size = file.length()
                }
                
                val formattedSize = formatSize(size)
                val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                
                html.append("<li class='file-item'>")
                html.append("<div><div class='file-name'>$name</div><div class='file-meta'>$formattedSize</div></div>")
                html.append("<a class='download-btn' href='/download?path=$encodedPath' download='$name'>Download</a>")
                html.append("</li>")
            }
        }
        
        html.append("</ul></div></body></html>")

        val bodyBytes = html.toString().toByteArray(Charsets.UTF_8)
        output.write("HTTP/1.1 200 OK\r\n".toByteArray())
        output.write("Content-Type: text/html; charset=utf-8\r\n".toByteArray())
        output.write("Content-Length: ${bodyBytes.size}\r\n".toByteArray())
        output.write("Connection: close\r\n\r\n".toByteArray())
        output.write(bodyBytes)
        output.flush()
    }

    private fun serveFile(path: String, output: OutputStream) {
        val safeContext = appContext ?: return
        try {
            val ins = if (path.startsWith("content://")) {
                safeContext.contentResolver.openInputStream(Uri.parse(path))
            } else {
                val file = File(path)
                if (file.exists()) FileInputStream(file) else null
            }

            if (ins == null) {
                sendError(output, 404, "File not found")
                return
            }

            val size = if (path.startsWith("content://")) {
                DocumentFile.fromSingleUri(safeContext, Uri.parse(path))?.length() ?: 0L
            } else {
                File(path).length()
            }

            val name = if (path.startsWith("content://")) {
                DocumentFile.fromSingleUri(safeContext, Uri.parse(path))?.name ?: "file"
            } else {
                File(path).name
            }

            val ext = name.substringAfterLast('.', "").lowercase()
            val mimeType = when (ext) {
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "mp3" -> "audio/mpeg"
                "txt" -> "text/plain"
                "srt" -> "text/plain"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "zip" -> "application/zip"
                else -> "application/octet-stream"
            }

            output.write("HTTP/1.1 200 OK\r\n".toByteArray())
            output.write("Content-Type: $mimeType\r\n".toByteArray())
            output.write("Content-Length: $size\r\n".toByteArray())
            output.write("Content-Disposition: attachment; filename=\"$name\"\r\n".toByteArray())
            output.write("Connection: close\r\n\r\n".toByteArray())

            ins.use { input ->
                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendError(output: OutputStream, code: Int, message: String) {
        val html = "<html><body><h1>$code $message</h1></body></html>"
        val bodyBytes = html.toByteArray(Charsets.UTF_8)
        output.write("HTTP/1.1 $code $message\r\n".toByteArray())
        output.write("Content-Type: text/html\r\n".toByteArray())
        output.write("Content-Length: ${bodyBytes.size}\r\n".toByteArray())
        output.write("Connection: close\r\n\r\n".toByteArray())
        output.write(bodyBytes)
        output.flush()
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}
