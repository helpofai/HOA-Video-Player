package com.helpofai.videoplayer.feature.watch_party.networking.discovery

import android.content.Context
import android.net.nsd.NsdServiceInfo
import com.helpofai.videoplayer.feature.watch_party.networking.discovery.mdns.WatchPartyMdnsDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the discovery of available Watch Party hosts on the local network.
 */
class WatchPartyDiscoveryManager(context: Context) {
    
    private val mdnsDiscovery = WatchPartyMdnsDiscovery(context) { serviceInfo ->
        _discoveredHosts.update { current ->
            if (current.any { it.serviceName == serviceInfo.serviceName }) current 
            else current + serviceInfo
        }
    }
    
    private val _discoveredHosts = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredHosts = _discoveredHosts.asStateFlow()

    fun startDiscovery() {
        mdnsDiscovery.startDiscovery()
    }

    fun stopDiscovery() {
        mdnsDiscovery.stopDiscovery()
    }
}
