package com.helpofai.videoplayer.feature.workspace.transfers

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.telephony.TelephonyManager
import android.bluetooth.BluetoothAdapter
import java.net.NetworkInterface
import java.io.File

object TransfersNetworkHelper {

    fun getWifiSsid(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifiManager?.connectionInfo
            val ssid = info?.ssid
            if (ssid.isNullOrEmpty() || ssid == "<unknown ssid>") {
                "Local Wi-Fi Network"
            } else {
                ssid.replace("\"", "")
            }
        } catch (e: Exception) {
            "Wi-Fi Connection"
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun getCellularNetworkName(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val name = telephonyManager?.networkOperatorName
            if (name.isNullOrEmpty()) "Cellular WAN Relay" else name
        } catch (e: Exception) {
            "Cellular Network"
        }
    }

    @Suppress("DEPRECATION")
    fun getBluetoothName(): String {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.name ?: "Android Device"
        } catch (e: Exception) {
            "Bluetooth Adapter"
        }
    }

    fun getUsbStorageInfo(context: Context): String {
        return try {
            val dirs = context.getExternalFilesDirs(null)
            val usbDirs = dirs.filterNotNull().filter { Environment.isExternalStorageRemovable(it) }
            if (usbDirs.isNotEmpty()) {
                val sizes = usbDirs.map { dir ->
                    val total = dir.totalSpace
                    val formatted = String.format("%.1f GB", total / (1024.0 * 1024.0 * 1024.0))
                    "${dir.name} ($formatted)"
                }
                "USB Storage: ${sizes.joinToString(", ")}"
            } else {
                "No external USB drives detected"
            }
        } catch (e: Exception) {
            "USB OTG Hub"
        }
    }
}
