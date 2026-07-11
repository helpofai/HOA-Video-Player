package com.helpofai.videoplayer.feature.watch_party.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.Collections

data class DiscoveredHost(val name: String, val ipAddress: String, val port: Int, val streamPort: Int = 9980)

/**
 * Real device discovery service using UDP broadcast scanning on the local subnet.
 * 
 * How it works:
 * 1. When startDiscovery() is called, it broadcasts a UDP "WATCH_PARTY_DISCOVER" message
 *    on the subnet broadcast address (port 8079).
 * 2. Any host running a Watch Party session should respond with "WATCH_PARTY_HOST:<DeviceName>:<Port>".
 * 3. Discovered hosts are added to the discoveredHosts StateFlow.
 * 4. Falls back to subnet ARP scanning if no UDP responses within 3 seconds.
 */
class WatchPartyDeviceDiscoveryService {
    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts

    private var discoveryJob: Job? = null
    private val discoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val DISCOVERY_PORT = 8079
        private const val DISCOVERY_MSG = "WATCH_PARTY_DISCOVER"
        private const val DISCOVERY_TIMEOUT_MS = 4000
    }

    /**
     * Start real network discovery via UDP broadcast.
     * Sends a broadcast message and listens for host responses.
     * Falls back to scanning nearby subnet IPs if no UDP response.
     */
    fun startDiscovery() {
        stopDiscovery()
        _discoveredHosts.value = emptyList()

        discoveryJob = discoveryScope.launch {
            val found = mutableListOf<DiscoveredHost>()

            // Step 1: UDP broadcast discovery
            try {
                val socket = DatagramSocket()
                socket.soTimeout = DISCOVERY_TIMEOUT_MS
                socket.broadcast = true

                val broadcastAddr = getBroadcastAddress()
                val msgBytes = DISCOVERY_MSG.toByteArray()
                val sendPacket = DatagramPacket(
                    msgBytes, msgBytes.size,
                    InetAddress.getByName(broadcastAddr), DISCOVERY_PORT
                )
                socket.send(sendPacket)

                // Listen for responses
                val buffer = ByteArray(1024)
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT_MS) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(receivePacket)
                        val response = String(receivePacket.data, 0, receivePacket.length).trim()

                        // Expected format: WATCH_PARTY_HOST:<DeviceName>:<TunnelPort>:<StreamPort>
                        if (response.startsWith("WATCH_PARTY_HOST:")) {
                            val parts = response.split(":")
                            if (parts.size >= 3) {
                                val hostName = parts[1]
                                val hostPort = parts[2].toIntOrNull() ?: 9990
                                val streamPort = if (parts.size >= 4) (parts[3].toIntOrNull() ?: 9980) else (if (hostPort == 8085) 8080 else (hostPort - 10))
                                val hostIp = receivePacket.address.hostAddress ?: continue
                                
                                // Avoid duplicates
                                if (found.none { it.ipAddress == hostIp }) {
                                    found.add(DiscoveredHost(hostName, hostIp, hostPort, streamPort))
                                    _discoveredHosts.value = found.toList()
                                }
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        break
                    }
                }
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Step 2: Subnet reachability scan fallback
            // If no hosts found via UDP, scan nearby IPs on subnet for open ports (9990, 9991, 9992, 8085)
            if (found.isEmpty()) {
                val localIp = getLocalIpAddress()
                val subnet = localIp.substringBeforeLast(".")
                val localLastOctet = localIp.substringAfterLast(".").toIntOrNull() ?: 1

                val scanJobs = (1..254).filter { it != localLastOctet }.map { octet ->
                    async {
                        val ip = "$subnet.$octet"
                        val portsToCheck = listOf(9990, 9991, 9992, 8085)
                        var activePort = 0
                        for (port in portsToCheck) {
                            try {
                                val sock = java.net.Socket()
                                sock.connect(InetSocketAddress(ip, port), 80)
                                sock.close()
                                activePort = port
                                break
                            } catch (e: Exception) {
                                // Ignore and try next port
                            }
                        }
                        if (activePort > 0) {
                            val streamPort = if (activePort == 8085) 8080 else (activePort - 10)
                            DiscoveredHost("Device at $ip", ip, activePort, streamPort)
                        } else {
                            null
                        }
                    }
                }

                scanJobs.forEach { job ->
                    val result = job.await()
                    if (result != null && found.none { it.ipAddress == result.ipAddress }) {
                        found.add(result)
                        _discoveredHosts.value = found.toList()
                    }
                }
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _discoveredHosts.value = emptyList()
    }

    private fun getBroadcastAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (ifAddr in intf.interfaceAddresses) {
                    val broadcast = ifAddr.broadcast
                    if (broadcast != null) {
                        return broadcast.hostAddress ?: "255.255.255.255"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "255.255.255.255"
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: continue
                        if (sAddr.indexOf(':') < 0) return sAddr
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.1.1"
    }
}
