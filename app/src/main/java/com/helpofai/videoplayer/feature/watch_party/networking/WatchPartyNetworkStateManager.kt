package com.helpofai.videoplayer.feature.watch_party.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface
import java.util.Collections

class WatchPartyNetworkStateManager private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: WatchPartyNetworkStateManager? = null
        fun getInstance(context: Context): WatchPartyNetworkStateManager {
            return instance ?: synchronized(this) {
                instance ?: WatchPartyNetworkStateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    enum class NetworkMode { NONE, WIFI_CLIENT, WIFI_HOTSPOT }

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _isHotspotEnabled = MutableStateFlow(false)
    val isHotspotEnabled: StateFlow<Boolean> = _isHotspotEnabled.asStateFlow()

    private val _localIpAddress = MutableStateFlow("--")
    val localIpAddress: StateFlow<String> = _localIpAddress.asStateFlow()

    private val _wifiSsid = MutableStateFlow("Not connected")
    val wifiSsid: StateFlow<String> = _wifiSsid.asStateFlow()

    private val _networkMode = MutableStateFlow(NetworkMode.NONE)
    val networkMode: StateFlow<NetworkMode> = _networkMode.asStateFlow()

    private val _isMetered = MutableStateFlow(false)
    val isMetered: StateFlow<Boolean> = _isMetered.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        registerNetworkCallback()
        refreshState()
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { refreshState() }
                override fun onLost(network: Network) { refreshState() }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { refreshState() }
            })
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun refreshState() {
        scope.launch {
            val wifiConnected = checkWifiConnected()
            val hotspotOn = checkHotspotEnabled()
            val ip = getLocalIp()
            val ssid = getWifiSsid()
            val metered = checkMetered()
            _isWifiConnected.value = wifiConnected
            _isHotspotEnabled.value = hotspotOn
            _localIpAddress.value = ip
            _wifiSsid.value = ssid
            _isMetered.value = metered
            _networkMode.value = when {
                hotspotOn     -> NetworkMode.WIFI_HOTSPOT
                wifiConnected -> NetworkMode.WIFI_CLIENT
                else          -> NetworkMode.NONE
            }
        }
    }

    private fun checkWifiConnected(): Boolean = try {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } catch (e: Exception) { false }

    private fun checkMetered(): Boolean = try {
        connectivityManager.isActiveNetworkMetered
    } catch (e: Exception) { false }

    private fun checkHotspotEnabled(): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as? Boolean ?: false
        } catch (e: Exception) {
            try {
                Collections.list(NetworkInterface.getNetworkInterfaces())
                    .filter { it.name.startsWith("ap") || it.name.startsWith("wlan") }
                    .any { intf ->
                        intf.isUp && !intf.isLoopback &&
                        Collections.list(intf.inetAddresses).any { addr ->
                            !addr.isLoopbackAddress && (addr.hostAddress?.indexOf(':') ?: 1) < 0
                        }
                    }
            } catch (e2: Exception) { false }
        }
    }

    private fun getLocalIp(): String = try {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .flatMap { Collections.list(it.inetAddresses) }
            .firstOrNull { !it.isLoopbackAddress && (it.hostAddress?.indexOf(':') ?: 1) < 0 }
            ?.hostAddress ?: "192.168.1.100"
    } catch (e: Exception) { "192.168.1.100" }

    private fun getWifiSsid(): String = try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo
        val raw = info?.ssid ?: return "Unknown"
        if (raw == "<unknown ssid>") "Unknown" else raw.removePrefix("\"").removeSuffix("\"")
    } catch (e: Exception) { "Unknown" }
}
