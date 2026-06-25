package com.powerbridge.relay

import android.content.Context

data class RelayTransportStatus(
    val modeLabel: String,
    val transportLabel: String,
    val pairingLabel: String,
    val linkedProfileLabel: String,
    val lastWakeRequestLabel: String,
    val warning: String
)

data class RelayReadinessReport(
    val status: String,
    val detail: String
)

interface RelayTransport {
    fun currentStatus(): RelayTransportStatus
    fun checkReadiness(context: Context): RelayReadinessReport
}

class LocalPrototypeRelayTransport : RelayTransport {
    override fun currentStatus(): RelayTransportStatus {
        return RelayTransportStatus(
            modeLabel = "Prototype",
            transportLabel = "Local-only placeholder",
            pairingLabel = "Not configured",
            linkedProfileLabel = "None",
            lastWakeRequestLabel = "Never",
            warning = "No production relay transport, pairing, cloud, or FCM is implemented yet."
        )
    }

    override fun checkReadiness(context: Context): RelayReadinessReport {
        return RelayReadinessReport(
            status = "Prototype readiness checked.",
            detail = "Relay module launches, diagnostics are local-only, and the transport remains a safe placeholder. No paired controller or wake transport exists yet."
        )
    }
}
