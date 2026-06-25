package com.powerbridge.relay

data class RelayPairingPlaceholder(
    val schema: String = "powerbridge.relay_pairing.v1",
    val pairingRequestId: String = "",
    val controllerDeviceId: String = "",
    val controllerDisplayName: String = "",
    val profileLinkMode: String = "manual_after_pairing"
)

data class RelayRegistrationPlaceholder(
    val schema: String = "powerbridge.relay_registration.v1",
    val relayDeviceId: String = "",
    val relayDisplayName: String = "",
    val relayDeviceType: String = "android_phone",
    val linkedProfileIds: List<String> = emptyList()
)

data class RelayWakeRequestPlaceholder(
    val schema: String = "powerbridge.relay_wake_request.v1",
    val requestId: String = "",
    val relayDeviceId: String = "",
    val profileId: String = "",
    val targetDisplayName: String = "",
    val targetMac: String = ""
)

data class RelayWakeResultPlaceholder(
    val schema: String = "powerbridge.relay_wake_result.v1",
    val requestId: String = "",
    val relayDeviceId: String = "",
    val profileId: String = "",
    val accepted: Boolean = false,
    val sent: Boolean = false,
    val resultCode: String = "not_implemented"
)

data class RelayDiagnosticsPlaceholder(
    val schema: String = "powerbridge.relay_diagnostics.v1",
    val relayDeviceId: String = "",
    val relayDisplayName: String = "",
    val relayDeviceType: String = "android_phone",
    val lastWakeResult: String = "none"
)

data class RelayCapabilitiesPlaceholder(
    val schema: String = "powerbridge.relay_capabilities.v1",
    val relayDeviceId: String = "",
    val canReceivePush: Boolean = false,
    val canPoll: Boolean = false,
    val canRunForegroundService: Boolean = false,
    val canStayAwakeOnPower: Boolean = false,
    val canAccessLocalNetwork: Boolean = true,
    val canSendUdpBroadcast: Boolean = false,
    val supportsTvUi: Boolean = false
)
