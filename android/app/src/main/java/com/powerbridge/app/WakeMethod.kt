package com.powerbridge.app

import android.content.Context
import kotlinx.coroutines.delay
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

enum class WakeDifficulty(val displayLabel: String) {
    EASY("Easy"),
    ADVANCED("Advanced"),
    FUTURE("Planned")
}

enum class WakeConfigField(val displayLabel: String) {
    HOME_RELAY_URL("Home relay URL"),
    REMOTE_RELAY_URL("Remote relay URL"),
    ROUTE_MODE("Route mode"),
    RELAY_TOKEN("Relay token"),
    HOME_WIFI_SUBNET("Home Wi-Fi subnet"),
    EXPECTED_GATEWAY("Expected gateway"),
    TARGET_MAC("Target MAC"),
    TARGET_IP("Target PC IP"),
    BROADCAST_IP("Broadcast IP"),
    SLEEP_PACKETS("Wake packets"),
    REMOTE_PACKETS("Boot packets")
}

data class WakeFieldStatus(
    val field: WakeConfigField,
    val present: Boolean
)

enum class WakeMethodId(
    val storageValue: String,
    val displayName: String,
    val description: String,
    val statusLabel: String,
    val supportsCellular: Boolean,
    val requiresAlwaysOnBridge: Boolean,
    val difficulty: WakeDifficulty,
    val testButtonLabel: String,
    val implemented: Boolean
) {
    LOCAL_WIFI(
        storageValue = "local_wifi",
        displayName = "Local Wi-Fi Wake",
        description = "Wake your PC while your phone is on the same Wi-Fi or LAN. Ethernet is recommended for the most reliable Wake-on-LAN support, and Wi-Fi wake from full shutdown is often hardware-dependent.",
        statusLabel = "Live in this build",
        supportsCellular = false,
        requiresAlwaysOnBridge = false,
        difficulty = WakeDifficulty.EASY,
        testButtonLabel = "Check Local Setup",
        implemented = true
    ),
    HOME_RELAY(
        storageValue = "home_relay",
        displayName = "Home Relay Server",
        description = "Use your own Raspberry Pi, NAS, server, Docker host, router, or personal relay domain. This is the advanced self-hosted method and is available now if you already run your own infrastructure.",
        statusLabel = "Live in this build",
        supportsCellular = true,
        requiresAlwaysOnBridge = true,
        difficulty = WakeDifficulty.ADVANCED,
        testButtonLabel = "Check Relay",
        implemented = true
    ),
    SPARE_ANDROID(
        storageValue = "spare_android",
        displayName = "Old Phone / Tablet Relay",
        description = "Planned for a spare Android phone or tablet left plugged in at home. The relay device would receive a secure request and send the local Wake-on-LAN packet.",
        statusLabel = "Prototype",
        supportsCellular = true,
        requiresAlwaysOnBridge = true,
        difficulty = WakeDifficulty.FUTURE,
        testButtonLabel = "Method Not Available",
        implemented = false
    ),
    MEDIA_CLIENT(
        storageValue = "media_client",
        displayName = "Fire TV / Smart TV Relay",
        description = "Planned for a Fire TV, Chromecast with Google TV, Android TV, or Google TV device left powered at home to act as the wake helper.",
        statusLabel = "Coming later",
        supportsCellular = true,
        requiresAlwaysOnBridge = true,
        difficulty = WakeDifficulty.FUTURE,
        testButtonLabel = "Method Not Available",
        implemented = false
    ),
    SMART_PLUG(
        storageValue = "smart_plug",
        displayName = "Smart Plug Boot Assist",
        description = "Planned hardware workaround that power-cycles a smart plug to force boot when the motherboard is set to Restore on AC Power Loss. This is not standard Wake-on-LAN.",
        statusLabel = "Coming later",
        supportsCellular = true,
        requiresAlwaysOnBridge = false,
        difficulty = WakeDifficulty.FUTURE,
        testButtonLabel = "Method Not Available",
        implemented = false
    ),
    ADVANCED_ROUTER_VPN(
        storageValue = "advanced_router_vpn",
        displayName = "Advanced Network Setup",
        description = "Planned guides for users who already run router Wake-on-LAN, VPN access, or other custom network paths. This is not a live guided setup in the app today.",
        statusLabel = "Guides later",
        supportsCellular = true,
        requiresAlwaysOnBridge = false,
        difficulty = WakeDifficulty.ADVANCED,
        testButtonLabel = "Method Not Available",
        implemented = false
    ),
    SMART_HOME(
        storageValue = "smart_home",
        displayName = "Smart Home Wake",
        description = "Research track for Alexa, Google Home, Home Assistant, and similar smart-home integrations where the platform supports wake flows.",
        statusLabel = "Research",
        supportsCellular = true,
        requiresAlwaysOnBridge = false,
        difficulty = WakeDifficulty.FUTURE,
        testButtonLabel = "Method Not Available",
        implemented = false
    );

    companion object {
        fun fromStorage(value: String?): WakeMethodId {
            return entries.firstOrNull { it.storageValue == value } ?: LOCAL_WIFI
        }
    }
}

