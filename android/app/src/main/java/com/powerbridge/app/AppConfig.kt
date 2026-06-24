package com.powerbridge.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import androidx.appcompat.app.AppCompatDelegate
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AppConfig(
    val selectedWakeMethod: WakeMethodId,
    val relayBaseUrl: String,
    val homeRelayBaseUrl: String,
    val remoteRelayBaseUrl: String,
    val homeWifiSubnet: String,
    val expectedGateway: String,
    val routeMode: RelayRouteMode,
    val token: String,
    val targetMac: String,
    val targetIp: String,
    val broadcastIp: String,
    val sleepWakeTimeoutSeconds: Int,
    val remoteBootTimeoutSeconds: Int,
    val sleepWakePackets: Int,
    val remoteBootPackets: Int,
    val themeMode: String,
    val themeStyle: String
)

data class PowerBridgeProfile(
    val id: String,
    val name: String,
    val config: AppConfig
)

data class ProfileSnapshot(
    val profiles: List<PowerBridgeProfile>,
    val activeProfile: PowerBridgeProfile
)

enum class RelayRouteMode(
    val storageValue: String,
    val displayLabel: String
) {
    AUTO("auto", "Auto"),
    FORCE_HOME_WIFI("force_home_wifi", "Force Home Wi-Fi Relay"),
    FORCE_REMOTE_PUBLIC("force_remote_public", "Force Remote Public Relay");

    companion object {
        fun fromStorage(value: String?): RelayRouteMode {
            return entries.firstOrNull { it.storageValue == value } ?: AUTO
        }
    }
}

data class SelectedRelayRoute(
    val displayName: String,
    val baseUrl: String,
    val reason: String,
    val mode: RelayRouteMode,
    val isManualOverride: Boolean,
    val isHomeWifiMatch: Boolean,
    val mismatchWarning: String? = null
)

object AppConfigStore {
    const val DEFAULT_BASE_URL = "http://192.168.1.10:8787"
    const val DEFAULT_HOME_RELAY_URL = "http://192.168.1.10:8787"
    const val DEFAULT_REMOTE_RELAY_URL = "https://your-relay.example.com"
    const val LEGACY_DEFAULT_URL = "https://your-relay.example.com/activate"
    const val DEFAULT_HOME_WIFI_SUBNET = "192.168.1."
    const val DEFAULT_EXPECTED_GATEWAY = "192.168.1.1"
    const val DEFAULT_TARGET_MAC = "AA:BB:CC:DD:EE:FF"
    const val DEFAULT_BROADCAST_IP = "192.168.1.255"
    const val DEFAULT_TARGET_IP = "192.168.1.100"
    const val DEFAULT_SLEEP_TIMEOUT_SECONDS = 45
    const val DEFAULT_REMOTE_BOOT_TIMEOUT_SECONDS = 180
    const val DEFAULT_SLEEP_PACKETS = 3
    const val DEFAULT_REMOTE_BOOT_PACKETS = 12
    const val DEFAULT_THEME_MODE = "dark"
    const val DEFAULT_THEME_STYLE = "cyberpunk_neon"
    private const val SAMPLE_RELAY_HOST = "your-relay.example.com"

    const val KEY_RELAY_BASE_URL = "relayBaseUrl"
    const val KEY_SELECTED_WAKE_METHOD = "selectedWakeMethod"
    const val KEY_HOME_RELAY_BASE_URL = "homeRelayBaseUrl"
    const val KEY_REMOTE_RELAY_BASE_URL = "remoteRelayBaseUrl"
    const val KEY_HOME_WIFI_SUBNET = "homeWifiSubnet"
    const val KEY_EXPECTED_GATEWAY = "expectedGateway"
    const val KEY_ROUTE_MODE = "routeMode"
    const val KEY_SERVER_URL = "serverUrl"
    const val KEY_TOKEN = "token"
    const val KEY_TARGET_MAC = "targetMac"
    const val KEY_TARGET_IP = "targetIp"
    const val KEY_BROADCAST_IP = "broadcastIp"
    const val KEY_SLEEP_TIMEOUT = "sleepWakeTimeoutSeconds"
    const val KEY_REMOTE_TIMEOUT = "remoteBootTimeoutSeconds"
    const val KEY_SLEEP_PACKETS = "sleepWakePackets"
    const val KEY_REMOTE_PACKETS = "remoteBootPackets"
    const val KEY_THEME_MODE = "themeMode"
    const val KEY_THEME_STYLE = "themeStyle"

