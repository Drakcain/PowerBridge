package com.powerbridge.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import java.net.Inet4Address

object HomeRelayDiagnosticsStore {
    private const val KEY_LOG = "homeRelayPrototypeLog"
    private const val KEY_UPDATED = "homeRelayPrototypeUpdated"

    fun refreshPrototypeReport(context: Context, transport: HomeRelayTransport, reason: String): String {
        val snapshot = DiagnosticsStore.collectNetworkSnapshot(context)
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
        val status = transport.currentStatus()
        val report = buildString {
            appendLine("PowerBridge Home Relay Diagnostics")
            appendLine("Reason: $reason")
            appendLine("Timestamp: ${DiagnosticsStore.timestampNow()}")
            appendLine()
            appendLine("App: PowerBridge")
            appendLine("Mode: ${status.modeLabel}")
            appendLine("Transport: ${status.transportLabel}")
            appendLine("Pairing status: ${status.pairingLabel}")
            appendLine("Linked profile: ${status.linkedProfileLabel}")
            appendLine("Last wake request: ${status.lastWakeRequestLabel}")
            appendLine()
            appendLine("Device manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Device model: ${android.os.Build.MODEL}")
            appendLine("Android version: ${android.os.Build.VERSION.RELEASE ?: "Unknown"}")
            appendLine("Network type: ${snapshot.transport}")
            appendLine("Local IP: ${snapshot.phoneIp}")
            appendLine("Gateway: ${snapshot.gateway}")
            appendLine("Power connected: $powerConnected")
            appendLine("Battery percent: $batteryPercent")
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
        val prefs = AppConfigStore.createPrefs(context)
        prefs.edit()
            .putString(KEY_LOG, report)
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
        return report
    }

    fun clear(context: Context) {
        AppConfigStore.createPrefs(context).edit()
            .remove(KEY_LOG)
            .remove(KEY_UPDATED)
            .apply()
    }

    fun getLog(context: Context): String = AppConfigStore.createPrefs(context).getString(KEY_LOG, "").orEmpty()

    fun getLastUpdatedLabel(context: Context): String {
        val value = AppConfigStore.createPrefs(context).getLong(KEY_UPDATED, 0L)
        if (value <= 0L) return "Never"
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault())
            .format(java.time.Instant.ofEpochMilli(value))
    }

    fun copyDiagnostics(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PowerBridge Home Relay Diagnostics", getLog(context)))
    }
}
