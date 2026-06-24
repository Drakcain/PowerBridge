package com.powerbridge.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiagnosticsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var summaryText: TextView
    private lateinit var latestText: TextView
    private lateinit var logText: TextView
    private lateinit var diagnosticsStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        prefs = AppConfigStore.createPrefs(this)
        configureSystemBars()

        summaryText = findViewById(R.id.diagnosticsSummaryText)
        latestText = findViewById(R.id.latestDiagnosticText)
        logText = findViewById(R.id.diagnosticLogText)
        diagnosticsStatusText = findViewById(R.id.diagnosticsStatusText)
        val runButton = findViewById<MaterialButton>(R.id.runDiagnosticsButton)
        val copyButton = findViewById<MaterialButton>(R.id.copyDiagnosticsButton)
        val shareButton = findViewById<MaterialButton>(R.id.shareDiagnosticsButton)
        val clearButton = findViewById<MaterialButton>(R.id.clearDiagnosticsButton)

        runButton.setOnClickListener {
            val config = ProfileStore.loadActiveProfile(prefs).config
            val wakeMethod = WakeMethodRegistry.resolve(config)
            setButtonsEnabled(false, runButton, copyButton, shareButton, clearButton)
            diagnosticsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            diagnosticsStatusText.text = getString(R.string.diagnostics_running_status)
            CoroutineScope(Dispatchers.IO).launch {
                val result = wakeMethod.runDiagnostics(this@DiagnosticsActivity, config)
                withContext(Dispatchers.Main) {
                    diagnosticsStatusText.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.cp_success))
                    diagnosticsStatusText.text = result.detail
                    refreshUi()
                    setButtonsEnabled(true, runButton, copyButton, shareButton, clearButton)
                }
            }
        }

        copyButton.setOnClickListener {
            DiagnosticsStore.copyDiagnostics(this, ProfileStore.loadActiveProfile(prefs))
            diagnosticsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
            diagnosticsStatusText.text = getString(R.string.diagnostics_copied_status)
        }

        shareButton.setOnClickListener {
            val reportFile = DiagnosticsStore.writeDiagnosticsReportZip(this, ProfileStore.loadActiveProfile(prefs))
            val reportUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", reportFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, reportUri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_share_subject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.diagnostics_share_chooser)))
            diagnosticsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
            diagnosticsStatusText.text = getString(R.string.diagnostics_shared_status)
        }

        clearButton.setOnClickListener {
            DiagnosticsStore.clear(this)
            diagnosticsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            diagnosticsStatusText.text = getString(R.string.diagnostics_cleared_status)
            refreshUi()
        }

        refreshUi()
        if (diagnosticsStatusText.text.isNullOrBlank()) {
            diagnosticsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_text_secondary))
            diagnosticsStatusText.text = getString(R.string.diagnostics_default_status)
        }
    }

    private fun configureSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    private fun refreshUi() {
        val profile = ProfileStore.loadActiveProfile(prefs)
        val events = DiagnosticsStore.getEvents(this)
        summaryText.text = DiagnosticsStore.getSummary(this)
        latestText.text = getString(
            R.string.diagnostics_latest_template,
            DiagnosticsStore.getLastAction(this),
            DiagnosticsStore.getLastStatus(this),
            DiagnosticsStore.getLastDetail(this)
        )
        logText.text = if (events.isEmpty()) {
            getString(R.string.diagnostics_empty_log)
        } else {
            DiagnosticsStore.buildReport(this, profile)
        }
    }

    private fun setButtonsEnabled(enabled: Boolean, vararg buttons: MaterialButton) {
        buttons.forEach { it.isEnabled = enabled }
    }
}