    fun forceDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun createPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "powerbridge_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences("powerbridge_prefs", Context.MODE_PRIVATE)
        }
    }

    fun defaultConfig(): AppConfig {
        val normalizedHome = normalizeRelayBaseUrl(DEFAULT_HOME_RELAY_URL)
        val normalizedRemote = normalizeRelayBaseUrl(DEFAULT_REMOTE_RELAY_URL)
        return AppConfig(
            selectedWakeMethod = WakeMethodId.LOCAL_WIFI,
            relayBaseUrl = normalizedHome,
            homeRelayBaseUrl = normalizedHome,
            remoteRelayBaseUrl = normalizedRemote,
            homeWifiSubnet = DEFAULT_HOME_WIFI_SUBNET,
            expectedGateway = DEFAULT_EXPECTED_GATEWAY,
            routeMode = RelayRouteMode.AUTO,
            token = "",
            targetMac = DEFAULT_TARGET_MAC,
            targetIp = DEFAULT_TARGET_IP,
            broadcastIp = DEFAULT_BROADCAST_IP,
            sleepWakeTimeoutSeconds = DEFAULT_SLEEP_TIMEOUT_SECONDS,
            remoteBootTimeoutSeconds = DEFAULT_REMOTE_BOOT_TIMEOUT_SECONDS,
            sleepWakePackets = DEFAULT_SLEEP_PACKETS,
            remoteBootPackets = DEFAULT_REMOTE_BOOT_PACKETS,
            themeMode = DEFAULT_THEME_MODE,
            themeStyle = DEFAULT_THEME_STYLE
        )
    }

    fun load(prefs: SharedPreferences): AppConfig {
        return ProfileStore.loadActiveProfile(prefs).config
    }

    fun save(
        prefs: SharedPreferences,
        selectedWakeMethod: WakeMethodId,
        homeRelayBaseUrl: String,
        remoteRelayBaseUrl: String,
        homeWifiSubnet: String,
        expectedGateway: String,
        routeMode: RelayRouteMode,
        token: String,
        targetMac: String,
        targetIp: String,
        broadcastIp: String,
        sleepWakeTimeoutSeconds: Int,
        remoteBootTimeoutSeconds: Int,
        sleepWakePackets: Int,
        remoteBootPackets: Int
    ) {
        val current = ProfileStore.loadActiveProfile(prefs)
        val updated = current.copy(
            config = buildSanitizedConfig(
                selectedWakeMethod = selectedWakeMethod,
                homeRelayBaseUrl = homeRelayBaseUrl,
                remoteRelayBaseUrl = remoteRelayBaseUrl,
                homeWifiSubnet = homeWifiSubnet,
                expectedGateway = expectedGateway,
                routeMode = routeMode,
                token = token,
                targetMac = targetMac,
                targetIp = targetIp,
                broadcastIp = broadcastIp,
                sleepWakeTimeoutSeconds = sleepWakeTimeoutSeconds,
                remoteBootTimeoutSeconds = remoteBootTimeoutSeconds,
                sleepWakePackets = sleepWakePackets,
                remoteBootPackets = remoteBootPackets,
                themeMode = current.config.themeMode,
                themeStyle = current.config.themeStyle
            )
        )
        ProfileStore.upsertProfile(prefs, updated, setActive = true)
    }

    fun normalizeRelayBaseUrl(url: String): String {
        val trimmed = url.trim().removeSuffix("/")
        if (trimmed.isBlank()) return DEFAULT_BASE_URL

        val suffixes = listOf(
            "/activate",
            "/health",
            "/status",
            "/wake/sleep",
            "/wake/boot",
            "/wake"
        )

        val matched = suffixes.firstOrNull { trimmed.endsWith(it, ignoreCase = true) }
        return if (matched != null) trimmed.dropLast(matched.length) else trimmed
    }

    fun validateBaseUrl(url: String): String? {
        if (url.isBlank()) return "Relay base URL is required."
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return "Relay base URL must start with http:// or https://"
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            return "Invalid relay base URL."
        }
        val lower = url.lowercase()
        if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
            return "Do not use localhost on the phone. Use your relay address."
        }
        return null
    }

    fun legacyActivateUrl(baseUrl: String): String = "${normalizeRelayBaseUrl(baseUrl)}/activate"

    fun healthUrl(baseUrl: String): String = "${normalizeRelayBaseUrl(baseUrl)}/health"

    fun statusUrl(baseUrl: String): String = "${normalizeRelayBaseUrl(baseUrl)}/status"

    fun wakeModeUrl(baseUrl: String, mode: WakeMode): String =
        "${normalizeRelayBaseUrl(baseUrl)}/wake/${mode.pathSegment}"

    fun wakeQueryUrl(baseUrl: String, mode: WakeMode): String =
        "${normalizeRelayBaseUrl(baseUrl)}/wake?mode=${mode.pathSegment}"

    fun isPlaceholderRelayBaseUrl(rawUrl: String): Boolean {
        val normalized = normalizeRelayBaseUrl(rawUrl)
        if (normalized.isBlank()) return true
        return normalized == DEFAULT_HOME_RELAY_URL ||
            normalized == DEFAULT_REMOTE_RELAY_URL ||
            normalized.contains(SAMPLE_RELAY_HOST, ignoreCase = true)
    }

    fun isPlaceholderTargetMac(rawMac: String): Boolean {
        return LocalWakePacketCodec.normalizeMacAddress(rawMac) == DEFAULT_TARGET_MAC
    }

    fun isPlaceholderTargetIp(rawIp: String): Boolean {
        return rawIp.trim() == DEFAULT_TARGET_IP
    }

    fun isPlaceholderBroadcastIp(rawIp: String): Boolean {
        return rawIp.trim() == DEFAULT_BROADCAST_IP
    }

    fun placeholderFields(config: AppConfig): List<String> {
        val placeholders = mutableListOf<String>()
        if (isPlaceholderRelayBaseUrl(config.homeRelayBaseUrl)) placeholders += "Home relay URL"
        if (isPlaceholderRelayBaseUrl(config.remoteRelayBaseUrl)) placeholders += "Remote relay URL"
        if (isPlaceholderTargetMac(config.targetMac)) placeholders += "Target MAC"
        if (isPlaceholderTargetIp(config.targetIp)) placeholders += "Target PC IP"
        if (isPlaceholderBroadcastIp(config.broadcastIp)) placeholders += "Broadcast IP"
        return placeholders
    }

    fun isSetupNeeded(config: AppConfig): Boolean {
        return when (config.selectedWakeMethod) {
            WakeMethodId.LOCAL_WIFI -> isPlaceholderTargetMac(config.targetMac) || isPlaceholderBroadcastIp(config.broadcastIp)
            WakeMethodId.HOME_RELAY -> isPlaceholderRelayBaseUrl(config.homeRelayBaseUrl) ||
                isPlaceholderRelayBaseUrl(config.remoteRelayBaseUrl) ||
                isPlaceholderTargetMac(config.targetMac) ||
                isPlaceholderBroadcastIp(config.broadcastIp)
            else -> true
        }
    }

    fun normalizeHomeSubnetPrefix(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""

        val cidrMatch = Regex("""^(\d{1,3}(?:\.\d{1,3}){3})/(\d{1,2})$""").matchEntire(trimmed)
        if (cidrMatch != null) {
            val ip = cidrMatch.groupValues[1]
            val prefixLength = cidrMatch.groupValues[2].toIntOrNull()
            if (isValidIpCandidate(ip) && prefixLength != null) {
                routePrefixForIp(ip, prefixLength)?.let { return it }
            }
        }

        if (isValidIpCandidate(trimmed)) {
            return trimmed
        }

        val parts = trimmed.trimEnd('.').split('.').filter { it.isNotBlank() }
        if (parts.isEmpty() || parts.size > 3) return trimmed
        if (parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }) {
            return parts.joinToString(".") + "."
        }
        return trimmed
    }

    fun validateHomeSubnetPrefix(raw: String): String? {
        val normalized = normalizeHomeSubnetPrefix(raw)
        if (normalized.isBlank()) return "Home Wi-Fi subnet is required."
        if (normalized.endsWith(".")) {
            val parts = normalized.trimEnd('.').split('.')
            if (parts.isNotEmpty() && parts.size <= 3 && parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }) {
                return null
            }
        }
        if (isValidIpCandidate(normalized)) {
            return null
        }
        return "Home Wi-Fi subnet must be a prefix like 192.168.1. or a full IPv4 address."
    }

    fun isValidIpCandidate(ip: String): Boolean {
        if (ip.isBlank()) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }
    }

    internal fun buildSanitizedConfig(
        selectedWakeMethod: WakeMethodId,
        homeRelayBaseUrl: String,
        remoteRelayBaseUrl: String,
        homeWifiSubnet: String,
        expectedGateway: String,
        routeMode: RelayRouteMode,
        token: String,
        targetMac: String,
        targetIp: String,
        broadcastIp: String,
        sleepWakeTimeoutSeconds: Int,
        remoteBootTimeoutSeconds: Int,
        sleepWakePackets: Int,
        remoteBootPackets: Int,
        themeMode: String,
        themeStyle: String
    ): AppConfig {
        val sanitizedMethod = sanitizeWakeMethod(selectedWakeMethod)
        val normalizedHome = normalizeRelayBaseUrl(homeRelayBaseUrl)
        val normalizedRemote = normalizeRelayBaseUrl(remoteRelayBaseUrl)
        return AppConfig(
            selectedWakeMethod = sanitizedMethod,
            relayBaseUrl = normalizedHome,
            homeRelayBaseUrl = normalizedHome,
            remoteRelayBaseUrl = normalizedRemote,
            homeWifiSubnet = normalizeHomeSubnetPrefix(homeWifiSubnet).ifBlank { DEFAULT_HOME_WIFI_SUBNET },
            expectedGateway = expectedGateway.trim().ifBlank { DEFAULT_EXPECTED_GATEWAY },
            routeMode = routeMode,
            token = token.trim(),
            targetMac = targetMac.trim().ifBlank { DEFAULT_TARGET_MAC }.let(LocalWakePacketCodec::normalizeMacAddress),
            targetIp = targetIp.trim().ifBlank { DEFAULT_TARGET_IP },
            broadcastIp = broadcastIp.trim().ifBlank { DEFAULT_BROADCAST_IP },
            sleepWakeTimeoutSeconds = sleepWakeTimeoutSeconds.coerceIn(15, 300),
            remoteBootTimeoutSeconds = remoteBootTimeoutSeconds.coerceIn(30, 600),
            sleepWakePackets = sleepWakePackets.coerceIn(1, 10),
            remoteBootPackets = remoteBootPackets.coerceIn(1, 24),
            themeMode = themeMode.ifBlank { DEFAULT_THEME_MODE },
            themeStyle = themeStyle.ifBlank { DEFAULT_THEME_STYLE }
        )
    }

    internal fun legacyConfigFromPrefs(prefs: SharedPreferences): AppConfig {
        val storedBase = prefs.getString(KEY_RELAY_BASE_URL, null)
        val legacyUrl = prefs.getString(KEY_SERVER_URL, LEGACY_DEFAULT_URL)
        val normalizedLegacyBase = normalizeRelayBaseUrl(storedBase ?: legacyUrl ?: DEFAULT_BASE_URL)
        val storedHome = prefs.getString(KEY_HOME_RELAY_BASE_URL, null)
        val storedRemote = prefs.getString(KEY_REMOTE_RELAY_BASE_URL, null)
        val homeRelayBaseUrl = normalizeRelayBaseUrl(
            storedHome ?: if (normalizedLegacyBase.startsWith("http://")) normalizedLegacyBase else DEFAULT_HOME_RELAY_URL
        )
        val remoteRelayBaseUrl = normalizeRelayBaseUrl(
            storedRemote ?: if (normalizedLegacyBase.startsWith("https://")) normalizedLegacyBase else DEFAULT_REMOTE_RELAY_URL
        )
        return buildSanitizedConfig(
            selectedWakeMethod = WakeMethodId.fromStorage(
                prefs.getString(KEY_SELECTED_WAKE_METHOD, WakeMethodId.LOCAL_WIFI.storageValue)
            ),
            homeRelayBaseUrl = homeRelayBaseUrl,
            remoteRelayBaseUrl = remoteRelayBaseUrl,
            homeWifiSubnet = prefs.getString(KEY_HOME_WIFI_SUBNET, DEFAULT_HOME_WIFI_SUBNET).orEmpty(),
            expectedGateway = prefs.getString(KEY_EXPECTED_GATEWAY, DEFAULT_EXPECTED_GATEWAY).orEmpty(),
            routeMode = RelayRouteMode.fromStorage(prefs.getString(KEY_ROUTE_MODE, RelayRouteMode.AUTO.storageValue)),
            token = prefs.getString(KEY_TOKEN, "").orEmpty(),
            targetMac = prefs.getString(KEY_TARGET_MAC, DEFAULT_TARGET_MAC).orEmpty(),
            targetIp = prefs.getString(KEY_TARGET_IP, DEFAULT_TARGET_IP).orEmpty(),
            broadcastIp = prefs.getString(KEY_BROADCAST_IP, DEFAULT_BROADCAST_IP).orEmpty(),
            sleepWakeTimeoutSeconds = prefs.getInt(KEY_SLEEP_TIMEOUT, DEFAULT_SLEEP_TIMEOUT_SECONDS),
            remoteBootTimeoutSeconds = prefs.getInt(KEY_REMOTE_TIMEOUT, DEFAULT_REMOTE_BOOT_TIMEOUT_SECONDS),
            sleepWakePackets = prefs.getInt(KEY_SLEEP_PACKETS, DEFAULT_SLEEP_PACKETS),
            remoteBootPackets = prefs.getInt(KEY_REMOTE_PACKETS, DEFAULT_REMOTE_BOOT_PACKETS),
            themeMode = prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE).orEmpty(),
            themeStyle = prefs.getString(KEY_THEME_STYLE, DEFAULT_THEME_STYLE).orEmpty()
        )
    }

    internal fun writeLegacyCompatibility(
        prefs: SharedPreferences,
        config: AppConfig
    ) {
        prefs.edit()
            .putString(KEY_SELECTED_WAKE_METHOD, config.selectedWakeMethod.storageValue)
            .putString(KEY_HOME_RELAY_BASE_URL, config.homeRelayBaseUrl)
            .putString(KEY_REMOTE_RELAY_BASE_URL, config.remoteRelayBaseUrl)
            .putString(KEY_HOME_WIFI_SUBNET, config.homeWifiSubnet)
            .putString(KEY_EXPECTED_GATEWAY, config.expectedGateway)
            .putString(KEY_ROUTE_MODE, config.routeMode.storageValue)
            .putString(KEY_RELAY_BASE_URL, config.homeRelayBaseUrl)
            .putString(KEY_SERVER_URL, legacyActivateUrl(config.remoteRelayBaseUrl))
            .putString(KEY_TOKEN, config.token)
            .putString(KEY_TARGET_MAC, config.targetMac)
            .putString(KEY_TARGET_IP, config.targetIp)
            .putString(KEY_BROADCAST_IP, config.broadcastIp)
            .putInt(KEY_SLEEP_TIMEOUT, config.sleepWakeTimeoutSeconds)
            .putInt(KEY_REMOTE_TIMEOUT, config.remoteBootTimeoutSeconds)
            .putInt(KEY_SLEEP_PACKETS, config.sleepWakePackets)
            .putInt(KEY_REMOTE_PACKETS, config.remoteBootPackets)
            .putString(KEY_THEME_MODE, config.themeMode)
            .putString(KEY_THEME_STYLE, config.themeStyle)
            .apply()
    }

    internal fun sanitizeWakeMethod(method: WakeMethodId): WakeMethodId {
        return if (method.implemented) method else WakeMethodId.LOCAL_WIFI
    }

    private fun routePrefixForIp(ip: String, prefixLength: Int): String? {
        val octets = ip.split(".")
        if (octets.size != 4 || prefixLength !in 0..32) return null
        return when {
            prefixLength >= 24 -> octets.take(3).joinToString(".") + "."
            prefixLength >= 16 -> octets.take(2).joinToString(".") + "."
            prefixLength >= 8 -> octets.take(1).joinToString(".") + "."
            else -> null
        }
    }
}