interface WakeMethod {
    val id: WakeMethodId
    val requiredFields: List<WakeConfigField>

    fun testConnection(context: Context, config: AppConfig): ActionResult

    suspend fun triggerWake(
        context: Context,
        config: AppConfig,
        mode: WakeMode,
        onProgress: suspend (String) -> Unit
    ): ActionResult

    fun runDiagnostics(context: Context, config: AppConfig): ActionResult
}

object WakeMethodRegistry {
    private val methods: Map<WakeMethodId, WakeMethod> = mapOf(
        WakeMethodId.LOCAL_WIFI to LocalWifiWakeMethod(
            listOf(
                WakeConfigField.TARGET_MAC,
                WakeConfigField.BROADCAST_IP,
                WakeConfigField.SLEEP_PACKETS,
                WakeConfigField.REMOTE_PACKETS
            )
        ),
        WakeMethodId.HOME_RELAY to HomeRelayWakeMethod(),
        WakeMethodId.SPARE_ANDROID to ComingSoonWakeMethod(
            WakeMethodId.SPARE_ANDROID,
            listOf(WakeConfigField.TARGET_MAC)
        ),
        WakeMethodId.MEDIA_CLIENT to ComingSoonWakeMethod(
            WakeMethodId.MEDIA_CLIENT,
            listOf(WakeConfigField.TARGET_MAC)
        ),
        WakeMethodId.SMART_PLUG to ComingSoonWakeMethod(
            WakeMethodId.SMART_PLUG,
            emptyList()
        ),
        WakeMethodId.ADVANCED_ROUTER_VPN to ComingSoonWakeMethod(
            WakeMethodId.ADVANCED_ROUTER_VPN,
            listOf(WakeConfigField.TARGET_MAC)
        ),
        WakeMethodId.SMART_HOME to ComingSoonWakeMethod(
            WakeMethodId.SMART_HOME,
            emptyList()
        )
    )

    fun resolve(config: AppConfig): WakeMethod = methods.getValue(config.selectedWakeMethod)

    fun get(methodId: WakeMethodId): WakeMethod = methods.getValue(methodId)

    fun fieldStatuses(config: AppConfig, methodId: WakeMethodId = config.selectedWakeMethod): List<WakeFieldStatus> {
        return get(methodId).requiredFields.map { field ->
            WakeFieldStatus(field = field, present = isFieldPresent(field, config))
        }
    }

