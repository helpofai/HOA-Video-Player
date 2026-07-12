package com.helpofai.videoplayer.feature.watch_party.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.watch_party.notification.WatchPartyNotificationManager
import com.helpofai.videoplayer.feature.watch_party.domain.models.ChatMessage
import com.helpofai.videoplayer.feature.watch_party.domain.models.Reaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONArray

class WatchPartySessionManager {
    companion object {
        private val instance = WatchPartySessionManager()
        fun getInstance(): WatchPartySessionManager = instance
    }

    private val _isClientMode = MutableStateFlow(false)
    var isClientMode: Boolean
        get() = _isClientMode.value
        set(value) { _isClientMode.value = value }
    val isClientModeFlow = _isClientMode.asStateFlow()

    private val _isFullPlayerActive = MutableStateFlow(false)
    val isFullPlayerActive: StateFlow<Boolean> = _isFullPlayerActive.asStateFlow()

    fun setFullPlayerActive(active: Boolean) {
        _isFullPlayerActive.value = active
    }

    private var currentTunnelPort: Int = 9990
    private var currentStreamPort: Int = 9980

    private fun findAvailablePort(startPort: Int): Int {
        var p = startPort
        while (p < startPort + 100) {
            try {
                val tempSocket = java.net.ServerSocket()
                tempSocket.reuseAddress = true
                tempSocket.bind(java.net.InetSocketAddress(p))
                tempSocket.close()
                return p
            } catch (e: Exception) {
                p++
            }
        }
        return startPort
    }

    private val _activeSession = MutableStateFlow<WatchPartySession?>(null)
    val activeSession: StateFlow<WatchPartySession?> = _activeSession.asStateFlow()

    private val _pendingRequest = MutableStateFlow<WatchPartyDevice?>(null)
    val pendingRequest: StateFlow<WatchPartyDevice?> = _pendingRequest.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _reactions = MutableStateFlow<List<Reaction>>(emptyList())
    val reactions: StateFlow<List<Reaction>> = _reactions.asStateFlow()

    // Set by the PlayerScreen's Watch Party toggle - this is the video that will be streamed
    private val _currentStreamingVideo = MutableStateFlow<Video?>(null)
    val currentStreamingVideo: StateFlow<Video?> = _currentStreamingVideo.asStateFlow()

    private var context: android.content.Context? = null

    fun init(ctx: android.content.Context) {
        this.context = ctx.applicationContext
    }

    fun clearPendingRequest() {
        _pendingRequest.value = null
    }

    fun setStreamingVideo(video: Video?) {
        _currentStreamingVideo.value = video
        if (video != null && !isClientMode) {
            val ctx = context
            if (ctx != null) {
                com.helpofai.videoplayer.feature.watch_party.streaming.WatchPartyVideoStreamServer
                    .getInstance().start(ctx, video.uri, currentStreamPort)
            } else {
                val file = java.io.File(video.path)
                if (file.exists()) {
                    com.helpofai.videoplayer.feature.watch_party.streaming.WatchPartyVideoStreamServer
                        .getInstance().start(file, currentStreamPort)
                }
            }
            _activeSession.value = _activeSession.value?.copy(video = video)
        } else {
            com.helpofai.videoplayer.feature.watch_party.streaming.WatchPartyVideoStreamServer
                .getInstance().stop()
            _activeSession.value = _activeSession.value?.copy(video = null)
        }
    }

    fun clearStreamingVideo() {
        _currentStreamingVideo.value = null
        com.helpofai.videoplayer.feature.watch_party.streaming.WatchPartyVideoStreamServer
            .getInstance().stop()
        _activeSession.value = _activeSession.value?.copy(video = null)
    }

    private val notificationManager = WatchPartyNotificationManager.getInstance()

    // Sync Mode: when ON, every video the host plays is automatically streamed
    private val _isSyncModeEnabled = MutableStateFlow(false)
    val isSyncModeEnabled: StateFlow<Boolean> = _isSyncModeEnabled.asStateFlow()