object ProfileStore {
    private const val KEY_PROFILES_JSON = "powerbridgeProfilesJson"
    private const val KEY_ACTIVE_PROFILE_ID = "powerbridgeActiveProfileId"
    private const val DEFAULT_PROFILE_NAME = "My PC"
    private const val MAX_NAME_LENGTH = 40

    fun loadSnapshot(prefs: SharedPreferences): ProfileSnapshot {
        val profiles = readProfiles(prefs)
        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
        val activeProfile = profiles.firstOrNull { it.id == activeId } ?: profiles.first()
        if (activeId != activeProfile.id) {
            writeProfiles(prefs, profiles, activeProfile.id)
        }
        return ProfileSnapshot(profiles = profiles, activeProfile = activeProfile)
    }

    fun loadProfiles(prefs: SharedPreferences): List<PowerBridgeProfile> = loadSnapshot(prefs).profiles

    fun loadActiveProfile(prefs: SharedPreferences): PowerBridgeProfile = loadSnapshot(prefs).activeProfile

    fun getActiveProfileOrCreateDefault(prefs: SharedPreferences): PowerBridgeProfile = loadSnapshot(prefs).activeProfile

    fun setActiveProfile(prefs: SharedPreferences, profileId: String): PowerBridgeProfile {
        val snapshot = loadSnapshot(prefs)
        val target = snapshot.profiles.firstOrNull { it.id == profileId } ?: snapshot.activeProfile
        writeProfiles(prefs, snapshot.profiles, target.id)
        return target
    }

