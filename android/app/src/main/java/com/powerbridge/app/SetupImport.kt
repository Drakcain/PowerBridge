package com.powerbridge.app

import org.json.JSONObject

data class PowerBridgeSetupImport(
    val schema: String,
    val pcName: String,
    val targetMac: String?,
    val targetIp: String?,
    val gateway: String?,
    val subnetPrefix: Int?,
    val broadcastIp: String?,
    val adapterName: String?,
    val adapterType: String?,
    val createdAt: String?,
    val windowsComputerName: String?,
    val networkName: String?,
    val notes: String?,
    val source: String?,
    val ignoredForbiddenFields: List<String>,
    val missingFields: List<String>
)

data class SetupImportPreview(
    val profileName: String,
    val lines: List<String>,
    val importData: PowerBridgeSetupImport,
    val wifiAdapterWarning: Boolean
)

sealed class SetupImportParseResult {
    data class Success(val preview: SetupImportPreview) : SetupImportParseResult()
    data class Error(val message: String) : SetupImportParseResult()
}

object SetupImportContract {
    const val SCHEMA_V1 = "powerbridge.local_setup.v1"
    private const val DEFAULT_IMPORTED_PROFILE_NAME = "Imported PC"

    private val forbiddenFields = setOf(
        "username",
        "password",
        "adminpassword",
        "relaytoken",
        "token",
        "apikey",
        "secret",
        "credential",
        "privatekey",
        "sshkey",
        "winrmpassword",
        "rdppassword"
    )

    fun parse(rawJson: String): SetupImportParseResult {
        val json = try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            return SetupImportParseResult.Error("Import failed: invalid JSON.")
        }

        val schema = json.optString("schema").trim()
        if (schema != SCHEMA_V1) {
            return SetupImportParseResult.Error("Import failed: unsupported schema. Expected $SCHEMA_V1.")
        }

        val ignoredForbidden = mutableListOf<String>()
        json.keys().forEach { key ->
            if (key.lowercase() in forbiddenFields) {
                ignoredForbidden += key
            }
        }

        val pcName = json.optString("pcName").trim().ifBlank { DEFAULT_IMPORTED_PROFILE_NAME }
        val targetMac = json.optString("targetMac").trim().ifBlank { null }
        val targetIp = json.optString("targetIp").trim().ifBlank { null }
        val gateway = json.optString("gateway").trim().ifBlank { null }
        val subnetPrefix = if (json.has("subnetPrefix") && !json.isNull("subnetPrefix")) json.optInt("subnetPrefix", -1) else null
        val broadcastIp = json.optString("broadcastIp").trim().ifBlank { null }

        if (targetMac != null && !LocalWakePacketCodec.isValidMac(targetMac)) {
            return SetupImportParseResult.Error("Import failed: target MAC is invalid.")
        }
        if (targetIp != null && !AppConfigStore.isValidIpCandidate(targetIp)) {
            return SetupImportParseResult.Error("Import failed: target IP is invalid.")
        }
        if (gateway != null && !AppConfigStore.isValidIpCandidate(gateway)) {
            return SetupImportParseResult.Error("Import failed: gateway is invalid.")
        }
        if (broadcastIp != null && !AppConfigStore.isValidIpCandidate(broadcastIp)) {
            return SetupImportParseResult.Error("Import failed: broadcast IP is invalid.")
        }
        if (subnetPrefix != null && subnetPrefix !in 0..32) {
            return SetupImportParseResult.Error("Import failed: subnet prefix must be between 0 and 32.")
        }

        val missingFields = buildList {
            if (targetMac == null) add("Target MAC")
            if (targetIp == null) add("Target IP")
            if (gateway == null) add("Gateway")
            if (broadcastIp == null) add("Broadcast IP")
        }

        val importData = PowerBridgeSetupImport(
            schema = schema,
            pcName = pcName,
            targetMac = targetMac?.let(LocalWakePacketCodec::normalizeMacAddress),
            targetIp = targetIp,
            gateway = gateway,
            subnetPrefix = subnetPrefix,
            broadcastIp = broadcastIp,
            adapterName = json.optString("adapterName").trim().ifBlank { null },
            adapterType = json.optString("adapterType").trim().ifBlank { null },
            createdAt = json.optString("createdAt").trim().ifBlank { null },
            windowsComputerName = json.optString("windowsComputerName").trim().ifBlank { null },
            networkName = json.optString("networkName").trim().ifBlank { null },
            notes = json.optString("notes").trim().ifBlank { null },
            source = json.optString("source").trim().ifBlank { null },
            ignoredForbiddenFields = ignoredForbidden.distinct(),
            missingFields = missingFields
        )

