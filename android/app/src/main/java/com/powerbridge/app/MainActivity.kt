package com.powerbridge.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    companion object {
        private const val SUCCESS_ROUTE_KEYWORD = "Route:"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var statusDetailText: TextView
    private lateinit var routeStatusText: TextView
    private lateinit var profileInput: MaterialAutoCompleteTextView
    private lateinit var profileSummaryText: TextView
    private lateinit var wakeButton: MaterialButton
    private lateinit var remoteBootButton: MaterialButton
    private lateinit var testRelayButton: MaterialButton
    private lateinit var diagnosticsButton: MaterialButton
    private lateinit var guidedSetupButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var manageProfilesButton: MaterialButton
    private var profiles: List<PowerBridgeProfile> = emptyList()
    private var isSyncingProfileSelector: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = AppConfigStore.createPrefs(this)
        statusText = findViewById(R.id.statusText)
        statusDetailText = findViewById(R.id.statusDetailText)
        routeStatusText = findViewById(R.id.routeStatusText)
        profileInput = findViewById(R.id.mainProfileInput)
        profileSummaryText = findViewById(R.id.mainProfileSummaryText)
        wakeButton = findViewById(R.id.wakeButton)
        remoteBootButton = findViewById(R.id.remoteBootButton)
        testRelayButton = findViewById(R.id.testRelayButton)
        diagnosticsButton = findViewById(R.id.diagnosticsButton)
        guidedSetupButton = findViewById(R.id.guidedSetupButton)
        settingsButton = findViewById(R.id.settingsButton)
        manageProfilesButton = findViewById(R.id.manageProfilesButton)
        val homeRelayModeButton = findViewById<MaterialButton>(R.id.homeRelayModeButton)

        configureSystemBars()
        syncUiFromConfig()
        applyInitialStatus()

        profileInput.setOnItemClickListener { _, _, position, _ ->
            if (isSyncingProfileSelector) return@setOnItemClickListener
            val selectedProfile = profiles.getOrNull(position) ?: return@setOnItemClickListener
            ProfileStore.setActiveProfile(prefs, selectedProfile.id)
            syncUiFromConfig(selectedProfile.id)
            applyInitialStatus()
            updateStatus(
                getString(R.string.status_initial_title),
                getString(R.string.main_profile_switched_status, selectedProfile.name)
            )
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        manageProfilesButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        diagnosticsButton.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        guidedSetupButton.setOnClickListener {
            startActivity(Intent(this, GuidedSetupActivity::class.java))
        }

        homeRelayModeButton.setOnClickListener {
            startActivity(Intent(this, HomeRelayActivity::class.java))
        }

        testRelayButton.setOnClickListener {
            val profile = ProfileStore.loadActiveProfile(prefs)
            val config = profile.config
            val wakeMethod = WakeMethodRegistry.resolve(config)
            setBusyState(true)
            updateStatus(
                getString(R.string.status_relay_check_title),
                "${getString(R.string.status_relay_check_detail, config.selectedWakeMethod.displayName)} Active profile: ${profile.name}."
            )
            CoroutineScope(Dispatchers.IO).launch {
                val result = wakeMethod.testConnection(this@MainActivity, config)
                withContext(Dispatchers.Main) {
                    applyResult(wakeMethod.id.testButtonLabel, result)
                    setBusyState(false)
                }
            }
        }

        wakeButton.setOnClickListener {
            startWakeFlow(WakeMode.SLEEP)
        }

        remoteBootButton.setOnClickListener {
            startWakeFlow(WakeMode.BOOT)
        }
    }

    override fun onResume() {
        super.onResume()
        syncUiFromConfig()
        applyInitialStatus()
    }

    private fun configureSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    private fun startWakeFlow(mode: WakeMode) {
        val profile = ProfileStore.loadActiveProfile(prefs)
        val config = profile.config
        val wakeMethod = WakeMethodRegistry.resolve(config)
        setBusyState(true)
        updateStatus(mode.sendingStatus, "Preparing ${mode.pathSegment} request for ${config.selectedWakeMethod.displayName} on profile ${profile.name}.")
        CoroutineScope(Dispatchers.IO).launch {
            val result = wakeMethod.triggerWake(this@MainActivity, config, mode) { progress ->
                withContext(Dispatchers.Main) {
                    updateStatus(progress, getString(R.string.status_progress_detail))
                }
            }
            withContext(Dispatchers.Main) {
                applyResult(mode.actionLabel, result)
                setBusyState(false)
            }
        }
    }

    private fun applyResult(action: String, result: ActionResult) {
        DiagnosticsStore.setLastState(this, action, result.status, result.detail)
        updateStatus(result.status, result.detail)
    }

    private fun applyInitialStatus() {
        val profile = ProfileStore.loadActiveProfile(prefs)
        val config = profile.config
        val status = when {
            !config.selectedWakeMethod.implemented -> Pair(
                config.selectedWakeMethod.statusLabel,
                "${config.selectedWakeMethod.displayName} is not active for profile ${profile.name} yet. Open Setup Help to choose the simplest live path."
            )
            config.selectedWakeMethod == WakeMethodId.LOCAL_WIFI && AppConfigStore.isSetupNeeded(config) ->
                Pair("Home Wi-Fi selected", "Profile ${profile.name}: import your PC from Windows Companion or open Settings, tap Auto-detect local network, then enter your PC MAC.")
            config.selectedWakeMethod == WakeMethodId.HOME_RELAY && AppConfigStore.isSetupNeeded(config) ->
                Pair("Setup needed", "Profile ${profile.name}: add your own relay details in Settings, or open Setup Help and switch to Home Wi-Fi.")
            else -> Pair(getString(R.string.status_initial_title), getString(R.string.status_initial_detail))
        }
        updateStatus(status.first, status.second)
    }

    private fun syncUiFromConfig(selectedProfileId: String? = null) {
        val snapshot = ProfileStore.loadSnapshot(prefs)
        profiles = snapshot.profiles
        val activeProfile = snapshot.profiles.firstOrNull { it.id == selectedProfileId } ?: snapshot.activeProfile
        isSyncingProfileSelector = true
        profileInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, profiles.map { it.name }))
        profileInput.setText(activeProfile.name, false)
        isSyncingProfileSelector = false
        val config = activeProfile.config
        val fieldStatuses = WakeMethodRegistry.fieldStatuses(config)
        val readinessLabel = if (AppConfigStore.isSetupNeeded(config)) {
            getString(R.string.main_profile_setup_needed)
        } else {
            getString(R.string.main_profile_ready)
        }
        profileSummaryText.text = getString(
            R.string.main_profile_summary_template,
            friendlyMethodName(config.selectedWakeMethod),
            readinessLabel,
            fieldStatuses.count { it.present },
            fieldStatuses.size
        )
        testRelayButton.text = config.selectedWakeMethod.testButtonLabel
        updateRouteStatus(config)
    }

    private fun setBusyState(isBusy: Boolean) {
        wakeButton.isEnabled = !isBusy
        remoteBootButton.isEnabled = !isBusy
        testRelayButton.isEnabled = !isBusy
        diagnosticsButton.isEnabled = !isBusy
        guidedSetupButton.isEnabled = !isBusy
        settingsButton.isEnabled = !isBusy
    }

    private fun friendlyMethodName(methodId: WakeMethodId): String {
        return when (methodId) {
            WakeMethodId.LOCAL_WIFI -> "Home Wi-Fi"
            WakeMethodId.HOME_RELAY -> getString(R.string.guided_setup_choice_server_title)
            WakeMethodId.SPARE_ANDROID -> getString(R.string.guided_setup_choice_legacy_mobile_title)
            WakeMethodId.SMART_HOME -> getString(R.string.guided_setup_choice_voice_title)
        }
    }

    private fun updateStatus(message: String, detail: String) {
        statusText.text = message
        statusText.setTextColor(statusColor(message))
        statusDetailText.text = detail
        statusDetailText.setTextColor(detailColor(message))
        updateRouteStatus()
    }

    private fun updateRouteStatus(config: AppConfig = AppConfigStore.load(prefs)) {
        val profile = ProfileStore.loadActiveProfile(prefs)
        val method = config.selectedWakeMethod
        val fieldStatus = WakeMethodRegistry.fieldStatuses(config).count { it.present }
        val fieldTotal = WakeMethodRegistry.fieldStatuses(config).size
        val route = if (method == WakeMethodId.HOME_RELAY) RelayRouteResolver.resolve(this, config) else null
        routeStatusText.text = buildString {
            append("Profile: ")
            append(profile.name)
            append('\n')
            append(getString(R.string.route_section_label))
            append(": ")
            append(method.displayName)
            if (route != null) {
                append('\n')
                append(route.displayName)
                append('\n')
                append(
                    if (AppConfigStore.isPlaceholderRelayBaseUrl(route.baseUrl)) {
                        "Sample placeholder relay URL"
                    } else {
                        route.baseUrl
                    }
                )
            } else {
                append('\n')
                append(
                    when {
                        !method.implemented -> "Status: ${method.statusLabel}. Not active in this build."
                        method == WakeMethodId.LOCAL_WIFI -> "Direct local method active. No relay required."
                        else -> "Method module active."
                    }
                )
            }
            if (fieldTotal > 0) {
                append('\n')
                append("Required fields ready: ")
                append(fieldStatus)
                append('/')
                append(fieldTotal)
            }
        }
        routeStatusText.setTextColor(
            if (route != null && !route.mismatchWarning.isNullOrBlank()) {
                ContextCompat.getColor(this, R.color.cp_warning)
            } else {
                ContextCompat.getColor(this, R.color.cp_text_muted)
            }
        )
    }

    private fun statusColor(message: String): Int {
        val lower = message.lowercase()
        return when {
            lower == "ready" -> ContextCompat.getColor(this, R.color.cp_cyan)
            "setup needed" in lower || "selected" in lower -> ContextCompat.getColor(this, R.color.cp_warning)
            "online" in lower || "ready" in lower || "diagnostics ready" in lower || "sent" in lower || "desktop" in lower ->
                ContextCompat.getColor(this, R.color.cp_success)
            "timed out" in lower || "failed" in lower || "offline" in lower || "error" in lower || "issue" in lower ->
                ContextCompat.getColor(this, R.color.cp_error)
            "waiting" in lower || "sending" in lower || "checking" in lower ->
                ContextCompat.getColor(this, R.color.cp_warning)
            else -> ContextCompat.getColor(this, R.color.cp_text_primary)
        }
    }

    private fun detailColor(message: String): Int {
        val lower = message.lowercase()
        return when {
            "offline" in lower || "failed" in lower || "timed out" in lower || "error" in lower ->
                ContextCompat.getColor(this, R.color.cp_text_secondary)
            "online" in lower || "ready" in lower || "sent" in lower || SUCCESS_ROUTE_KEYWORD.lowercase() in lower ->
                ContextCompat.getColor(this, R.color.cp_text_secondary)
            else -> ContextCompat.getColor(this, R.color.cp_text_secondary)
        }
    }
}