    fun addProfile(prefs: SharedPreferences, requestedName: String): PowerBridgeProfile {
        val snapshot = loadSnapshot(prefs)
        val name = normalizedUniqueName(requestedName, snapshot.profiles.map { it.name })
        val profile = PowerBridgeProfile(
            id = newProfileId(),
            name = name,
            config = AppConfigStore.defaultConfig()
        )
        writeProfiles(prefs, snapshot.profiles + profile, profile.id)
        return profile
    }

    fun renameProfile(prefs: SharedPreferences, profileId: String, requestedName: String): PowerBridgeProfile {
        val snapshot = loadSnapshot(prefs)
        val current = snapshot.profiles.firstOrNull { it.id == profileId } ?: snapshot.activeProfile
        val otherNames = snapshot.profiles.filterNot { it.id == profileId }.map { it.name }
        val renamed = current.copy(name = normalizedUniqueName(requestedName, otherNames))
        val updatedProfiles = snapshot.profiles.map { if (it.id == profileId) renamed else it }
        writeProfiles(prefs, updatedProfiles, snapshot.activeProfile.id)
        return renamed
    }

    fun deleteProfile(prefs: SharedPreferences, profileId: String): Boolean {
        val snapshot = loadSnapshot(prefs)
        if (snapshot.profiles.size <= 1) return false
        if (snapshot.profiles.none { it.id == profileId }) return false
        val updatedProfiles = snapshot.profiles.filterNot { it.id == profileId }
        val nextActiveId = if (snapshot.activeProfile.id == profileId) {
            updatedProfiles.first().id
        } else {
            snapshot.activeProfile.id
        }
        writeProfiles(prefs, updatedProfiles, nextActiveId)
        return true
    }