        return SetupImportParseResult.Success(
            SetupImportPreview(
                profileName = importData.pcName,
                lines = previewLines(importData),
                importData = importData,
                wifiAdapterWarning = importData.adapterType.equals("wifi", ignoreCase = true)
            )
        )
    }

    fun applyToNewProfile(
        prefs: android.content.SharedPreferences,
        importData: PowerBridgeSetupImport
    ): PowerBridgeProfile {
        val created = ProfileStore.addProfile(prefs, importData.pcName)
        val importedConfig = mergeIntoConfig(
            base = created.config.copy(selectedWakeMethod = WakeMethodId.LOCAL_WIFI),
            importData = importData
        )
        return ProfileStore.upsertProfile(
            prefs,
            created.copy(config = importedConfig),
            setActive = true
        )
    }

    fun applyToExistingProfile(
        prefs: android.content.SharedPreferences,
        currentProfile: PowerBridgeProfile,
        importData: PowerBridgeSetupImport
    ): PowerBridgeProfile {
        val importedConfig = mergeIntoConfig(
            base = currentProfile.config,
            importData = importData
        )
        return ProfileStore.upsertProfile(
            prefs,
            currentProfile.copy(config = importedConfig),
            setActive = true
        )
    }

    private fun mergeIntoConfig(
        base: AppConfig,
        importData: PowerBridgeSetupImport
    ): AppConfig {
        val computedSubnet = computeHomeSubnet(importData)
        return AppConfigStore.buildSanitizedConfig(
            selectedWakeMethod = WakeMethodId.LOCAL_WIFI,
            homeRelayBaseUrl = base.homeRelayBaseUrl,
            remoteRelayBaseUrl = base.remoteRelayBaseUrl,
            homeWifiSubnet = computedSubnet ?: base.homeWifiSubnet,
            expectedGateway = importData.gateway ?: base.expectedGateway,
            routeMode = base.routeMode,
            token = base.token,
            targetMac = importData.targetMac ?: base.targetMac,
            targetIp = importData.targetIp ?: base.targetIp,
            broadcastIp = importData.broadcastIp ?: base.broadcastIp,
            sleepWakeTimeoutSeconds = base.sleepWakeTimeoutSeconds,
            remoteBootTimeoutSeconds = base.remoteBootTimeoutSeconds,
            sleepWakePackets = base.sleepWakePackets,
            remoteBootPackets = base.remoteBootPackets,
            themeMode = base.themeMode,
            themeStyle = base.themeStyle
        )
    }

    private fun computeHomeSubnet(importData: PowerBridgeSetupImport): String? {
        val prefix = importData.subnetPrefix ?: return null
        val referenceIp = importData.gateway ?: importData.targetIp ?: return null
        return when {
            prefix >= 24 -> referenceIp.split(".").take(3).joinToString(".") + "."
            prefix >= 16 -> referenceIp.split(".").take(2).joinToString(".") + "."
            prefix >= 8 -> referenceIp.split(".").take(1).joinToString(".") + "."
            else -> null
        }
    }

    private fun previewLines(importData: PowerBridgeSetupImport): List<String> {
        return buildList {
            add("Schema: ${importData.schema}")
            add("Profile name: ${importData.pcName}")
            add("Wake method after import: ${WakeMethodId.LOCAL_WIFI.displayName}")
            add("Source: ${importData.source?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Not provided"}")
            add("Created: ${importData.createdAt ?: "Not provided"}")
            add("Target MAC: ${importData.targetMac?.let(DiagnosticsStore::maskMacForDisplay) ?: "Not provided"}")
            add("Target IP: ${importData.targetIp ?: "Not provided"}")
            add("Gateway: ${importData.gateway ?: "Not provided"}")
            add("Subnet prefix: ${importData.subnetPrefix?.toString() ?: "Not provided"}")
            add("Broadcast IP: ${importData.broadcastIp ?: "Not provided"}")
            val adapterDetails = buildList {
                importData.adapterName?.let(::add)
                importData.adapterType?.let { add(it.toUserFacingAdapterType()) }
            }.joinToString(" / ")
            add("Adapter: ${adapterDetails.ifBlank { "Not provided" }}")
            if (importData.adapterType.equals("wifi", ignoreCase = true)) {
                add("Wi-Fi warning: Wake may depend on laptop, BIOS, driver, and hardware support.")
            }
            if (importData.missingFields.isNotEmpty()) {
                add("Missing fields: ${importData.missingFields.joinToString(", ")}")
            }
            if (importData.ignoredForbiddenFields.isNotEmpty()) {
                add("Ignored forbidden fields: ${importData.ignoredForbiddenFields.joinToString(", ")}")
            }
            add("Relay token import: blocked")
            add("Relay URLs import: unchanged")
            add("Next step: Save the profile, return to the main screen, and run Check Local Setup before wake testing.")
        }
    }

    private fun String.toUserFacingAdapterType(): String {
        return when (lowercase()) {
            "wifi" -> "Wireless"
            "ethernet" -> "Wired"
            else -> replaceFirstChar { it.uppercase() }
        }
    }
}