    fun isFieldPresent(field: WakeConfigField, config: AppConfig): Boolean {
        return when (field) {
            WakeConfigField.HOME_RELAY_URL -> AppConfigStore.validateBaseUrl(config.homeRelayBaseUrl) == null
                && !AppConfigStore.isPlaceholderRelayBaseUrl(config.homeRelayBaseUrl)
            WakeConfigField.REMOTE_RELAY_URL -> AppConfigStore.validateBaseUrl(config.remoteRelayBaseUrl) == null
                && !AppConfigStore.isPlaceholderRelayBaseUrl(config.remoteRelayBaseUrl)
            WakeConfigField.ROUTE_MODE -> true
            WakeConfigField.RELAY_TOKEN -> config.token.isNotBlank()
            WakeConfigField.HOME_WIFI_SUBNET -> config.homeWifiSubnet.isNotBlank()
            WakeConfigField.EXPECTED_GATEWAY -> AppConfigStore.isValidIpCandidate(config.expectedGateway)
            WakeConfigField.TARGET_MAC -> LocalWakePacketCodec.isValidMac(config.targetMac) && !AppConfigStore.isPlaceholderTargetMac(config.targetMac)
            WakeConfigField.TARGET_IP -> AppConfigStore.isValidIpCandidate(config.targetIp) && !AppConfigStore.isPlaceholderTargetIp(config.targetIp)
            WakeConfigField.BROADCAST_IP -> AppConfigStore.isValidIpCandidate(config.broadcastIp) && !AppConfigStore.isPlaceholderBroadcastIp(config.broadcastIp)
            WakeConfigField.SLEEP_PACKETS -> config.sleepWakePackets in 1..10
            WakeConfigField.REMOTE_PACKETS -> config.remoteBootPackets in 1..24
        }
    }
}

private class HomeRelayWakeMethod : WakeMethod {
    override val id: WakeMethodId = WakeMethodId.HOME_RELAY
    override val requiredFields: List<WakeConfigField> = listOf(
        WakeConfigField.HOME_RELAY_URL,
        WakeConfigField.REMOTE_RELAY_URL,
        WakeConfigField.ROUTE_MODE,
        WakeConfigField.RELAY_TOKEN,
        WakeConfigField.HOME_WIFI_SUBNET,
        WakeConfigField.EXPECTED_GATEWAY,
        WakeConfigField.TARGET_MAC,
        WakeConfigField.BROADCAST_IP,
        WakeConfigField.SLEEP_PACKETS,
        WakeConfigField.REMOTE_PACKETS
    )

    override fun testConnection(context: Context, config: AppConfig): ActionResult {
        return RelayOps(context, config).testRelay()
    }

    override suspend fun triggerWake(
        context: Context,
        config: AppConfig,
        mode: WakeMode,
        onProgress: suspend (String) -> Unit
    ): ActionResult {
        return RelayOps(context, config).runWake(mode, onProgress)
    }

    override fun runDiagnostics(context: Context, config: AppConfig): ActionResult {
        return RelayOps(context, config).runDiagnostics()
    }
}

