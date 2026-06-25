package com.powerbridge.app

import android.content.Context

data class HomeRelayTransportStatus(
    val modeLabel: String,
    val transportLabel: String,
    val pairingLabel: String,
    val linkedProfileLabel: String,
    val lastWakeRequestLabel: String,
    val warning: String
)

data class HomeRelayReadinessReport(
    val status: String,
    val detail: String
)

interface HomeRelayTransport {
    fun currentStatus(): HomeRelayTransportStatus
    fun checkReadiness(context: Context): HomeRelayReadinessReport
}

class LocalPrototypeHomeRelayTransport : HomeRelayTransport {
    override fun currentStatus(): HomeRelayTransportStatus {
        return HomeRelayTransportStatus(
            modeLabel = "Home Relay Prototype",
            transportLabel = "Local-only placeholder",
            pairingLabel = "Not configured",
            linkedProfileLabel = "None",
            lastWakeRequestLabel = "Never",
            warning = "No production relay transport, pairing, cloud, or FCM is implemented yet."
        )
    }

    override fun checkReadiness(context: Context): HomeRelayReadinessReport {
        return HomeRelayReadinessReport(
            status = "Prototype readiness checked.",
            detail = "This device can host the AIO Home Relay prototype screen and local diagnostics. No paired controller, live transport, or wake execution exists yet."
        )
    }
}
