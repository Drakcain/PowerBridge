package com.powerbridge.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton

class HomeRelayActivity : AppCompatActivity() {
    private val relayTransport: HomeRelayTransport = LocalPrototypeHomeRelayTransport()

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
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_relay)

        relayModeText = findViewById(R.id.relayModeText)
        transportText = findViewById(R.id.transportText)
        pairingText = findViewById(R.id.pairingText)
        linkedProfileText = findViewById(R.id.linkedProfileText)
        lastWakeRequestText = findViewById(R.id.lastWakeRequestText)
        statusText = findViewById(R.id.statusText)
        statusDetailText = findViewById(R.id.statusDetailText)
        updatedText = findViewById(R.id.updatedText)
        logText = findViewById(R.id.logText)

        configureSystemBars()

        findViewById<MaterialButton>(R.id.checkRelayReadinessButton).setOnClickListener {
            val result = relayTransport.checkReadiness(this)
            HomeRelayDiagnosticsStore.refreshPrototypeReport(this, relayTransport, "Check Relay Readiness")
            updateStatus(result.status, result.detail, success = true)
            renderState()
        }

        findViewById<MaterialButton>(R.id.copyDiagnosticsButton).setOnClickListener {
            ensureReport()
            HomeRelayDiagnosticsStore.copyDiagnostics(this)
            updateStatus(getString(R.string.home_relay_report_copied_title), getString(R.string.home_relay_report_copied_detail), success = true)
            renderState()
        }

        findViewById<MaterialButton>(R.id.shareReportButton).setOnClickListener {
            ensureReport()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.home_relay_share_subject))
                putExtra(Intent.EXTRA_TEXT, HomeRelayDiagnosticsStore.getLog(this@HomeRelayActivity))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.home_relay_share_chooser)))
            updateStatus(getString(R.string.home_relay_report_shared_title), getString(R.string.home_relay_report_shared_detail), success = true)
            renderState()
        }

        findViewById<MaterialButton>(R.id.clearLogButton).setOnClickListener {
            HomeRelayDiagnosticsStore.clear(this)
            updateStatus(getString(R.string.home_relay_report_cleared_title), getString(R.string.home_relay_report_cleared_detail), success = true)
            renderState()
        }

        ensureReport()
        renderState()
    }

    private fun configureSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    private fun ensureReport() {
        if (HomeRelayDiagnosticsStore.getLog(this).isBlank()) {
            HomeRelayDiagnosticsStore.refreshPrototypeReport(this, relayTransport, "App Launch")
        }
    }

    private fun renderState() {
        val currentStatus = relayTransport.currentStatus()
        relayModeText.text = currentStatus.modeLabel
        transportText.text = currentStatus.transportLabel
        pairingText.text = currentStatus.pairingLabel
        linkedProfileText.text = currentStatus.linkedProfileLabel
        lastWakeRequestText.text = currentStatus.lastWakeRequestLabel
        updatedText.text = getString(R.string.home_relay_last_updated_template, HomeRelayDiagnosticsStore.getLastUpdatedLabel(this))
        logText.text = HomeRelayDiagnosticsStore.getLog(this).ifBlank { getString(R.string.home_relay_log_empty) }
        if (statusText.text.isNullOrBlank()) {
            updateStatus(
                getString(R.string.home_relay_status_initial_title),
                getString(R.string.home_relay_status_initial_detail),
                success = true
            )
        }
    }

    private fun updateStatus(status: String, detail: String, success: Boolean) {
        statusText.text = status
        statusDetailText.text = detail
        statusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (success) R.color.cp_success else R.color.cp_warning
            )
        )
    }
}
