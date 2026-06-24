package com.powerbridge.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class DiagnosticEvent(
    val timestamp: String,
    val action: String,
    val step: String,
    val method: String,
    val url: String,
    val success: Boolean,
    val httpStatusCode: Int? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val suggestedCause: String = "",
    val suggestedFix: String = ""
)

data class NetworkSnapshot(
    val connected: Boolean,
    val transport: String,
    val phoneIp: String,
    val gateway: String
)

object DiagnosticsStore {
    private const val KEY_EVENTS = "diagnosticEvents"
    private const val KEY_SUMMARY = "diagnosticSummary"
    private const val KEY_LAST_ACTION = "diagnosticLastAction"
    private const val KEY_LAST_STATUS = "diagnosticLastStatus"
    private const val KEY_LAST_DETAIL = "diagnosticLastDetail"
    private const val MAX_EVENTS = 250

    fun recordEvent(context: Context, event: DiagnosticEvent) {
        val prefs = AppConfigStore.createPrefs(context)
        val events = getEvents(context).toMutableList()
        events.add(event)
        while (events.size > MAX_EVENTS) {
            events.removeAt(0)
        }
        prefs.edit()
            .putString(KEY_EVENTS, JSONArray(events.map { it.toJson() }).toString())
            .apply()
    }

    fun getEvents(context: Context): List<DiagnosticEvent> {
        val prefs = AppConfigStore.createPrefs(context)
        val raw = prefs.getString(KEY_EVENTS, "[]").orEmpty()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getJSONObject(i).toEvent())
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        AppConfigStore.createPrefs(context).edit()
            .remove(KEY_EVENTS)
            .remove(KEY_SUMMARY)
            .remove(KEY_LAST_ACTION)
            .remove(KEY_LAST_STATUS)
            .remove(KEY_LAST_DETAIL)
            .apply()
    }

    fun setSummary(context: Context, summary: String) {
        AppConfigStore.createPrefs(context).edit().putString(KEY_SUMMARY, summary).apply()
    }

    fun getSummary(context: Context): String {
        return AppConfigStore.createPrefs(context).getString(KEY_SUMMARY, "No diagnostics summary yet.").orEmpty()
    }

    fun setLastState(context: Context, action: String, status: String, detail: String) {
        AppConfigStore.createPrefs(context).edit()
            .putString(KEY_LAST_ACTION, action)
            .putString(KEY_LAST_STATUS, status)
            .putString(KEY_LAST_DETAIL, detail)
            .apply()
    }

    fun getLastAction(context: Context): String =
        AppConfigStore.createPrefs(context).getString(KEY_LAST_ACTION, "No action this session").orEmpty()

    fun getLastStatus(context: Context): String =
        AppConfigStore.createPrefs(context).getString(KEY_LAST_STATUS, "Ready").orEmpty()

    fun getLastDetail(context: Context): String =
        AppConfigStore.createPrefs(context).getString(KEY_LAST_DETAIL, "Choose an action to begin.").orEmpty()

    fun collectNetworkSnapshot(context: Context): NetworkSnapshot {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            val props = cm.getLinkProperties(network)
            val transport = when {
                capabilities == null -> "Disconnected"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi connected"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular connected"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet connected"
                else -> "Other network connected"
            }
            val phoneIp = props?.linkAddresses
                ?.firstOrNull { it.address is Inet4Address }
                ?.address
                ?.hostAddress
                .orEmpty()
                .ifBlank { "Unavailable" }
            val gateway = props?.routes
                ?.firstOrNull { it.gateway != null && it.gateway is Inet4Address }
                ?.gateway
                ?.hostAddress
                .orEmpty()
                .ifBlank { "Unavailable" }
            NetworkSnapshot(
                connected = capabilities != null,
                transport = transport,
                phoneIp = phoneIp,
                gateway = gateway
            )
        } catch (_: Exception) {
            NetworkSnapshot(
                connected = false,
                transport = "Network info unavailable",
                phoneIp = "Unavailable",
                gateway = "Unavailable"
            )
        }
    }

    fun copyDiagnostics(context: Context, profile: PowerBridgeProfile) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = buildReport(context, profile)
        clipboard.setPrimaryClip(ClipData.newPlainText("PowerBridge Diagnostics", text))
    }

    fun writeDiagnosticsReportZip(context: Context, profile: PowerBridgeProfile): File {
        val diagnosticsDir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val zipFile = File(diagnosticsDir, buildDiagnosticsZipFileName())
        val reportText = buildReport(context, profile)
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.putNextEntry(ZipEntry("powerbridge-diagnostics.txt"))
            zip.write(reportText.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return zipFile
    }

    fun buildReport(context: Context, profile: PowerBridgeProfile): String {
        val config = profile.config
        val snapshot = collectNetworkSnapshot(context)
        val events = getEvents(context)
        val wakeMethod = config.selectedWakeMethod
        val fieldStatuses = WakeMethodRegistry.fieldStatuses(config)
        val placeholderFields = AppConfigStore.placeholderFields(config)
        val selectedRoute = if (wakeMethod == WakeMethodId.HOME_RELAY) RelayRouteResolver.resolve(context, config) else null
        val relayUri = selectedRoute?.let { runCatching { URI(it.baseUrl) }.getOrNull() }
        val relayHost = relayUri?.host ?: "Not used"
        val relayPort = when {
            selectedRoute == null -> "Not used"
            (relayUri?.port ?: -1) > 0 -> relayUri?.port.toString()
            selectedRoute.baseUrl.startsWith("https://") -> "443"
            else -> "80"
        }
        return buildString {
            appendLine("PowerBridge Diagnostics")
            appendLine("Timestamp: ${timestampNow()}")
            appendLine("App version: ${appVersion(context)}")
            appendLine("Build type: ${if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) "debug" else "release"}")
            appendLine("Active profile: ${profile.name}")
            appendLine("Profile ID: ${profile.id}")
            appendLine()
            appendLine("Wake method: ${wakeMethod.displayName}")
            appendLine("Wake method implemented: ${if (wakeMethod.implemented) "yes" else "no"}")
            appendLine("Can support cellular with a home anchor: ${if (wakeMethod.supportsCellular) "yes" else "no"}")
            appendLine("Requires always-on home anchor: ${if (wakeMethod.requiresAlwaysOnBridge) "yes" else "no"}")
            if (fieldStatuses.isNotEmpty()) {
                appendLine("Required fields:")
                fieldStatuses.forEach { status ->
                    appendLine("- ${status.field.displayLabel}: ${if (status.present) "ready" else "missing"}")
                }
                appendLine()
            }
            appendLine("Placeholder fields detected:")
            if (placeholderFields.isEmpty()) {
                appendLine("- none")
            } else {
                placeholderFields.forEach { appendLine("- $it") }
            }
            appendLine()
            if (selectedRoute != null) {
                appendLine("Route mode: ${config.routeMode.displayLabel}")
                appendLine("Selected route: ${selectedRoute.displayName}")
                appendLine("Selected relay URL: ${selectedRoute.baseUrl}")
                appendLine("Route reason: ${selectedRoute.reason}")
                if (!selectedRoute.mismatchWarning.isNullOrBlank()) {
                    appendLine("Route warning: ${selectedRoute.mismatchWarning}")
                }
                appendLine("Home relay URL: ${config.homeRelayBaseUrl}")
                appendLine("Remote relay URL: ${config.remoteRelayBaseUrl}")
                appendLine("Expected home subnet: ${config.homeWifiSubnet}")
                appendLine("Expected gateway: ${config.expectedGateway}")
                if (AppConfigStore.isPlaceholderRelayBaseUrl(config.remoteRelayBaseUrl)) {
                    appendLine("Remote relay placeholder note: Remote relay URL is still the sample placeholder.")
                }
            } else {
                appendLine("Route mode: Not used by Local Wi-Fi Wake")
                appendLine("Selected route: Local UDP broadcast")
                appendLine("Selected relay URL: Not used")
                appendLine("Local wake note: Wake and Boot both send the same Magic Packet on the local LAN.")
                appendLine("Boot note: Power-on from shutdown depends on BIOS, NIC, firmware, and Windows Fast Startup support.")
            }
            appendLine()
            appendLine("Relay base URL: ${selectedRoute?.baseUrl ?: "Not used"}")
            appendLine("Relay host/IP: $relayHost")
            appendLine("Relay port: $relayPort")
            if (selectedRoute != null) {
                appendLine("Configured endpoints:")
                appendLine("- ${AppConfigStore.healthUrl(selectedRoute.baseUrl)}")
                appendLine("- ${AppConfigStore.statusUrl(selectedRoute.baseUrl)}")
                appendLine("- ${AppConfigStore.wakeModeUrl(selectedRoute.baseUrl, WakeMode.SLEEP)}")
                appendLine("- ${AppConfigStore.wakeModeUrl(selectedRoute.baseUrl, WakeMode.BOOT)}")
                appendLine("- ${AppConfigStore.legacyActivateUrl(selectedRoute.baseUrl)}")
            } else {
                appendLine("Configured local wake target:")
                appendLine("- UDP broadcast ${config.broadcastIp}:${LocalWakePacketCodec.DEFAULT_WOL_PORT}")
                appendLine("- Wake packets: ${config.sleepWakePackets}")
                appendLine("- Boot packets: ${config.remoteBootPackets}")
            }
            appendLine()
            appendLine("Phone network: ${snapshot.transport}")
            appendLine("Phone IP: ${snapshot.phoneIp}")
            appendLine("Gateway: ${snapshot.gateway}")
            appendLine("Target PC IP: ${config.targetIp.ifBlank { "Not set" }}")
            appendLine("Broadcast IP: ${config.broadcastIp}")
            appendLine("Target MAC: ${maskMacForDisplay(config.targetMac)}")
            appendLine("UDP port: ${LocalWakePacketCodec.DEFAULT_WOL_PORT}")
            appendLine("Relay token: ${if (selectedRoute == null) "not used" else if (config.token.isBlank()) "missing" else "present, masked"}")
            appendLine("Relay token value: [hidden]")
            appendLine()
            appendLine("Last action: ${getLastAction(context)}")
            appendLine("Last status: ${getLastStatus(context)}")
            appendLine("Last detail: ${getLastDetail(context)}")
            appendLine("Summary: ${getSummary(context)}")
            appendLine()
            appendLine("Diagnostic events:")
            if (events.isEmpty()) {
                appendLine("- No events recorded.")
            } else {
                events.forEach { event ->
                    appendLine("- [${event.timestamp}] ${event.action} :: ${event.step}")
                    appendLine("  ${event.method} ${event.url}")
                    appendLine("  success=${event.success} http=${event.httpStatusCode ?: "n/a"} durationMs=${event.durationMs}")
                    if (!event.errorType.isNullOrBlank() || !event.errorMessage.isNullOrBlank()) {
                        appendLine("  error=${event.errorType ?: "Unknown"} ${event.errorMessage.orEmpty()}".trimEnd())
                    }
                    if (event.suggestedCause.isNotBlank()) appendLine("  cause=${event.suggestedCause}")
                    if (event.suggestedFix.isNotBlank()) appendLine("  fix=${event.suggestedFix}")
                }
            }
            appendLine()
            appendLine("Suggested next step: ${getSummary(context)}")
        }
    }

    private fun DiagnosticEvent.toJson(): JSONObject = JSONObject().apply {
        put("timestamp", timestamp)
        put("action", action)
        put("step", step)
        put("method", method)
        put("url", url)
        put("success", success)
        put("httpStatusCode", httpStatusCode)
        put("errorType", errorType)
        put("errorMessage", errorMessage)
        put("durationMs", durationMs)
        put("suggestedCause", suggestedCause)
        put("suggestedFix", suggestedFix)
    }

    private fun JSONObject.toEvent(): DiagnosticEvent = DiagnosticEvent(
        timestamp = optString("timestamp"),
        action = optString("action"),
        step = optString("step"),
        method = optString("method"),
        url = optString("url"),
        success = optBoolean("success"),
        httpStatusCode = if (has("httpStatusCode") && !isNull("httpStatusCode")) optInt("httpStatusCode") else null,
        errorType = optString("errorType").ifBlank { null },
        errorMessage = optString("errorMessage").ifBlank { null },
        durationMs = optLong("durationMs"),
        suggestedCause = optString("suggestedCause"),
        suggestedFix = optString("suggestedFix")
    )

    fun timestampNow(): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
    }

    private fun buildDiagnosticsZipFileName(): String {
        val stamp = DateTimeFormatter.ofPattern("MM-dd-yy").format(LocalDate.now())
        return "PowerBridge-Diag-$stamp.zip"
    }

    private fun appVersion(context: Context): String {
        return try {
            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            "${pkg.versionName} (${pkg.longVersionCode})"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun maskMacForDisplay(mac: String): String {
        val normalized = LocalWakePacketCodec.normalizeMacAddress(mac)
        val parts = normalized.split(":")
        if (parts.size != 6) return mac
        return "${parts[0]}:${parts[1]}:${parts[2]}:**:**:${parts[5]}"
    }
}
