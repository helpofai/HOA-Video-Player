package com.helpofai.videoplayer.feature.nearbyshare

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class NearbyShareDiscovery(private val context: Context) {

    private val _discoveredDevices = MutableStateFlow<List<NearbyShareDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private var udpJob: Job? = null
    private var advertiseJob: Job? = null
    private var scanJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    companion object {
        private const val DISCOVERY_PORT = 8099
    }

    fun startDiscovery() {
        if (isRunning.getAndSet(true)) return
        _discoveredDevices.value = emptyList()

        // 1. Listen for UDP broadcast announcements
        udpJob = CoroutineScope(Dispatchers.IO).launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(DISCOVERY_PORT).apply {
                    broadcast = true
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    
                    // Message format: VIDPLAY_SHARE_AD|DeviceName|IP|Port
                    if (message.startsWith("VIDPLAY_SHARE_AD")) {
                        val parts = message.split("|")
                        val deviceName = parts.getOrNull(1) ?: "Unknown Device"
                        val ip = parts.getOrNull(2) ?: packet.address.hostAddress ?: ""
                        val port = parts.getOrNull(3)?.toIntOrNull() ?: 5050
                        
                        if (ip.isNotEmpty() && ip != getLocalIpAddress()) {
                            val newDevice = NearbyShareDevice(
                                name = deviceName,
                                ipAddress = ip,
                                port = port,
                                isSender = true
                            )
                            updateDeviceList(newDevice)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NearbyShareDiscovery", "UDP discovery error: ${e.message}")
            } finally {
                socket?.close()
            }
        }

        // 2. Perform Subnet Port Scan fallback in background
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                performSubnetScan()
                delay(15000) // Scan every 15 seconds
            }
        }
    }

    fun startAdvertising(deviceName: String, serverPort: Int) {
        advertiseJob?.cancel()
        advertiseJob = CoroutineScope(Dispatchers.IO).launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                
                val adMessage = "VIDPLAY_SHARE_AD|$deviceName|${getLocalIpAddress()}|$serverPort"
                val packetData = adMessage.toByteArray(Charsets.UTF_8)
                val broadcastAddress = getBroadcastAddress() ?: InetAddress.getByName("255.255.255.255")

                while (isActive) {
                    val packet = DatagramPacket(packetData, packetData.size, broadcastAddress, DISCOVERY_PORT)
                    socket.send(packet)
                    delay(3000) // Broadcast every 3 seconds
                }
            } catch (e: Exception) {
                android.util.Log.e("NearbyShareDiscovery", "Advertising error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    fun stopDiscovery() {
        isRunning.set(false)
        udpJob?.cancel()
        udpJob = null
        scanJob?.cancel()
        scanJob = null
        _discoveredDevices.value = emptyList()
    }

    fun stopAdvertising() {
        advertiseJob?.cancel()
        advertiseJob = null
    }

    private fun updateDeviceList(device: NearbyShareDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.ipAddress == device.ipAddress }
        if (index >= 0) {
            current[index] = device.copy(lastSeen = System.currentTimeMillis())
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    private suspend fun performSubnetScan() {
        val localIp = getLocalIpAddress() ?: return
        val subnetPrefix = localIp.substringBeforeLast(".") + "."
        
        coroutineScope {
            val jobs = (1..254).map { hostId ->
                val ip = subnetPrefix + hostId
                if (ip == localIp) return@map null
                
                launch(Dispatchers.IO) {
                    if (isPortOpen(ip, 5050, 200)) {
                        val device = NearbyShareDevice(
                            name = "Sender @ $ip",
                            ipAddress = ip,
                            port = 5050,
                            isSender = true
                        )
                        updateDeviceList(device)
                    }
                }
            }.filterNotNull()
            jobs.joinAll()
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getBroadcastAddress(): InetAddress? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val dhcp = wifiManager.dhcpInfo ?: return null
        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) {
            quads[k] = ((broadcast shr (k * 8)) and 0xFF).toByte()
        }
        return InetAddress.getByAddress(quads)
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