    fun toggleSyncMode() {
        _isSyncModeEnabled.value = !_isSyncModeEnabled.value
    }

    fun setSyncMode(enabled: Boolean) {
        _isSyncModeEnabled.value = enabled
        if (!enabled) {
            clearStreamingVideo()
        }
    }

    private var serverJob: kotlinx.coroutines.Job? = null
    private var clientJob: kotlinx.coroutines.Job? = null
    private var discoveryUdpJob: kotlinx.coroutines.Job? = null

    private val clientSockets = java.util.concurrent.CopyOnWriteArrayList<java.net.Socket>()
    private var hostSocket: java.net.Socket? = null

    fun sendMessage(text: String) {
        val session = _activeSession.value ?: return
        val deviceId = if (isClientMode) "client_id" else "host_id"
        val deviceName = if (isClientMode) {
            val brand = android.os.Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
            val model = android.os.Build.MODEL ?: "Device"
            "$brand $model"
        } else {
            session.devices.firstOrNull { it.isHost }?.name ?: "Host"
        }
        val msg = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            senderId = deviceId,
            senderName = deviceName,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        _chatMessages.value = _chatMessages.value + msg

        val json = JSONObject().apply {
            put("command", "chat")
            put("id", msg.id)
            put("senderId", msg.senderId)
            put("senderName", msg.senderName)
            put("text", msg.text)
            put("timestamp", msg.timestamp)
        }
        val msgStr = json.toString() + "\n"
        if (isClientMode) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    hostSocket?.getOutputStream()?.write(msgStr.toByteArray(Charsets.UTF_8))
                    hostSocket?.getOutputStream()?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            broadcastToClients(msgStr, excludeSocket = null)
        }
    }

    fun sendReaction(emoji: String) {
        val session = _activeSession.value ?: return
        val deviceId = if (isClientMode) "client_id" else "host_id"
        val deviceName = if (isClientMode) {
            val brand = android.os.Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
            val model = android.os.Build.MODEL ?: "Device"
            "$brand $model"
        } else {
            session.devices.firstOrNull { it.isHost }?.name ?: "Host"
        }
        val r = Reaction(
            id = java.util.UUID.randomUUID().toString(),
            senderId = deviceId,
            emoji = emoji,
            timestamp = System.currentTimeMillis()
        )
        _reactions.value = _reactions.value + r

        val json = JSONObject().apply {
            put("command", "reaction")
            put("id", r.id)
            put("senderId", r.senderId)
            put("emoji", r.emoji)
            put("timestamp", r.timestamp)
        }
        val msgStr = json.toString() + "\n"
        if (isClientMode) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    hostSocket?.getOutputStream()?.write(msgStr.toByteArray(Charsets.UTF_8))
                    hostSocket?.getOutputStream()?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            broadcastToClients(msgStr, excludeSocket = null)
        }
    }

    fun sendPlaybackControl(isPlaying: Boolean, positionMs: Long) {
        if (!isClientMode) {
            updatePlaybackState(positionMs, isPlaying)
            return
        }
        val json = JSONObject().apply {
            put("command", "playback_control")
            put("isPlaying", isPlaying)
            put("position", positionMs)
        }
        val msgStr = json.toString() + "\n"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                hostSocket?.getOutputStream()?.write(msgStr.toByteArray(Charsets.UTF_8))
                hostSocket?.getOutputStream()?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun broadcastToClients(msgStr: String, excludeSocket: java.net.Socket?) {
        clientSockets.forEach { s ->
            if (s != excludeSocket) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        s.getOutputStream().write(msgStr.toByteArray(Charsets.UTF_8))
                        s.getOutputStream().flush()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val pendingDeepLink = MutableStateFlow<String?>(null)

    fun createSession(
        name: String,
        hostIp: String,
        hostDeviceName: String,
        video: Video?,
        securityToken: String,
        usePassword: Boolean = false,
        password: String = "",
        allowPlayPause: Boolean = false,
        allowSeek: Boolean = false,
        allowVolume: Boolean = true,
        allowNextPrev: Boolean = false,
        allowGestures: Boolean = true,
        allowReactions: Boolean = true,
        allowSubtitleToggle: Boolean = false,
        allowAudioTrack: Boolean = false,
        allowFolderQueue: Boolean = false,
        maxUsers: Int = 10,
        id: String? = null,
        tunnelPort: Int = 9990
    ): WatchPartySession {
        if (id == null) {
            // We are Host -> Find and allocate available ports
            currentTunnelPort = findAvailablePort(9990)
            currentStreamPort = findAvailablePort(9980)
        } else {
            // We are Client
            currentTunnelPort = tunnelPort
        }

        val session = WatchPartySession(
            id = id ?: ("wp_" + System.currentTimeMillis()),
            name = name,
            hostIp = hostIp,
            port = currentStreamPort,
            video = video,
            securityToken = securityToken,
            usePassword = usePassword,
            password = password,
            allowPlayPause = allowPlayPause,
            allowSeek = allowSeek,
            allowVolume = allowVolume,
            allowNextPrev = allowNextPrev,
            allowGestures = allowGestures,
            allowReactions = allowReactions,
            allowSubtitleToggle = allowSubtitleToggle,
            allowAudioTrack = allowAudioTrack,
            allowFolderQueue = allowFolderQueue,
            maxUsers = maxUsers,
            devices = listOf(
                WatchPartyDevice(
                    id = "host_id",
                    name = hostDeviceName,
                    ipAddress = hostIp,
                    isHost = true,
                    status = "Idle"
                )
            )
        )
        _activeSession.value = session
        notificationManager.notifySessionCreated(name)

        if (id == null) {
            // We are Host -> Start TCP socket server & UDP discovery responder
            startHostTunnelServer(currentTunnelPort)
            startHostDiscoveryUdpServer(name, currentTunnelPort)
        } else {
            // We are Client -> Connect client tunnel to host
            startClientTunnelConnection(hostIp, currentTunnelPort)
        }

        return session
    }

    fun updateSession(
        name: String,
        usePassword: Boolean,
        password: String,
        maxUsers: Int,
        allowPlayPause: Boolean,
        allowSeek: Boolean,
        allowVolume: Boolean,
        allowNextPrev: Boolean,
        allowGestures: Boolean,
        allowReactions: Boolean,
        allowSubtitleToggle: Boolean,
        allowAudioTrack: Boolean,
        allowFolderQueue: Boolean
    ) {
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(
            name = name,
            usePassword = usePassword,
            password = password,
            maxUsers = maxUsers,
            allowPlayPause = allowPlayPause,
            allowSeek = allowSeek,
            allowVolume = allowVolume,
            allowNextPrev = allowNextPrev,
            allowGestures = allowGestures,
            allowReactions = allowReactions,
            allowSubtitleToggle = allowSubtitleToggle,
            allowAudioTrack = allowAudioTrack,
            allowFolderQueue = allowFolderQueue
        )
    }
    
    fun endSession() {
        serverJob?.cancel()
        serverJob = null
        clientJob?.cancel()
        clientJob = null
        discoveryUdpJob?.cancel()
        discoveryUdpJob = null
        clientSockets.forEach { try { it.close() } catch(e: Exception){} }
        clientSockets.clear()
        try { hostSocket?.close() } catch(e: Exception){}
        hostSocket = null
        com.helpofai.videoplayer.feature.watch_party.streaming.WatchPartyVideoStreamServer
            .getInstance().stop()
        _currentStreamingVideo.value = null
        _activeSession.value = null
        notificationManager.notifySessionEnded()
    }
    
    fun addDevice(device: WatchPartyDevice) {
        val current = _activeSession.value ?: return
        if (current.devices.any { it.id == device.id }) return
        val updated = current.copy(devices = current.devices + device)
        _activeSession.value = updated
        notificationManager.notifyDeviceConnected(device, updated.devices.size)
    }
    
    fun removeDevice(deviceId: String) {
        val current = _activeSession.value ?: return
        val removedDevice = current.devices.firstOrNull { it.id == deviceId }
        val updated = current.copy(devices = current.devices.filterNot { it.id == deviceId })
        _activeSession.value = updated
        if (removedDevice != null) {
            notificationManager.notifyDeviceDisconnected(removedDevice, updated.devices.size)
        }
    }
    
    fun updatePlaybackState(positionMs: Long, isPlaying: Boolean) {
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(currentPositionMs = positionMs, isPlaying = isPlaying)
    }
    
    fun updateDeviceStatus(deviceId: String, status: String) {
        val current = _activeSession.value ?: return
        val updatedDevices = current.devices.map {
            if (it.id == deviceId) it.copy(status = status) else it
        }
        _activeSession.value = current.copy(devices = updatedDevices)
    }

    fun setDevicePermission(deviceId: String, playPause: Boolean, seek: Boolean, volume: Boolean) {
        val current = _activeSession.value ?: return
        val updatedDevices = current.devices.map {
            if (it.id == deviceId) {
                it.copy(hasPlayPausePermission = playPause, hasSeekPermission = seek, hasVolumePermission = volume)
            } else it
        }
        _activeSession.value = current.copy(devices = updatedDevices)
    }

    fun banDevice(deviceId: String) {
        val current = _activeSession.value ?: return
        val updatedDevices = current.devices.map {
            if (it.id == deviceId) it.copy(isBanned = true) else it
        }
        _activeSession.value = current.copy(devices = updatedDevices)
    }

    fun unbanDevice(deviceId: String) {
        val current = _activeSession.value ?: return
        val updatedDevices = current.devices.map {
            if (it.id == deviceId) it.copy(isBanned = false) else it
        }
        _activeSession.value = current.copy(devices = updatedDevices)
    }

    private fun startHostTunnelServer(port: Int) {
        serverJob?.cancel()
        clientSockets.clear()
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            var serverSocket: java.net.ServerSocket? = null
            try {
                android.util.Log.d("WatchPartySession", "Host: Binding ServerSocket on port $port...")
                serverSocket = java.net.ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                android.util.Log.d("WatchPartySession", "Host: ServerSocket bound successfully on port $port")
                
                // Collect and broadcast Host state changes in real-time
                launch {
                    activeSession.collect { session ->
                        if (session != null) {
                            val jsonStr = sessionToJson(session) + "\n"
                            android.util.Log.d("WatchPartySession", "Host: Broadcasting activeSession update to ${clientSockets.size} clients: $jsonStr")
                            val iterator = clientSockets.iterator()
                            while (iterator.hasNext()) {
                                val s = iterator.next()
                                try {
                                    s.getOutputStream().write(jsonStr.toByteArray(Charsets.UTF_8))
                                    s.getOutputStream().flush()
                                } catch (e: Exception) {
                                    android.util.Log.e("WatchPartySession", "Host: Error writing to client socket, closing & removing: ${e.message}")
                                    try { s.close() } catch(ex: Exception){}
                                    clientSockets.remove(s)
                                }
                            }
                        }
                    }
                }
                
                while (isActive) {
                    val socket = serverSocket.accept().apply {
                        try {
                            tcpNoDelay = true
                            sendBufferSize = 512 * 1024
                            receiveBufferSize = 512 * 1024
                        } catch (e: Exception) {}
                    }
                    clientSockets.add(socket)
                    android.util.Log.d("WatchPartySession", "Host: Accepted new client connection from ${socket.remoteSocketAddress}")

                    // Send initial state immediately
                    activeSession.value?.let { session ->
                        val jsonStr = sessionToJson(session) + "\n"
                        try {
                            android.util.Log.d("WatchPartySession", "Host: Sending initial session state to new client: $jsonStr")
                            socket.getOutputStream().write(jsonStr.toByteArray(Charsets.UTF_8))
                            socket.getOutputStream().flush()
                        } catch (e: Exception) {
                            android.util.Log.e("WatchPartySession", "Host: Error sending initial state, closing: ${e.message}")
                            try { socket.close() } catch(ex: Exception){}
                            clientSockets.remove(socket)
                        }
                    }

                    // Read client-side actions (chat, reactions, hello)
                    launch {
                        val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                        try {
                            while (isActive) {
                                val line = reader.readLine() ?: break
                                val action = JSONObject(line)
                                val cmd = action.optString("command")
                                when (cmd) {
                                    "client_hello" -> {
                                        // A new client identified itself - notify the host
                                        val clientName = action.optString("deviceName", "Unknown Device")
                                        val roomName = activeSession.value?.name ?: ""
                                        notificationManager.notifyClientConnectedToRoom(clientName, roomName)
                                        // Add device to session
                                        val deviceId = action.optString("deviceId", java.util.UUID.randomUUID().toString())
                                        addDevice(WatchPartyDevice(
                                            id = deviceId,
                                            name = clientName,
                                            ipAddress = socket.inetAddress.hostAddress ?: "",
                                            isHost = false,
                                            status = "Connected"
                                        ))
                                    }
                                    "reaction" -> {
                                        val r = Reaction(
                                            id = action.getString("id"),
                                            senderId = action.getString("senderId"),
                                            emoji = action.getString("emoji"),
                                            timestamp = action.getLong("timestamp")
                                        )
                                        _reactions.value = _reactions.value + r
                                        // Broadcast reaction to other clients
                                        broadcastToClients(action.toString() + "\n", excludeSocket = socket)
                                    }
                                    "chat" -> {
                                        val msg = ChatMessage(
                                            id = action.getString("id"),
                                            senderId = action.getString("senderId"),
                                            senderName = action.getString("senderName"),
                                            text = action.getString("text"),
                                            timestamp = action.getLong("timestamp")
                                        )
                                        _chatMessages.value = _chatMessages.value + msg
                                        // Broadcast chat message to other clients
                                        broadcastToClients(action.toString() + "\n", excludeSocket = socket)
                                    }
                                    "playback_control" -> {
                                        val playPause = action.optBoolean("isPlaying")
                                        val position = action.optLong("position")
                                        updatePlaybackState(position, playPause)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WatchPartySession", "Host: Client connection lost or error: ${e.message}")
                        } finally {
                            try { socket.close() } catch (e: Exception) {}
                            clientSockets.remove(socket)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WatchPartySession", "Host: ServerSocket error or stopped: ${e.message}")
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    private fun startClientTunnelConnection(hostIp: String, port: Int) {
        clientJob?.cancel()
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            val deviceName = try {
                val brand = android.os.Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
                val model = android.os.Build.MODEL ?: "Device"
                "$brand $model"
            } catch (e: Exception) {
                "Android Client"
            }
            val deviceId = try {
                android.os.Build.ID ?: java.util.UUID.randomUUID().toString()
            } catch (e: Exception) {
                java.util.UUID.randomUUID().toString()
            }

            while (isActive) {
                var socket: java.net.Socket? = null
                try {
                    android.util.Log.d("WatchPartySession", "Client: Connecting to host at $hostIp:$port...")
                    socket = java.net.Socket().apply {
                        try {
                            tcpNoDelay = true
                            sendBufferSize = 512 * 1024
                            receiveBufferSize = 512 * 1024
                        } catch (e: Exception) {}
                    }
                    socket.connect(java.net.InetSocketAddress(hostIp, port), 5000)
                    hostSocket = socket
                    android.util.Log.d("WatchPartySession", "Client: Connected successfully to host")

                    // Introduce ourselves to the host
                    val hello = JSONObject().apply {
                        put("command", "client_hello")
                        put("deviceName", deviceName)
                        put("deviceId", deviceId)
                    }
                    socket.getOutputStream().write((hello.toString() + "\n").toByteArray(Charsets.UTF_8))
                    socket.getOutputStream().flush()
                    android.util.Log.d("WatchPartySession", "Client: Sent client_hello to host")

                    // Notify client UI
                    val roomName = activeSession.value?.name ?: ""
                    notificationManager.notifyConnectedToStream(roomName)

                    val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        android.util.Log.d("WatchPartySession", "Client: Received line: $line")
                        
                        if (line.trim().startsWith("{")) {
                            val json = JSONObject(line)
                            val cmd = json.optString("command")
                            if (cmd == "chat") {
                                val msg = ChatMessage(
                                    id = json.getString("id"),
                                    senderId = json.getString("senderId"),
                                    senderName = json.getString("senderName"),
                                    text = json.getString("text"),
                                    timestamp = json.getLong("timestamp")
                                )
                                _chatMessages.value = _chatMessages.value + msg
                            } else if (cmd == "reaction") {
                                val r = Reaction(
                                    id = json.getString("id"),
                                    senderId = json.getString("senderId"),
                                    emoji = json.getString("emoji"),
                                    timestamp = json.getLong("timestamp")
                                )
                                _reactions.value = _reactions.value + r
                            } else {
                                // Assume it's a full session update
                                val session = jsonToSession(line)
                                _activeSession.value = session
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WatchPartySession", "Client: Error in connection loop: ${e.message}")
                    // Retry connection in 3 seconds
                    delay(3000L)
                } finally {
                    try { socket?.close() } catch(e: Exception){}
                    hostSocket = null
                }
            }
        }
    }

    private fun sessionToJson(session: WatchPartySession): String {
        val json = JSONObject()
        json.put("id", session.id)
        json.put("name", session.name)
        json.put("hostIp", session.hostIp)
        json.put("port", session.port)
        json.put("currentPositionMs", session.currentPositionMs)
        json.put("isPlaying", session.isPlaying)
        json.put("maxUsers", session.maxUsers)
        json.put("securityToken", session.securityToken)
        json.put("usePassword", session.usePassword)
        json.put("password", session.password)
        json.put("allowPlayPause", session.allowPlayPause)
        json.put("allowSeek", session.allowSeek)
        json.put("allowVolume", session.allowVolume)
        json.put("allowNextPrev", session.allowNextPrev)
        json.put("allowGestures", session.allowGestures)
        json.put("allowReactions", session.allowReactions)
        json.put("allowSubtitleToggle", session.allowSubtitleToggle)
        json.put("allowAudioTrack", session.allowAudioTrack)
        json.put("allowFolderQueue", session.allowFolderQueue)
        
        val videoJson = session.video?.let { v ->
            val vj = JSONObject()
            vj.put("id", v.id)
            vj.put("title", v.title)
            vj.put("uri", v.uri.toString())
            vj.put("path", v.path)
            vj.put("duration", v.duration)
            vj.put("size", v.size)
            vj
        }
        json.put("video", videoJson ?: JSONObject.NULL)
        
        val devicesArr = JSONArray()
        session.devices.forEach { d ->
            val dj = JSONObject()
            dj.put("id", d.id)
            dj.put("name", d.name)
            dj.put("ipAddress", d.ipAddress)
            dj.put("batteryLevel", d.batteryLevel)
            dj.put("connectionSpeed", d.connectionSpeed)
            dj.put("latency", d.latency)
            dj.put("isHost", d.isHost)
            dj.put("hasPlayPausePermission", d.hasPlayPausePermission)
            dj.put("hasSeekPermission", d.hasSeekPermission)
            dj.put("hasVolumePermission", d.hasVolumePermission)
            dj.put("status", d.status)
            dj.put("isBanned", d.isBanned)
            devicesArr.put(dj)
        }
        json.put("devices", devicesArr)
        
        return json.toString()
    }

    private fun jsonToSession(jsonStr: String): WatchPartySession {
        val json = JSONObject(jsonStr)
        val videoObj = if (json.isNull("video")) null else json.optJSONObject("video")
        val video = videoObj?.let { vj ->
            Video(
                id = vj.getLong("id"),
                title = vj.getString("title"),
                uri = android.net.Uri.parse(vj.getString("uri")),
                path = vj.getString("path"),
                duration = vj.getLong("duration"),
                size = vj.getLong("size"),
                dateAdded = System.currentTimeMillis()
            )
        }
        
        val devicesList = mutableListOf<WatchPartyDevice>()
        val devicesArr = json.optJSONArray("devices")
        if (devicesArr != null) {
            for (i in 0 until devicesArr.length()) {
                val dj = devicesArr.getJSONObject(i)
                devicesList.add(
                    WatchPartyDevice(
                        id = dj.getString("id"),
                        name = dj.getString("name"),
                        ipAddress = dj.getString("ipAddress"),
                        batteryLevel = dj.optInt("batteryLevel", 100),
                        connectionSpeed = dj.optDouble("connectionSpeed", 45.0).toFloat(),
                        latency = dj.optInt("latency", 10),
                        isHost = dj.optBoolean("isHost", false),
                        hasPlayPausePermission = dj.optBoolean("hasPlayPausePermission", true),
                        hasSeekPermission = dj.optBoolean("hasSeekPermission", false),
                        hasVolumePermission = dj.optBoolean("hasVolumePermission", true),
                        status = dj.optString("status", "Idle"),
                        isBanned = dj.optBoolean("isBanned", false)
                    )
                )
            }
        }
        
        return WatchPartySession(
            id = json.getString("id"),
            name = json.getString("name"),
            hostIp = json.getString("hostIp"),
            port = json.optInt("port", 8080),
            video = video,
            devices = devicesList,
            currentPositionMs = json.getLong("currentPositionMs"),
            isPlaying = json.getBoolean("isPlaying"),
            maxUsers = json.optInt("maxUsers", 10),
            securityToken = json.optString("securityToken", ""),
            usePassword = json.optBoolean("usePassword", false),
            password = json.optString("password", ""),
            allowPlayPause = json.optBoolean("allowPlayPause", false),
            allowSeek = json.optBoolean("allowSeek", false),
            allowVolume = json.optBoolean("allowVolume", true),
            allowNextPrev = json.optBoolean("allowNextPrev", false),
            allowGestures = json.optBoolean("allowGestures", true),
            allowReactions = json.optBoolean("allowReactions", true),
            allowSubtitleToggle = json.optBoolean("allowSubtitleToggle", false),
            allowAudioTrack = json.optBoolean("allowAudioTrack", false),
            allowFolderQueue = json.optBoolean("allowFolderQueue", false)
        )
    }

    private fun startHostDiscoveryUdpServer(roomName: String, port: Int) {
        discoveryUdpJob?.cancel()
        discoveryUdpJob = CoroutineScope(Dispatchers.IO).launch {
            var socket: java.net.DatagramSocket? = null
            try {
                socket = java.net.DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(8079))
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = java.net.DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    if (message == "DISCOVER_WATCH_PARTY_REQUEST" || message == "WATCH_PARTY_DISCOVER") {
                        val response = "WATCH_PARTY_HOST:$roomName:$port:$currentStreamPort"
                        val responseBytes = response.toByteArray(Charsets.UTF_8)
                        val replyPacket = java.net.DatagramPacket(
                            responseBytes,
                            responseBytes.size,
                            packet.address,
                            packet.port
                        )
                        socket.send(replyPacket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }
}
