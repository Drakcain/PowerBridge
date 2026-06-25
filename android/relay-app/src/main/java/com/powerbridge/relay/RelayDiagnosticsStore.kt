package com.powerbridge.relay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import java.net.Inet4Address
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RelayDeviceSnapshot(
    val timestamp: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val networkType: String,
    val localIp: String,
    val powerConnected: String,
    val batteryPercent: String
)

object RelayDiagnosticsStore {
    private const val PREFS_NAME = "powerbridge_relay_proto"
    private const val KEY_LOG = "relay_log"
    private const val KEY_UPDATED = "relay_updated"

    fun refreshPrototypeReport(context: Context, transport: RelayTransport, reason: String): String {
        val snapshot = collectSnapshot(context)
        val status = transport.currentStatus()
        val report = buildString {
            appendLine("PowerBridge Relay Diagnostics")
            appendLine("Mode: ${status.modeLabel}")
            appendLine("Reason: $reason")
            appendLine("Timestamp: ${snapshot.timestamp}")
            appendLine()
            appendLine("App: PowerBridge Relay")
            appendLine("Prototype: Home Device Relay Prototype")
            appendLine("Transport: ${status.transportLabel}")
            appendLine("Pairing status: ${status.pairingLabel}")
            appendLine("Linked profile: ${status.linkedProfileLabel}")
            appendLine("Last wake request: ${status.lastWakeRequestLabel}")
            appendLine()
            appendLine("Device manufacturer: ${snapshot.manufacturer}")
            appendLine("Device model: ${snapshot.model}")
            appendLine("Android version: ${snapshot.androidVersion}")
            appendLine("Network type: ${snapshot.networkType}")
            appendLine("Local IP: ${snapshot.localIp}")
            appendLine("Power connected: ${snapshot.powerConnected}")
            appendLine("Battery percent: ${snapshot.batteryPercent}")
            appendLine()
            appendLine("Warnings:")
            appendLine("- no production relay transport yet")
            appendLine("- no cloud or FCM transport yet")
            appendLine("- no paired controller yet")
            appendLine("- no wake request execution yet")
            appendLine()
            appendLine("Transport note:")
            appendLine(status.warning)
        }
        prefs(context).edit()
            .putString(KEY_LOG, report)
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
        return report
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_LOG).remove(KEY_UPDATED).apply()
    }

    fun getLog(context: Context): String = prefs(context).getString(KEY_LOG, "").orEmpty()

    fun getLastUpdatedLabel(context: Context): String {
        val value = prefs(context).getLong(KEY_UPDATED, 0L)
        if (value <= 0L) return "Never"
        return formatTimestamp(Date(value))
    }

    fun copyDiagnostics(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PowerBridge Relay Diagnostics", getLog(context)))
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun collectSnapshot(context: Context): RelayDeviceSnapshot {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryPercent = batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) "${(level * 100) / scale}%" else "Unknown"
        } ?: "Unknown"
        val powerConnected = batteryIntent?.let {
            when (it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_USB,
                BatteryManager.BATTERY_PLUGGED_AC,
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Yes"
                else -> "No"
            }
        } ?: "Unknown"

        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        val capabilities = connectivity.getNetworkCapabilities(network)
        val linkProperties = connectivity.getLinkProperties(network)
        val networkType = when {
            capabilities == null -> "Unavailable"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Other"
        }
        val localIp = linkProperties?.linkAddresses
            ?.mapNotNull { address -> address.address as? Inet4Address }
            ?.firstOrNull()
            ?.hostAddress
            ?: "Unavailable"

        return RelayDeviceSnapshot(
            timestamp = formatTimestamp(Date()),
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE ?: "Unknown",
            networkType = networkType,
            localIp = localIp,
            powerConnected = powerConnected,
            batteryPercent = batteryPercent
        )
    }

    private fun formatTimestamp(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(date)
    }
}
