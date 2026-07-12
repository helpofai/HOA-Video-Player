package com.helpofai.videoplayer.feature.nearbyshare

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpofai.videoplayer.core.data.VideoRepository
import com.helpofai.videoplayer.core.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyShareViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val server = NearbyShareServer(context)
    private val client = NearbyShareClient(context, videoRepository)
    private val discovery = NearbyShareDiscovery(context)

    val discoveredDevices = discovery.discoveredDevices

    private val _transferQueue = MutableStateFlow<List<NearbyShareTransferState>>(emptyList())
    val transferQueue = _transferQueue.asStateFlow()

    private val _localIp = MutableStateFlow<String?>("Unavailable")
    val localIp = _localIp.asStateFlow()

    private val _deviceName = MutableStateFlow("Android Device")
    val deviceName = _deviceName.asStateFlow()

    private val _qrCodeData = MutableStateFlow<String?>(null)
    val qrCodeData = _qrCodeData.asStateFlow()

    init {
        _localIp.value = discovery.getLocalIpAddress() ?: "Not Connected to Wi-Fi"
        _deviceName.value = Build.MODEL ?: "Android Device"

        viewModelScope.launch {
            combine(server.transferState, client.transferState) { serverState, clientState ->
                val queue = mutableListOf<NearbyShareTransferState>()
                if (serverState != null) queue.add(serverState)
                if (clientState != null) queue.add(clientState)
                queue
            }.collect {
                _transferQueue.value = it
            }
        }
    }

    fun startDiscovering() {
        discovery.startDiscovery()
        _localIp.value = discovery.getLocalIpAddress() ?: "Not Connected to Wi-Fi"
    }

    fun stopDiscovering() {
        discovery.stopDiscovery()
    }

    fun startShare(video: Video) {
        val port = 5050
        val ip = discovery.getLocalIpAddress() ?: "127.0.0.1"
        
        server.startServer(
            video = video.uri,
            title = video.title,
            size = video.size,
            port = port,
            deviceName = _deviceName.value
        )
        
        discovery.startAdvertising(_deviceName.value, port)
        
        _qrCodeData.value = "vidplay://share?ip=$ip&port=$port&title=${android.net.Uri.encode(video.title)}&size=${video.size}&device=${android.net.Uri.encode(_deviceName.value)}"
    }

    fun stopShare() {
        server.stopServer()
        discovery.stopAdvertising()
        _qrCodeData.value = null
    }

    fun connectToPeer(device: NearbyShareDevice) {
        client.startReceive(
            hostIp = device.ipAddress,
            port = device.port,
            clientDeviceName = _deviceName.value
        )
    }

    fun connectToQr(qrData: String) {
        try {
            val uri = android.net.Uri.parse(qrData)
            val ip = uri.getQueryParameter("ip")
            val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 5050
            val device = uri.getQueryParameter("device") ?: "Remote Peer"

            if (ip != null) {
                client.startReceive(
                    hostIp = ip,
                    port = port,
                    clientDeviceName = _deviceName.value
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseTransfer(state: NearbyShareTransferState) {
        if (state.role == NearbyShareRole.SENDER) {
            server.pauseTransfer()
        } else {
            client.pauseTransfer()
        }
    }

    fun resumeTransfer(state: NearbyShareTransferState) {
        if (state.role == NearbyShareRole.SENDER) {
            server.resumeTransfer()
        } else {
            client.resumeTransfer(
                hostIp = state.peerDeviceName,
                port = 5050,
                clientDeviceName = _deviceName.value
            )
        }
    }

    fun cancelTransfer(state: NearbyShareTransferState) {
        if (state.role == NearbyShareRole.SENDER) {
            stopShare()
        } else {
            client.stopReceive()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopShare()
        stopDiscovering()
        client.stopReceive()
    }
}
