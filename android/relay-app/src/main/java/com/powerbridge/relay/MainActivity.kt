package com.powerbridge.relay

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private val relayTransport: RelayTransport = LocalPrototypeRelayTransport()

    private lateinit var relayModeText: TextView
    private lateinit var transportText: TextView
    private lateinit var pairingText: TextView
    private lateinit var linkedProfileText: TextView
    private lateinit var lastWakeRequestText: TextView
    private lateinit var statusText: TextView
    private lateinit var statusDetailText: TextView
    private lateinit var updatedText: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        relayModeText = findViewById(R.id.relayModeText)
        transportText = findViewById(R.id.transportText)
        pairingText = findViewById(R.id.pairingText)
        linkedProfileText = findViewById(R.id.linkedProfileText)
        lastWakeRequestText = findViewById(R.id.lastWakeRequestText)
        statusText = findViewById(R.id.statusText)
        statusDetailText = findViewById(R.id.statusDetailText)
        updatedText = findViewById(R.id.updatedText)
        logText = findViewById(R.id.logText)

        findViewById<MaterialButton>(R.id.checkRelayReadinessButton).setOnClickListener {
            val result = relayTransport.checkReadiness(this)
            RelayDiagnosticsStore.refreshPrototypeReport(this, relayTransport, "Check Relay Readiness")
            updateStatus(result.status, result.detail, success = true)
            renderState()
        }

        findViewById<MaterialButton>(R.id.copyDiagnosticsButton).setOnClickListener {
            ensureReport()
            RelayDiagnosticsStore.copyDiagnostics(this)
            updateStatus("Diagnostics copied.", "Relay diagnostics copied to the clipboard.", success = true)
            renderState()
        }

        findViewById<MaterialButton>(R.id.clearLogButton).setOnClickListener {
            RelayDiagnosticsStore.clear(this)
            updateStatus("Log cleared.", "Prototype diagnostics log cleared.", success = true)
            renderState()
        }

        ensureReport()
        renderState()
    }

    private fun ensureReport() {
        if (RelayDiagnosticsStore.getLog(this).isBlank()) {
            RelayDiagnosticsStore.refreshPrototypeReport(this, relayTransport, "App Launch")
        }
    }

    private fun renderState() {
        val currentStatus = relayTransport.currentStatus()
        relayModeText.text = currentStatus.modeLabel
        transportText.text = currentStatus.transportLabel
        pairingText.text = currentStatus.pairingLabel
        linkedProfileText.text = currentStatus.linkedProfileLabel
        lastWakeRequestText.text = currentStatus.lastWakeRequestLabel
        updatedText.text = getString(R.string.relay_last_updated_template, RelayDiagnosticsStore.getLastUpdatedLabel(this))
        logText.text = RelayDiagnosticsStore.getLog(this).ifBlank { getString(R.string.relay_log_empty) }
    }

    private fun updateStatus(status: String, detail: String, success: Boolean) {
        statusText.text = status
        statusDetailText.text = detail
        statusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (success) R.color.pb_green else R.color.pb_blue
            )
        )
    }
}