private class LocalWifiWakeMethod(
    override val requiredFields: List<WakeConfigField>
) : WakeMethod {
    override val id: WakeMethodId = WakeMethodId.LOCAL_WIFI

    override fun testConnection(context: Context, config: AppConfig): ActionResult {
        DiagnosticsStore.setLastState(context, id.testButtonLabel, "Checking local setup...", "Validating MAC address, broadcast IP, packet count, and phone network.")
        val validation = validateLocalWifiConfig(context, config, id.testButtonLabel, WakeMode.SLEEP)
        if (validation != null) return validation

        val summary = "Local Wi-Fi Wake is configured for direct LAN Wake-on-LAN. This method uses UDP Magic Packets only and does not use relay URLs, tokens, or cellular routing."
        recordMethodNoteEvent(
            context = context,
            action = id.testButtonLabel,
            success = true,
            status = "Local setup ready.",
            detail = "MAC address, broadcast IP, packet count, UDP port 9, and phone network look usable for same-LAN wake."
        )
        DiagnosticsStore.setSummary(context, summary)
        DiagnosticsStore.setLastState(context, id.testButtonLabel, "Local setup ready.", "Ready to send Wake-on-LAN Magic Packets on the local network.")
        return ActionResult(
            "Local setup ready.",
            "Ready to send Wake-on-LAN Magic Packets on the same local network.",
            summary
        )
    }

    override suspend fun triggerWake(
        context: Context,
        config: AppConfig,
        mode: WakeMode,
        onProgress: suspend (String) -> Unit
    ): ActionResult {
        val validation = validateLocalWifiConfig(context, config, mode.actionLabel, mode)
        if (validation != null) return validation

        onProgress("Sending local Magic Packet...")
        val macBytes = LocalWakePacketCodec.parseMacAddress(config.targetMac)
            ?: return ActionResult(
                "Invalid target MAC.",
                "Target MAC must be AA:BB:CC:DD:EE:FF, AA-BB-CC-DD-EE-FF, or AABBCCDDEEFF.",
                "Local Wi-Fi Wake cannot build a Magic Packet because the target MAC address is invalid."
            )
        val packetCount = if (mode == WakeMode.SLEEP) config.sleepWakePackets else config.remoteBootPackets
        val packet = LocalWakePacketCodec.buildMagicPacket(macBytes)
        val broadcastAddress = InetAddress.getByName(config.broadcastIp)
        val started = System.currentTimeMillis()

        return try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                repeat(packetCount) { index ->
                    val datagram = DatagramPacket(packet, packet.size, broadcastAddress, LocalWakePacketCodec.DEFAULT_WOL_PORT)
                    socket.send(datagram)
                    DiagnosticsStore.recordEvent(
                        context,
                        DiagnosticEvent(
                            timestamp = DiagnosticsStore.timestampNow(),
                            action = mode.actionLabel,
                            step = "Send Magic Packet ${index + 1}",
                            method = "UDP",
                            url = "${config.broadcastIp}:${LocalWakePacketCodec.DEFAULT_WOL_PORT}",
                            success = true,
                            durationMs = System.currentTimeMillis() - started,
                            suggestedCause = "Wake-on-LAN Magic Packet sent on the local network.",
                            suggestedFix = "If the PC does not react, verify BIOS/NIC wake support, Ethernet readiness, and the broadcast IP."
                        )
                    )
                    if (index < packetCount - 1) {
                        delay(150)
                    }
                }
            }

            val detail = if (mode == WakeMode.SLEEP) {
                "Wake packet sent on local network."
            } else {
                "Boot request sent as Wake-on-LAN Magic Packet. Power-on from shutdown depends on BIOS/NIC/Fast Startup support."
            }
            val summary = if (mode == WakeMode.SLEEP) {
                "Magic Packet sent over local UDP broadcast. PowerBridge cannot confirm wake completion for Local Wi-Fi Wake."
            } else {
                "Magic Packet sent over local UDP broadcast. Boot from shutdown depends on BIOS, NIC, motherboard firmware, and Windows Fast Startup behavior."
            }
            recordMethodNoteEvent(
                context = context,
                action = mode.actionLabel,
                success = true,
                status = "Magic Packet sent.",
                detail = detail
            )
            DiagnosticsStore.setSummary(context, summary)
            DiagnosticsStore.setLastState(context, mode.actionLabel, "Magic Packet sent.", detail)
            ActionResult("Magic Packet sent.", detail, summary)
        } catch (e: Exception) {
            DiagnosticsStore.recordEvent(
                context,
                DiagnosticEvent(
                    timestamp = DiagnosticsStore.timestampNow(),
                    action = mode.actionLabel,
                    step = "Send Magic Packet",
                    method = "UDP",
                    url = "${config.broadcastIp}:${LocalWakePacketCodec.DEFAULT_WOL_PORT}",
                    success = false,
                    errorType = e::class.java.simpleName,
                    errorMessage = e.message,
                    durationMs = System.currentTimeMillis() - started,
                    suggestedCause = "Local UDP wake packet send failed.",
                    suggestedFix = "Confirm the phone is on the same LAN, the broadcast IP is correct, and Android is allowed to use the active network."
                )
            )
            val summary = "Local Wi-Fi Wake failed before sending a Magic Packet. Check the phone network, MAC address, broadcast IP, and Android network availability."
            DiagnosticsStore.setSummary(context, summary)
            DiagnosticsStore.setLastState(context, mode.actionLabel, "Local wake failed.", "Failed to send local UDP Magic Packet. Open Diagnostics for details.")
            ActionResult("Local wake failed.", "Failed to send local UDP Magic Packet. Open Diagnostics for details.", summary)
        }
    }

    override fun runDiagnostics(context: Context, config: AppConfig): ActionResult {
        val validation = validateLocalWifiConfig(context, config, "Run Diagnostics", WakeMode.SLEEP, diagnosticsOnly = true)
        if (validation != null) return validation

        recordMethodNoteEvent(
            context = context,
            action = "Run Diagnostics",
            success = true,
            status = "Diagnostics ready.",
            detail = "Local Wi-Fi Wake uses UDP port 9 and the same Magic Packet for both Wake and Boot."
        )
        val summary = "Local Wi-Fi Wake diagnostics are ready. This method uses UDP Magic Packets on the same LAN only. Boot from shutdown depends on hardware, firmware, and Windows support."
        DiagnosticsStore.setSummary(context, summary)
        DiagnosticsStore.setLastState(context, "Run Diagnostics", "Diagnostics ready.", "Local Wi-Fi Wake diagnostics complete.")
        return ActionResult(
            "Diagnostics ready.",
            "Local Wi-Fi Wake diagnostics complete. Review required fields and phone network state.",
            summary
        )
    }

    private fun validateLocalWifiConfig(
        context: Context,
        config: AppConfig,
        action: String,
        mode: WakeMode,
        diagnosticsOnly: Boolean = false
    ): ActionResult? {
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Wake Method Selection",
                method = "LOCAL",
                url = id.storageValue,
                success = true,
                suggestedCause = "Local Wi-Fi Wake selected.",
                suggestedFix = "No action needed."
            )
        )

        val network = LocalSetupAssist.detect(context)
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Phone Network Check",
                method = "LOCAL",
                url = network.transportLabel,
                success = network.isLocalLanCapable,
                errorMessage = network.warning,
                suggestedCause = if (network.isLocalLanCapable) {
                    "Phone appears connected to Wi-Fi or another local LAN-capable transport."
                } else {
                    "Local Wi-Fi Wake works only when this phone is connected to the same home network as the target PC."
                },
                suggestedFix = if (network.isLocalLanCapable) {
                    "No action needed."
                } else {
                    "Connect the phone to the same home Wi-Fi or LAN as the target PC."
                }
            )
        )
        if (!network.isLocalLanCapable) {
            val summary = "Local Wi-Fi Wake is limited to the same home network. The phone does not currently appear to be on Wi-Fi or another local LAN-capable connection."
            DiagnosticsStore.setSummary(context, summary)
            val detail = "Local Wi-Fi Wake works only when this phone is connected to the same home network as the target PC."
            DiagnosticsStore.setLastState(context, action, "Local network required.", detail)
            return ActionResult("Local network required.", detail, summary)
        }

        val macBytes = LocalWakePacketCodec.parseMacAddress(config.targetMac)
        val placeholderMac = AppConfigStore.isPlaceholderTargetMac(config.targetMac)
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Target MAC Validation",
                method = "LOCAL",
                url = DiagnosticsStore.maskMacForDisplay(config.targetMac),
                success = macBytes != null && !placeholderMac,
                errorMessage = when {
                    placeholderMac -> "Target MAC is still the sample placeholder."
                    macBytes == null -> "Target MAC is invalid."
                    else -> null
                },
                suggestedCause = when {
                    placeholderMac -> "Target MAC is still the sample placeholder. Enter your PC's MAC address."
                    macBytes == null -> 
                    "Target MAC must be AA:BB:CC:DD:EE:FF, AA-BB-CC-DD-EE-FF, or AABBCCDDEEFF."
                    else ->
                    "Target MAC format is valid for Magic Packet generation."
                },
                suggestedFix = if (placeholderMac || macBytes == null) "Update the target MAC in Settings." else "No action needed."
            )
        )
        if (placeholderMac || macBytes == null) {
            val summary = if (placeholderMac) {
                "Target MAC is still the sample placeholder. Enter your PC's MAC address before using Local Wi-Fi Wake."
            } else {
                "Local Wi-Fi Wake cannot build a Magic Packet because the target MAC address is invalid."
            }
            DiagnosticsStore.setSummary(context, summary)
            DiagnosticsStore.setLastState(
                context,
                action,
                if (placeholderMac) "Setup needed." else "Invalid target MAC.",
                if (placeholderMac) "Target MAC is still the sample placeholder. Enter your PC's MAC address." else "Target MAC must be AA:BB:CC:DD:EE:FF, AA-BB-CC-DD-EE-FF, or AABBCCDDEEFF."
            )
            return ActionResult(
                if (placeholderMac) "Setup needed." else "Invalid target MAC.",
                if (placeholderMac) "Target MAC is still the sample placeholder. Enter your PC's MAC address." else "Target MAC must be AA:BB:CC:DD:EE:FF, AA-BB-CC-DD-EE-FF, or AABBCCDDEEFF.",
                summary
            )
        }

        val placeholderBroadcast = AppConfigStore.isPlaceholderBroadcastIp(config.broadcastIp)
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Broadcast IP Validation",
                method = "LOCAL",
                url = config.broadcastIp,
                success = AppConfigStore.isValidIpCandidate(config.broadcastIp) && !placeholderBroadcast,
                errorMessage = when {
                    placeholderBroadcast -> "Broadcast IP is still the sample placeholder."
                    AppConfigStore.isValidIpCandidate(config.broadcastIp) -> null
                    else -> "Broadcast IP is invalid."
                },
                suggestedCause = when {
                    placeholderBroadcast -> "Broadcast IP is still the sample placeholder. Review or replace it with Auto-detect local network."
                    AppConfigStore.isValidIpCandidate(config.broadcastIp) -> {
                    "Broadcast IP format looks valid."
                    }
                    else -> {
                    "Broadcast IP must be a valid IPv4 address."
                    }
                },
                suggestedFix = if (placeholderBroadcast || !AppConfigStore.isValidIpCandidate(config.broadcastIp)) "Update the broadcast IP in Settings." else "No action needed."
            )
        )
        if (placeholderBroadcast || !AppConfigStore.isValidIpCandidate(config.broadcastIp)) {
            val summary = if (placeholderBroadcast) {
                "Broadcast IP is still the sample placeholder. Run Auto-detect local network or enter the correct broadcast IP before using Local Wi-Fi Wake."
            } else {
                "Local Wi-Fi Wake cannot send a Magic Packet because the broadcast IP is invalid."
            }
            DiagnosticsStore.setSummary(context, summary)
            DiagnosticsStore.setLastState(
                context,
                action,
                if (placeholderBroadcast) "Setup needed." else "Invalid broadcast IP.",
                if (placeholderBroadcast) "Broadcast IP is still the sample placeholder. Run Auto-detect local network or enter the correct broadcast IP." else "Broadcast IP must be a valid IPv4 address."
            )
            return ActionResult(
                if (placeholderBroadcast) "Setup needed." else "Invalid broadcast IP.",
                if (placeholderBroadcast) "Broadcast IP is still the sample placeholder. Run Auto-detect local network or enter the correct broadcast IP." else "Broadcast IP must be a valid IPv4 address.",
                summary
            )
        }

        val packetCount = if (mode == WakeMode.SLEEP) config.sleepWakePackets else config.remoteBootPackets
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Packet Count Validation",
                method = "LOCAL",
                url = packetCount.toString(),
                success = packetCount in 1..24,
                errorMessage = if (packetCount in 1..24) null else "Packet count is outside the supported range.",
                suggestedCause = if (packetCount in 1..24) "Packet count is valid." else "Packet count must stay between 1 and 24.",
                suggestedFix = if (packetCount in 1..24) "No action needed." else "Update the packet count in Settings."
            )
        )
        if (packetCount !in 1..24) {
            val summary = "Local Wi-Fi Wake cannot send a Magic Packet because packet count is invalid."
            DiagnosticsStore.setSummary(context, summary)
            DiagnosticsStore.setLastState(context, action, "Invalid packet count.", "Packet count must be between 1 and 24.")
            return ActionResult("Invalid packet count.", "Packet count must be between 1 and 24.", summary)
        }

        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "UDP Port Validation",
                method = "UDP",
                url = LocalWakePacketCodec.DEFAULT_WOL_PORT.toString(),
                success = true,
                suggestedCause = "Local Wi-Fi Wake uses UDP port 9 in this build.",
                suggestedFix = "No action needed."
            )
        )

        if (diagnosticsOnly) {
            recordMethodNoteEvent(
                context = context,
                action = action,
                success = true,
                status = "Diagnostics ready.",
                detail = "Wake and Boot both use the same UDP Magic Packet on port 9 for Local Wi-Fi Wake."
            )
        }
        return null
    }

    private fun recordMethodNoteEvent(
        context: Context,
        action: String,
        success: Boolean,
        status: String,
        detail: String
    ) {
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Local Wi-Fi Wake Note",
                method = "UDP",
                url = "port:${LocalWakePacketCodec.DEFAULT_WOL_PORT}",
                success = success,
                suggestedCause = "$status $detail",
                suggestedFix = "Wake and Boot both use the same Magic Packet. Boot from shutdown still depends on BIOS, NIC, firmware, and Windows support."
            )
        )
    }
}

