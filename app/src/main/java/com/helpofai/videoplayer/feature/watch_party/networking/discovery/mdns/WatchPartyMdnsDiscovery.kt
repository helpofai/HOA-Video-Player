package com.helpofai.videoplayer.feature.watch_party.networking.discovery.mdns

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Professional mDNS discovery using Android's NsdManager.
 */
class WatchPartyMdnsDiscovery(
    private val context: Context,
    private val onServiceResolved: (NsdServiceInfo) -> Unit
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_hoa_watchparty._tcp."

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) { Log.d("mDNS", "Discovery started") }
        override fun onDiscoveryStopped(serviceType: String) { Log.d("mDNS", "Discovery stopped") }
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.d("mDNS", "Service found: ${serviceInfo.serviceName}")
            if (serviceInfo.serviceType == serviceType) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        Log.d("mDNS", "Service resolved: ${resolvedInfo.host}:${resolvedInfo.port}")
                        onServiceResolved(resolvedInfo)
                    }
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e("mDNS", "Resolve failed: $errorCode")
                    }
                })
            }
        }
        override fun onServiceLost(serviceInfo: NsdServiceInfo) { Log.d("mDNS", "Service lost") }
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { nsdManager.stopServiceDiscovery(this) }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { nsdManager.stopServiceDiscovery(this) }
    }

    fun startDiscovery() {
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun registerService(port: Int, roomName: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = roomName
            serviceType = serviceType
            setPort(port)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(si: NsdServiceInfo, errorCode: Int) { Log.e("mDNS", "Reg failed: $errorCode") }
            override fun onUnregistrationFailed(si: NsdServiceInfo, errorCode: Int) { Log.e("mDNS", "Unreg failed: $errorCode") }
            override fun onServiceRegistered(si: NsdServiceInfo) { Log.d("mDNS", "Service registered: ${si.serviceName}") }
            override fun onServiceUnregistered(si: NsdServiceInfo) { Log.d("mDNS", "Service unregistered") }
        })
    }

    fun unregisterService() {
        try {
            // Note: NsdManager requires a reference to the RegistrationListener to unregister, 
            // which needs to be stored if we implement robust unregistration.
        } catch (e: Exception) { e.printStackTrace() }
    }
}