    fun upsertProfile(prefs: SharedPreferences, profile: PowerBridgeProfile, setActive: Boolean = false): PowerBridgeProfile {
        val snapshot = loadSnapshot(prefs)
        val sanitized = sanitizeProfile(profile, snapshot.profiles.filterNot { it.id == profile.id }.map { it.name })
        val updatedProfiles = if (snapshot.profiles.any { it.id == sanitized.id }) {
            snapshot.profiles.map { if (it.id == sanitized.id) sanitized else it }
        } else {
            snapshot.profiles + sanitized
        }
        val activeId = when {
            setActive -> sanitized.id
            snapshot.profiles.any { it.id == snapshot.activeProfile.id } -> snapshot.activeProfile.id
            else -> sanitized.id
        }
        writeProfiles(prefs, updatedProfiles, activeId)
        return sanitized
    }

    private fun readProfiles(prefs: SharedPreferences): List<PowerBridgeProfile> {
        val raw = prefs.getString(KEY_PROFILES_JSON, null).orEmpty()
        val parsed = if (raw.isBlank()) emptyList() else runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(profileFromJson(array.getJSONObject(index)))
                }
            }
        }.getOrDefault(emptyList())

        val sanitized = parsed
            .mapIndexed { index, profile ->
                val fallbackName = if (index == 0) DEFAULT_PROFILE_NAME else "PC ${index + 1}"
                sanitizeProfile(profile.copy(name = profile.name.ifBlank { fallbackName }), parsed.filterNot { it.id == profile.id }.map { it.name })
            }
            .distinctBy { it.id }

        if (sanitized.isNotEmpty()) {
            return sanitized
        }

        val migrated = PowerBridgeProfile(
            id = newProfileId(),
            name = DEFAULT_PROFILE_NAME,
            config = AppConfigStore.legacyConfigFromPrefs(prefs)
        )
        writeProfiles(prefs, listOf(migrated), migrated.id)
        return listOf(migrated)
    }

    private fun writeProfiles(
        prefs: SharedPreferences,
        profiles: List<PowerBridgeProfile>,
        activeProfileId: String
    ) {
        val safeProfiles = profiles.ifEmpty {
            listOf(
                PowerBridgeProfile(
                    id = newProfileId(),
                    name = DEFAULT_PROFILE_NAME,
                    config = AppConfigStore.defaultConfig()
                )
            )
        }
        val safeActive = safeProfiles.firstOrNull { it.id == activeProfileId }?.id ?: safeProfiles.first().id
        val json = JSONArray().apply {
            safeProfiles.forEach { put(profileToJson(it)) }
        }.toString()
        prefs.edit()
            .putString(KEY_PROFILES_JSON, json)
            .putString(KEY_ACTIVE_PROFILE_ID, safeActive)
            .apply()
        AppConfigStore.writeLegacyCompatibility(
            prefs = prefs,
            config = safeProfiles.first { it.id == safeActive }.config
        )
    }

    private fun sanitizeProfile(
        profile: PowerBridgeProfile,
        existingNames: List<String>
    ): PowerBridgeProfile {
        val sanitizedId = profile.id.ifBlank { newProfileId() }
        val sanitizedName = normalizedUniqueName(profile.name, existingNames)
        val config = AppConfigStore.buildSanitizedConfig(
            selectedWakeMethod = profile.config.selectedWakeMethod,
            homeRelayBaseUrl = profile.config.homeRelayBaseUrl,
            remoteRelayBaseUrl = profile.config.remoteRelayBaseUrl,
            homeWifiSubnet = profile.config.homeWifiSubnet,
            expectedGateway = profile.config.expectedGateway,
            routeMode = profile.config.routeMode,
            token = profile.config.token,
            targetMac = profile.config.targetMac,
            targetIp = profile.config.targetIp,
            broadcastIp = profile.config.broadcastIp,
            sleepWakeTimeoutSeconds = profile.config.sleepWakeTimeoutSeconds,
            remoteBootTimeoutSeconds = profile.config.remoteBootTimeoutSeconds,
            sleepWakePackets = profile.config.sleepWakePackets,
            remoteBootPackets = profile.config.remoteBootPackets,
            themeMode = profile.config.themeMode,
            themeStyle = profile.config.themeStyle
        )
        return PowerBridgeProfile(id = sanitizedId, name = sanitizedName, config = config)
    }

    private fun normalizedUniqueName(name: String, existingNames: List<String>): String {
        val trimmed = name.trim().ifBlank { DEFAULT_PROFILE_NAME }.take(MAX_NAME_LENGTH)
        if (existingNames.none { it.equals(trimmed, ignoreCase = true) }) {
            return trimmed
        }
        var suffix = 2
        while (true) {
            val candidate = "$trimmed ($suffix)".take(MAX_NAME_LENGTH)
            if (existingNames.none { it.equals(candidate, ignoreCase = true) }) {
                return candidate
            }
            suffix += 1
        }
    }

    private fun profileToJson(profile: PowerBridgeProfile): JSONObject {
        return JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("config", JSONObject().apply {
                put("selectedWakeMethod", profile.config.selectedWakeMethod.storageValue)
                put("relayBaseUrl", profile.config.relayBaseUrl)
                put("homeRelayBaseUrl", profile.config.homeRelayBaseUrl)
                put("remoteRelayBaseUrl", profile.config.remoteRelayBaseUrl)
                put("homeWifiSubnet", profile.config.homeWifiSubnet)
                put("expectedGateway", profile.config.expectedGateway)
                put("routeMode", profile.config.routeMode.storageValue)
                put("token", profile.config.token)
                put("targetMac", profile.config.targetMac)
                put("targetIp", profile.config.targetIp)
                put("broadcastIp", profile.config.broadcastIp)
                put("sleepWakeTimeoutSeconds", profile.config.sleepWakeTimeoutSeconds)
                put("remoteBootTimeoutSeconds", profile.config.remoteBootTimeoutSeconds)
                put("sleepWakePackets", profile.config.sleepWakePackets)
                put("remoteBootPackets", profile.config.remoteBootPackets)
                put("themeMode", profile.config.themeMode)
                put("themeStyle", profile.config.themeStyle)
            })
        }
    }

    private fun profileFromJson(json: JSONObject): PowerBridgeProfile {
        val configJson = json.optJSONObject("config") ?: JSONObject()
        val legacyBase = AppConfigStore.normalizeRelayBaseUrl(
            configJson.optString("relayBaseUrl").ifBlank {
                configJson.optString("homeRelayBaseUrl").ifBlank { AppConfigStore.DEFAULT_BASE_URL }
            }
        )
        val homeBase = configJson.optString("homeRelayBaseUrl").ifBlank { legacyBase }
        val remoteBase = configJson.optString("remoteRelayBaseUrl").ifBlank {
            if (legacyBase.startsWith("https://")) legacyBase else AppConfigStore.DEFAULT_REMOTE_RELAY_URL
        }
        return PowerBridgeProfile(
            id = json.optString("id"),
            name = json.optString("name"),
            config = AppConfigStore.buildSanitizedConfig(
                selectedWakeMethod = WakeMethodId.fromStorage(configJson.optString("selectedWakeMethod")),
                homeRelayBaseUrl = homeBase,
                remoteRelayBaseUrl = remoteBase,
                homeWifiSubnet = configJson.optString("homeWifiSubnet"),
                expectedGateway = configJson.optString("expectedGateway"),
                routeMode = RelayRouteMode.fromStorage(configJson.optString("routeMode")),
                token = configJson.optString("token"),
                targetMac = configJson.optString("targetMac"),
                targetIp = configJson.optString("targetIp"),
                broadcastIp = configJson.optString("broadcastIp"),
                sleepWakeTimeoutSeconds = configJson.optInt("sleepWakeTimeoutSeconds", AppConfigStore.DEFAULT_SLEEP_TIMEOUT_SECONDS),
                remoteBootTimeoutSeconds = configJson.optInt("remoteBootTimeoutSeconds", AppConfigStore.DEFAULT_REMOTE_BOOT_TIMEOUT_SECONDS),
                sleepWakePackets = configJson.optInt("sleepWakePackets", AppConfigStore.DEFAULT_SLEEP_PACKETS),
                remoteBootPackets = configJson.optInt("remoteBootPackets", AppConfigStore.DEFAULT_REMOTE_BOOT_PACKETS),
                themeMode = configJson.optString("themeMode", AppConfigStore.DEFAULT_THEME_MODE),
                themeStyle = configJson.optString("themeStyle", AppConfigStore.DEFAULT_THEME_STYLE)
            )
        )
    }

    private fun newProfileId(): String = "pb-${UUID.randomUUID()}"
}