private class ComingSoonWakeMethod(
    override val id: WakeMethodId,
    override val requiredFields: List<WakeConfigField>
) : WakeMethod {
    override fun testConnection(context: Context, config: AppConfig): ActionResult {
        return unavailableResult(
            context = context,
            action = id.testButtonLabel,
            detail = "${id.displayName} is planned but not implemented in this build yet."
        )
    }

    override suspend fun triggerWake(
        context: Context,
        config: AppConfig,
        mode: WakeMode,
        onProgress: suspend (String) -> Unit
    ): ActionResult {
        onProgress("Checking ${id.displayName.lowercase()} setup...")
        return unavailableResult(
            context = context,
            action = mode.actionLabel,
            detail = "${id.displayName} is not wired yet. Use Local Wi-Fi Wake or Home Relay Server in this build."
        )
    }

    override fun runDiagnostics(context: Context, config: AppConfig): ActionResult {
        val summary = "Selected wake method ${id.displayName} is not implemented yet. This build currently supports Local Wi-Fi Wake for same-network use and Home Relay Server for user-owned relay setups."
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = "Run Diagnostics",
                step = "Wake Method Availability",
                method = "LOCAL",
                url = id.storageValue,
                success = false,
                errorMessage = "${id.displayName} is not implemented in this build.",
                suggestedCause = summary,
                suggestedFix = "Switch Wake Method to Local Wi-Fi Wake for same-network use, or Home Relay Server if you already run your own relay."
            )
        )
        DiagnosticsStore.setSummary(context, summary)
        DiagnosticsStore.setLastState(
            context,
            "Run Diagnostics",
            "Diagnostics ready.",
            "${id.displayName} is present as a planned module only."
        )
        return ActionResult(
            "Diagnostics ready.",
            "${id.displayName} is not active in this build yet. Use one of the live methods in this build instead.",
            summary
        )
    }

    private fun unavailableResult(context: Context, action: String, detail: String): ActionResult {
        val summary = "${id.displayName} is a staged PowerBridge module and is not implemented in this build. Use Local Wi-Fi Wake for same-LAN wake, or Home Relay Server if you already run your own relay."
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Wake Method Availability",
                method = "LOCAL",
                url = id.storageValue,
                success = false,
                errorMessage = detail,
                suggestedCause = summary,
                suggestedFix = "Use Local Wi-Fi Wake for same-LAN wake, use Home Relay Server for a user-owned relay setup, or wait for a later PowerBridge phase."
            )
        )
        DiagnosticsStore.setSummary(context, summary)
        DiagnosticsStore.setLastState(context, action, "Coming later.", detail)
        return ActionResult("Coming later.", detail, summary)
    }
}

object LocalWakePacketCodec {
    const val DEFAULT_WOL_PORT: Int = 9

    fun parseMacAddress(rawMac: String): ByteArray? {
        val cleaned = rawMac.trim().replace(":", "").replace("-", "").uppercase()
        if (cleaned.length != 12 || !cleaned.all { it in '0'..'9' || it in 'A'..'F' }) {
            return null
        }
        return ByteArray(6) { index ->
            cleaned.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    fun isValidMac(rawMac: String): Boolean = parseMacAddress(rawMac) != null

    fun normalizeMacAddress(rawMac: String): String {
        val bytes = parseMacAddress(rawMac) ?: return rawMac.trim().uppercase()
        return bytes.joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
    }

    fun buildMagicPacket(macBytes: ByteArray): ByteArray {
        val packet = ByteArray(6 + 16 * macBytes.size)
        repeat(6) { packet[it] = 0xFF.toByte() }
        repeat(16) { repeatIndex ->
            System.arraycopy(macBytes, 0, packet, 6 + repeatIndex * macBytes.size, macBytes.size)
        }
        return packet
    }
}