enum class WakeMode(
    val pathSegment: String,
    val actionLabel: String,
    val sendingStatus: String,
    val successStatus: String,
    val timeoutStatus: String,
    val stageStatuses: List<String>,
    val requiresOnlineVerification: Boolean
) {
    SLEEP(
        pathSegment = "sleep",
        actionLabel = "Wake PC",
        sendingStatus = "Sending wake packet...",
        successStatus = "Desktop ready",
        timeoutStatus = "Wake timed out. Check PC sleep state, Ethernet, and relay connectivity.",
        stageStatuses = listOf(
            "Sending wake packet...",
            "Waiting for PC network...",
            "Waiting for Windows..."
        ),
        requiresOnlineVerification = true
    ),
    BOOT(
        pathSegment = "boot",
        actionLabel = "Boot PC",
        sendingStatus = "Sending remote boot packets...",
        successStatus = "PC online",
        timeoutStatus = "Boot timed out. Check BIOS or UEFI wake settings, Ethernet connectivity, and the target PC's Wake-on-LAN support.",
        stageStatuses = listOf(
            "Sending remote boot packets...",
            "Waiting for motherboard...",
            "Waiting for Windows...",
            "Waiting for Ethernet...",
            "Waiting for PC network..."
        ),
        requiresOnlineVerification = true
    )
}
