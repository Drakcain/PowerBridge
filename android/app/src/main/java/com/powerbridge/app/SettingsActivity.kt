package com.powerbridge.app

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var activeProfile: PowerBridgeProfile
    private var profiles: List<PowerBridgeProfile> = emptyList()
    private lateinit var activeMethod: WakeMethodId
    private lateinit var settingsStatusText: TextView
    private var isLoadingProfileForm: Boolean = false
    private var reloadProfileUi: ((PowerBridgeProfile) -> Unit)? = null
    private val importQrLauncher = registerForActivityResult(ScanContract(), ::handleQrImportResult)

    private fun requireActiveProfile(): PowerBridgeProfile {
        if (!::activeProfile.isInitialized) {
            activeProfile = ProfileStore.getActiveProfileOrCreateDefault(prefs)
        }
        return activeProfile
    }

    private fun updateActiveProfileWakeMethod(wakeMethod: WakeMethodId): PowerBridgeProfile {
        val currentProfile = requireActiveProfile()
        val sanitizedMethod = if (wakeMethod.implemented) wakeMethod else WakeMethodId.LOCAL_WIFI
        val updatedProfile = ProfileStore.upsertProfile(
            prefs = prefs,
            profile = currentProfile.copy(
                config = currentProfile.config.copy(selectedWakeMethod = sanitizedMethod)
            ),
            setActive = true
        )
        activeProfile = updatedProfile
        activeMethod = updatedProfile.config.selectedWakeMethod
        return updatedProfile
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = AppConfigStore.createPrefs(this)
        configureSystemBars()

        val profileInput = findViewById<MaterialAutoCompleteTextView>(R.id.profileInput)
        val profileSummaryText = findViewById<TextView>(R.id.profileSummaryText)
        val addProfileButton = findViewById<MaterialButton>(R.id.addProfileButton)
        val renameProfileButton = findViewById<MaterialButton>(R.id.renameProfileButton)
        val deleteProfileButton = findViewById<MaterialButton>(R.id.deleteProfileButton)
        val importProfileButton = findViewById<MaterialButton>(R.id.importProfileButton)
        val wakeMethodInput = findViewById<MaterialAutoCompleteTextView>(R.id.wakeMethodInput)
        val wakeMethodDescription = findViewById<TextView>(R.id.wakeMethodDescriptionText)
        val relayRoutingCard = findViewById<View>(R.id.relayRoutingCard)
        val targetCard = findViewById<View>(R.id.targetSettingsCard)
        val wakeCard = findViewById<View>(R.id.wakeSettingsCard)
        val bootCard = findViewById<View>(R.id.bootSettingsCard)
        val helpText = findViewById<TextView>(R.id.settingsHelpBodyText)
        val routeModeInput = findViewById<MaterialAutoCompleteTextView>(R.id.routeModeInput)
        val homeRelayBaseUrlInput = findViewById<TextInputEditText>(R.id.homeRelayBaseUrlInput)
        val remoteRelayBaseUrlInput = findViewById<TextInputEditText>(R.id.remoteRelayBaseUrlInput)
        val homeWifiSubnetInput = findViewById<TextInputEditText>(R.id.homeWifiSubnetInput)
        val expectedGatewayInput = findViewById<TextInputEditText>(R.id.expectedGatewayInput)
        val tokenInput = findViewById<TextInputEditText>(R.id.tokenInput)
        val targetMacInput = findViewById<TextInputEditText>(R.id.targetMacInput)
        val targetIpInput = findViewById<TextInputEditText>(R.id.targetIpInput)
        val broadcastIpInput = findViewById<TextInputEditText>(R.id.broadcastIpInput)
        val sleepTimeoutInput = findViewById<TextInputEditText>(R.id.sleepTimeoutInput)
        val remoteTimeoutInput = findViewById<TextInputEditText>(R.id.remoteTimeoutInput)
        val sleepPacketsInput = findViewById<TextInputEditText>(R.id.sleepPacketsInput)
        val remotePacketsInput = findViewById<TextInputEditText>(R.id.remotePacketsInput)
        val autoDetectButton = findViewById<MaterialButton>(R.id.autoDetectLocalNetworkButton)
        val saveButton = findViewById<MaterialButton>(R.id.saveSettingsButton)
        val clearTokenButton = findViewById<MaterialButton>(R.id.clearTokenButton)
        val statusText = findViewById<TextView>(R.id.settingsStatusText)
        settingsStatusText = statusText

        val methodLabels = WakeMethodId.entries.map { it.displayName }
        wakeMethodInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, methodLabels))
        val routeLabels = RelayRouteMode.entries.map { it.displayLabel }
        routeModeInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, routeLabels))
        attachMacAddressFormatter(targetMacInput)

        fun refreshProfilesUi(selectedProfileId: String) {
            val snapshot = ProfileStore.loadSnapshot(prefs)
            profiles = snapshot.profiles
            activeProfile = snapshot.profiles.firstOrNull { it.id == selectedProfileId } ?: snapshot.activeProfile
            profileInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, profiles.map { it.name }))
            profileInput.setText(activeProfile.name, false)
            profileSummaryText.text = buildProfileSummary(activeProfile, profiles.size)
            deleteProfileButton.isEnabled = profiles.size > 1
        }

        fun loadProfileIntoForm(profile: PowerBridgeProfile) {
            isLoadingProfileForm = true
            activeProfile = profile
            val config = profile.config
            activeMethod = config.selectedWakeMethod
            profileInput.setText(profile.name, false)
            profileSummaryText.text = buildProfileSummary(profile, profiles.size)
            wakeMethodInput.setText(config.selectedWakeMethod.displayName, false)
            routeModeInput.setText(config.routeMode.displayLabel, false)
            homeRelayBaseUrlInput.setText(config.homeRelayBaseUrl)
            remoteRelayBaseUrlInput.setText(config.remoteRelayBaseUrl)
            homeWifiSubnetInput.setText(config.homeWifiSubnet)
            expectedGatewayInput.setText(config.expectedGateway)
            tokenInput.setText(config.token)
            targetMacInput.setText(config.targetMac)
            targetIpInput.setText(config.targetIp)
            broadcastIpInput.setText(config.broadcastIp)
            sleepTimeoutInput.setText(config.sleepWakeTimeoutSeconds.toString())
            remoteTimeoutInput.setText(config.remoteBootTimeoutSeconds.toString())
            sleepPacketsInput.setText(config.sleepWakePackets.toString())
            remotePacketsInput.setText(config.remoteBootPackets.toString())
            applyMethodUi(
                method = config.selectedWakeMethod,
                descriptionView = wakeMethodDescription,
                relayRoutingCard = relayRoutingCard,
                targetCard = targetCard,
                wakeCard = wakeCard,
                bootCard = bootCard,
                helpText = helpText,
                autoDetectButton = autoDetectButton
            )
            isLoadingProfileForm = false
        }
        reloadProfileUi = ::loadProfileIntoForm

        val initialProfile = ProfileStore.getActiveProfileOrCreateDefault(prefs)
        activeProfile = initialProfile
        refreshProfilesUi(initialProfile.id)
        loadProfileIntoForm(requireActiveProfile())

        profileInput.setOnItemClickListener { _, _, position, _ ->
            val selectedProfile = profiles.getOrNull(position) ?: return@setOnItemClickListener
            ProfileStore.setActiveProfile(prefs, selectedProfile.id)
            refreshProfilesUi(selectedProfile.id)
            loadProfileIntoForm(requireActiveProfile())
            statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            statusText.text = getString(R.string.settings_profile_switched_status, requireActiveProfile().name)
        }

        addProfileButton.setOnClickListener {
            showProfileNameDialog(
                title = getString(R.string.settings_profile_add_title),
                initialValue = "",
                positiveLabel = getString(R.string.settings_profile_add_button)
            ) { requestedName ->
                val created = ProfileStore.addProfile(prefs, requestedName)
                refreshProfilesUi(created.id)
                loadProfileIntoForm(requireActiveProfile())
                statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
                statusText.text = getString(R.string.settings_profile_added_status, created.name)
            }
        }

        renameProfileButton.setOnClickListener {
            showProfileNameDialog(
                title = getString(R.string.settings_profile_rename_title),
                initialValue = requireActiveProfile().name,
                positiveLabel = getString(R.string.settings_profile_rename_button)
            ) { requestedName ->
                val renamed = ProfileStore.renameProfile(prefs, requireActiveProfile().id, requestedName)
                refreshProfilesUi(renamed.id)
                loadProfileIntoForm(requireActiveProfile())
                statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
                statusText.text = getString(R.string.settings_profile_renamed_status, renamed.name)
            }
        }

        deleteProfileButton.setOnClickListener {
            if (profiles.size <= 1) {
                statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
                statusText.text = getString(R.string.settings_profile_delete_blocked_status)
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_profile_delete_title, requireActiveProfile().name))
                .setMessage(getString(R.string.settings_profile_delete_message, requireActiveProfile().name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(getString(R.string.settings_profile_delete_button)) { _, _ ->
                    val deletedName = requireActiveProfile().name
                    if (ProfileStore.deleteProfile(prefs, requireActiveProfile().id)) {
                        val nextProfile = ProfileStore.getActiveProfileOrCreateDefault(prefs)
                        refreshProfilesUi(nextProfile.id)
                        loadProfileIntoForm(nextProfile)
                        statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
                        statusText.text = getString(R.string.settings_profile_deleted_status, deletedName, requireActiveProfile().name)
                    }
                }
                .show()
        }

        importProfileButton.setOnClickListener {
            launchQrImport()
        }

        wakeMethodInput.setOnItemClickListener { _, _, _, _ ->
            if (isLoadingProfileForm) return@setOnItemClickListener
            val selectedMethod = WakeMethodId.entries.firstOrNull {
                it.displayName == wakeMethodInput.text?.toString()?.trim().orEmpty()
            } ?: WakeMethodId.LOCAL_WIFI
            if (!selectedMethod.implemented) {
                AlertDialog.Builder(this)
                    .setTitle(selectedMethod.statusLabel)
                    .setMessage("${selectedMethod.displayName} is visible so you can see the planned PowerBridge wake paths, but it is not active in this build.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                wakeMethodInput.setText(activeMethod.displayName, false)
                statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
                statusText.text = "${selectedMethod.displayName} is ${selectedMethod.statusLabel.lowercase()} and was not selected."
                return@setOnItemClickListener
            }
            val updatedProfile = updateActiveProfileWakeMethod(selectedMethod)
            refreshProfilesUi(updatedProfile.id)
            applyMethodUi(
                method = updatedProfile.config.selectedWakeMethod,
                descriptionView = wakeMethodDescription,
                relayRoutingCard = relayRoutingCard,
                targetCard = targetCard,
                wakeCard = wakeCard,
                bootCard = bootCard,
                helpText = helpText,
                autoDetectButton = autoDetectButton
            )
            profileSummaryText.text = buildProfileSummary(updatedProfile, profiles.size)
        }

        autoDetectButton.setOnClickListener {
            val selectedMethod = WakeMethodId.entries.firstOrNull {
                it.displayName == wakeMethodInput.text?.toString()?.trim().orEmpty()
            } ?: WakeMethodId.LOCAL_WIFI
            runLocalSetupAssist(
                selectedMethod = selectedMethod,
                homeWifiSubnetInput = homeWifiSubnetInput,
                expectedGatewayInput = expectedGatewayInput,
                broadcastIpInput = broadcastIpInput,
                targetMacInput = targetMacInput,
                statusText = statusText
            )
        }

        saveButton.setOnClickListener {
            val wakeMethod = WakeMethodId.entries.firstOrNull {
                it.displayName == wakeMethodInput.text?.toString()?.trim().orEmpty()
            } ?: WakeMethodId.LOCAL_WIFI
            if (!wakeMethod.implemented) {
                statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
                statusText.text = "${wakeMethod.displayName} is ${wakeMethod.statusLabel.lowercase()} and cannot be saved as the active method."
                wakeMethodInput.setText(activeMethod.displayName, false)
                return@setOnClickListener
            }
            val routeMode = RelayRouteMode.entries.firstOrNull { it.displayLabel == routeModeInput.text?.toString()?.trim().orEmpty() }
                ?: RelayRouteMode.AUTO
            val homeRelayBaseUrl = homeRelayBaseUrlInput.text?.toString()?.trim().orEmpty()
            val remoteRelayBaseUrl = remoteRelayBaseUrlInput.text?.toString()?.trim().orEmpty()
            val homeWifiSubnet = homeWifiSubnetInput.text?.toString()?.trim().orEmpty()
            val expectedGateway = expectedGatewayInput.text?.toString()?.trim().orEmpty()
            val token = tokenInput.text?.toString()?.trim().orEmpty()
            val targetMac = targetMacInput.text?.toString()?.trim().orEmpty()
            val targetIp = targetIpInput.text?.toString()?.trim().orEmpty()
            val broadcastIp = broadcastIpInput.text?.toString()?.trim().orEmpty()
            val sleepTimeout = sleepTimeoutInput.text?.toString()?.trim().orEmpty()
            val remoteTimeout = remoteTimeoutInput.text?.toString()?.trim().orEmpty()
            val sleepPackets = sleepPacketsInput.text?.toString()?.trim().orEmpty()
            val remotePackets = remotePacketsInput.text?.toString()?.trim().orEmpty()

            val validation = validateInputs(
                wakeMethod = wakeMethod,
                homeRelayBaseUrl = homeRelayBaseUrl,
                remoteRelayBaseUrl = remoteRelayBaseUrl,
                homeWifiSubnet = homeWifiSubnet,
                expectedGateway = expectedGateway,
                targetMac = targetMac,
                targetIp = targetIp,
                broadcastIp = broadcastIp,
                sleepTimeout = sleepTimeout,
                remoteTimeout = remoteTimeout,
                sleepPackets = sleepPackets,
                remotePackets = remotePackets
            )

            if (validation != null) {
                statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_error))
                statusText.text = validation
                return@setOnClickListener
            }

            AppConfigStore.save(
                prefs = prefs,
                selectedWakeMethod = wakeMethod,
                homeRelayBaseUrl = homeRelayBaseUrl,
                remoteRelayBaseUrl = remoteRelayBaseUrl,
                homeWifiSubnet = homeWifiSubnet,
                expectedGateway = expectedGateway,
                routeMode = routeMode,
                token = token,
                targetMac = targetMac,
                targetIp = targetIp,
                broadcastIp = broadcastIp,
                sleepWakeTimeoutSeconds = sleepTimeout.toInt(),
                remoteBootTimeoutSeconds = remoteTimeout.toInt(),
                sleepWakePackets = sleepPackets.toInt(),
                remoteBootPackets = remotePackets.toInt()
            )
            val savedProfile = ProfileStore.getActiveProfileOrCreateDefault(prefs)
            refreshProfilesUi(savedProfile.id)
            loadProfileIntoForm(savedProfile)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
            statusText.text = getString(R.string.settings_saved_profile_status, savedProfile.name)
            activeMethod = wakeMethod
            setResult(Activity.RESULT_OK)
            finish()
        }

        clearTokenButton.setOnClickListener {
            val currentProfile = requireActiveProfile()
            val config = currentProfile.config
            ProfileStore.upsertProfile(
                prefs,
                currentProfile.copy(config = config.copy(token = "")),
                setActive = true
            )
            val clearedProfile = ProfileStore.getActiveProfileOrCreateDefault(prefs)
            refreshProfilesUi(clearedProfile.id)
            loadProfileIntoForm(clearedProfile)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            statusText.text = getString(R.string.settings_token_cleared_profile_status, clearedProfile.name)
        }
    }

    private fun applyMethodUi(
        method: WakeMethodId,
        descriptionView: TextView,
        relayRoutingCard: View,
        targetCard: View,
        wakeCard: View,
        bootCard: View,
        helpText: TextView,
        autoDetectButton: MaterialButton
    ) {
        val methodSummary = listOf(
            method.description,
            getString(R.string.settings_method_status_template, if (method.implemented) getString(R.string.settings_method_status_live) else method.statusLabel),
            if (method.supportsCellular) {
                getString(R.string.settings_method_cellular_yes)
            } else {
                getString(R.string.settings_method_cellular_no)
            },
            if (method.requiresAlwaysOnBridge) {
                getString(R.string.settings_method_bridge_yes)
            } else {
                getString(R.string.settings_method_bridge_no)
            },
            if (method.implemented) {
                getString(R.string.settings_method_difficulty, method.difficulty.displayLabel)
            } else {
                getString(R.string.settings_method_availability_template, method.statusLabel)
            }
        ).joinToString("\n")
        descriptionView.text = methodSummary
        relayRoutingCard.visibility = if (method == WakeMethodId.HOME_RELAY) View.VISIBLE else View.GONE
        val showWakeTargetCards = method == WakeMethodId.HOME_RELAY || method == WakeMethodId.LOCAL_WIFI
        targetCard.visibility = if (showWakeTargetCards) View.VISIBLE else View.GONE
        wakeCard.visibility = if (showWakeTargetCards) View.VISIBLE else View.GONE
        bootCard.visibility = if (showWakeTargetCards) View.VISIBLE else View.GONE
        autoDetectButton.visibility = if (method == WakeMethodId.LOCAL_WIFI) View.VISIBLE else View.GONE
        helpText.text = when (method) {
            WakeMethodId.HOME_RELAY -> getString(R.string.settings_help_home_relay)
            WakeMethodId.LOCAL_WIFI -> getString(R.string.settings_help_local_wifi)
            WakeMethodId.SPARE_ANDROID -> getString(R.string.settings_help_home_device_relay)
            WakeMethodId.SMART_PLUG -> getString(R.string.settings_help_smart_plug)
            WakeMethodId.ADVANCED_ROUTER_VPN -> getString(R.string.settings_help_advanced_network)
            WakeMethodId.SMART_HOME -> getString(R.string.settings_help_smart_home)
        }
    }

    private fun configureSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    private fun validateInputs(
        wakeMethod: WakeMethodId,
        homeRelayBaseUrl: String,
        remoteRelayBaseUrl: String,
        homeWifiSubnet: String,
        expectedGateway: String,
        targetMac: String,
        targetIp: String,
        broadcastIp: String,
        sleepTimeout: String,
        remoteTimeout: String,
        sleepPackets: String,
        remotePackets: String
    ): String? {
        if (wakeMethod == WakeMethodId.HOME_RELAY) {
            AppConfigStore.validateBaseUrl(AppConfigStore.normalizeRelayBaseUrl(homeRelayBaseUrl))?.let { return "Home relay: $it" }
            AppConfigStore.validateBaseUrl(AppConfigStore.normalizeRelayBaseUrl(remoteRelayBaseUrl))?.let { return "Remote relay: $it" }
            AppConfigStore.validateHomeSubnetPrefix(homeWifiSubnet)?.let { return it }
            if (expectedGateway.isBlank()) return "Expected gateway is required."
            if (!AppConfigStore.isValidIpCandidate(expectedGateway)) {
                return "Expected gateway must be a valid IPv4 address."
            }
        }
        if (!LocalWakePacketCodec.isValidMac(targetMac)) {
            return "Target MAC must look like AA:BB:CC:DD:EE:FF, AA-BB-CC-DD-EE-FF, or AABBCCDDEEFF."
        }
        if (!AppConfigStore.isValidIpCandidate(broadcastIp)) {
            return "Broadcast IP must be a valid IPv4 address."
        }
        if (targetIp.isNotBlank() && !AppConfigStore.isValidIpCandidate(targetIp)) {
            return "Target PC IP must be blank or a valid IPv4 address."
        }

        val sleepTimeoutValue = sleepTimeout.toIntOrNull() ?: return "Sleep timeout must be a number."
        val remoteTimeoutValue = remoteTimeout.toIntOrNull() ?: return "Remote boot timeout must be a number."
        val sleepPacketsValue = sleepPackets.toIntOrNull() ?: return "Sleep packets must be a number."
        val remotePacketsValue = remotePackets.toIntOrNull() ?: return "Remote boot packets must be a number."

        if (sleepTimeoutValue !in 15..300) return "Sleep timeout must be between 15 and 300 seconds."
        if (remoteTimeoutValue !in 30..600) return "Remote boot timeout must be between 30 and 600 seconds."
        if (sleepPacketsValue !in 1..10) return "Sleep packets must be between 1 and 10."
        if (remotePacketsValue !in 1..24) return "Remote boot packets must be between 1 and 24."
        return null
    }

    private fun runLocalSetupAssist(
        selectedMethod: WakeMethodId,
        homeWifiSubnetInput: TextInputEditText,
        expectedGatewayInput: TextInputEditText,
        broadcastIpInput: TextInputEditText,
        targetMacInput: TextInputEditText,
        statusText: TextView
    ) {
        if (selectedMethod != WakeMethodId.LOCAL_WIFI) {
            statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            statusText.text = getString(R.string.local_setup_switch_method_first)
            return
        }

        val suggestion = LocalSetupAssist.detect(this)
        DiagnosticsStore.recordEvent(
            this,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = "Auto-detect local network",
                step = "Network Detection",
                method = "LOCAL",
                url = suggestion.transportLabel,
                success = suggestion.isLocalLanCapable,
                errorMessage = suggestion.warning,
                suggestedCause = buildString {
                    append("Profile: ")
                    append(requireActiveProfile().name)
                    append(", Wi-Fi detected: ")
                    append(if (suggestion.isWifi) "yes" else "no")
                    append(", phone IPv4: ")
                    append(suggestion.phoneIpv4 ?: "unavailable")
                    append(", prefix: ")
                    append(suggestion.prefixLength?.toString() ?: "unavailable")
                    append(", broadcast: ")
                    append(suggestion.broadcastIp ?: "unavailable")
                    append(", gateway: ")
                    append(suggestion.gatewayIp ?: "unavailable")
                    append(", private LAN: ")
                    append(if (suggestion.isPrivateLan) "yes" else "no")
                },
                suggestedFix = suggestion.warning ?: "Review the detected values and apply them only to the active profile if they match your home network."
            )
        )

        if (!suggestion.isLocalLanCapable) {
            DiagnosticsStore.setSummary(this, "Local Setup Assist could not confirm a local Wi-Fi or LAN connection. Connect the phone to the same home network as the target PC and try again.")
            statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            statusText.text = getString(R.string.local_setup_wifi_warning)
            return
        }

        val preview = buildString {
            appendLine(getString(R.string.local_setup_detected_title))
            appendLine()
            appendLine(getString(R.string.settings_profile_detected_for_label, requireActiveProfile().name))
            appendLine(getString(R.string.local_setup_phone_ip_label, suggestion.phoneIpv4 ?: getString(R.string.local_setup_unknown_value)))
            appendLine(getString(R.string.local_setup_network_label, suggestion.subnetDisplay ?: getString(R.string.local_setup_unknown_value)))
            appendLine(getString(R.string.local_setup_broadcast_label, suggestion.broadcastIp ?: getString(R.string.local_setup_unknown_value)))
            appendLine(getString(R.string.local_setup_gateway_label, suggestion.gatewayIp ?: getString(R.string.local_setup_unknown_value)))
            val currentSubnet = homeWifiSubnetInput.text?.toString()?.trim().orEmpty()
            if (currentSubnet.isNotBlank() && suggestion.suggestedHomeSubnetPrefix != null && currentSubnet != suggestion.suggestedHomeSubnetPrefix) {
                appendLine()
                appendLine("Your saved wake settings are for $currentSubnet, but this phone is currently on ${suggestion.suggestedHomeSubnetPrefix}.")
            }
            appendLine()
            appendLine(getString(R.string.local_setup_suggestion_note))
            appendLine()
            if (targetMacInput.text?.toString()?.trim().isNullOrBlank()) {
                append(getString(R.string.local_setup_target_mac_required))
            } else {
                append(getString(R.string.local_setup_ready_for_check))
            }
            suggestion.warning?.let {
                appendLine()
                appendLine()
                append(it)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.local_setup_detected_dialog_title))
            .setMessage(preview)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(getString(R.string.local_setup_use_detected_settings)) { _, _ ->
                suggestion.broadcastIp?.let { broadcastIpInput.setText(it) }
                suggestion.suggestedHomeSubnetPrefix?.let { homeWifiSubnetInput.setText(it) }
                suggestion.gatewayIp?.let { expectedGatewayInput.setText(it) }
                DiagnosticsStore.recordEvent(
                    this,
                    DiagnosticEvent(
                        timestamp = DiagnosticsStore.timestampNow(),
                        action = "Auto-detect local network",
                        step = "Apply Detected Settings",
                        method = "LOCAL",
                        url = suggestion.broadcastIp ?: "No broadcast detected",
                        success = true,
                        suggestedCause = "Detected local network values were applied to the active profile fields for ${requireActiveProfile().name}.",
                        suggestedFix = "Review the values, add or verify the target MAC, then press Save."
                    )
                )
                DiagnosticsStore.setSummary(this, "Local Setup Assist filled the available LAN fields for ${requireActiveProfile().name}. Review the values, then save them before testing Local Wi-Fi Wake.")
                statusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
                statusText.text = getString(R.string.local_setup_applied_status)
            }
            .show()
    }

    private fun buildProfileSummary(profile: PowerBridgeProfile, profileCount: Int): String {
        val required = WakeMethodRegistry.fieldStatuses(profile.config)
        val readyCount = required.count { it.present }
        val placeholderCount = AppConfigStore.placeholderFields(profile.config).size
        return getString(
            R.string.settings_profile_summary_template,
            profile.name,
            profile.config.selectedWakeMethod.displayName,
            readyCount,
            required.size,
            placeholderCount,
            profileCount
        )
    }

    private fun showProfileNameDialog(
        title: String,
        initialValue: String,
        positiveLabel: String,
        onConfirm: (String) -> Unit
    ) {
        val input = android.widget.EditText(this).apply {
            setText(initialValue)
            setSelection(text?.length ?: 0)
            hint = getString(R.string.settings_profile_name_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(positiveLabel) { _, _ ->
                onConfirm(input.text?.toString().orEmpty())
            }
            .show()
    }

    private fun launchQrImport() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.settings_profile_scan_prompt))
            setBeepEnabled(false)
            setOrientationLocked(false)
        }
        importQrLauncher.launch(options)
    }

    private fun handleQrImportResult(result: ScanIntentResult) {
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            settingsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            settingsStatusText.text = getString(R.string.settings_profile_scan_cancelled_status)
            return
        }

        when (val parseResult = SetupImportContract.parse(contents)) {
            is SetupImportParseResult.Error -> {
                recordImportEvent(
                    success = false,
                    step = "QR Import Parse",
                    detail = "QR scan failed: ${parseResult.message}",
                    ignoredForbiddenFields = emptyList(),
                    profileName = requireActiveProfile().name
                )
                settingsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_error))
                settingsStatusText.text = parseResult.message
            }
            is SetupImportParseResult.Success -> {
                showImportPreviewDialog(parseResult.preview)
            }
        }
    }

    private fun showImportPreviewDialog(preview: SetupImportPreview) {
        val message = preview.lines.joinToString("\n")
        recordImportEvent(
            success = true,
            step = "QR Import Preview Ready",
            detail = "Scanned setup for ${preview.profileName}.",
            ignoredForbiddenFields = preview.importData.ignoredForbiddenFields,
            profileName = preview.profileName
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_profile_import_preview_title))
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                recordImportEvent(
                    success = true,
                    step = "QR Import Cancelled",
                    detail = "User cancelled the QR setup preview.",
                    ignoredForbiddenFields = preview.importData.ignoredForbiddenFields,
                    profileName = preview.profileName
                )
                settingsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
                settingsStatusText.text = getString(R.string.settings_profile_import_cancelled_status)
            }
            .setNeutralButton(getString(R.string.settings_profile_import_update_button)) { _, _ ->
                val updatedProfile = SetupImportContract.applyToExistingProfile(
                    prefs = prefs,
                    currentProfile = requireActiveProfile(),
                    importData = preview.importData
                )
                finishImport(preview, updatedProfile, created = false)
            }
            .setPositiveButton(getString(R.string.settings_profile_import_create_button)) { _, _ ->
                val createdProfile = SetupImportContract.applyToNewProfile(
                    prefs = prefs,
                    importData = preview.importData
                )
                finishImport(preview, createdProfile, created = true)
            }
            .show()
    }

    private fun finishImport(
        preview: SetupImportPreview,
        importedProfile: PowerBridgeProfile,
        created: Boolean
    ) {
        val step = if (created) "QR Import Created Profile" else "QR Import Updated Profile"
        val detail = if (created) {
            "Created ${importedProfile.name} from ${preview.importData.source ?: preview.importData.schema}."
        } else {
            "Updated ${importedProfile.name} from ${preview.importData.source ?: preview.importData.schema}."
        }
        recordImportEvent(
            success = true,
            step = step,
            detail = detail,
            ignoredForbiddenFields = preview.importData.ignoredForbiddenFields,
            profileName = importedProfile.name
        )
        val wifiNote = if (preview.wifiAdapterWarning) {
            " Imported adapter is Wi-Fi. Wake reliability may depend on laptop, BIOS, driver, and hardware support."
        } else {
            ""
        }
        DiagnosticsStore.setSummary(
            this,
            "Profile ${importedProfile.name} was ${if (created) "created" else "updated"} from Windows Companion. ${WakeMethodId.LOCAL_WIFI.displayName} is now selected for this profile. Return to the main screen and run Check Local Setup before wake testing.$wifiNote"
        )
        profiles = ProfileStore.loadProfiles(prefs)
        activeProfile = importedProfile
        reloadProfileUi?.invoke(importedProfile)
        settingsStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
        settingsStatusText.text = if (created) {
            getString(R.string.settings_profile_import_created_status, importedProfile.name)
        } else {
            getString(R.string.settings_profile_import_updated_status, importedProfile.name)
        }
    }

    private fun recordImportEvent(
        success: Boolean,
        step: String,
        detail: String,
        ignoredForbiddenFields: List<String>,
        profileName: String
    ) {
        DiagnosticsStore.recordEvent(
            this,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = "Import Setup",
                step = step,
                method = "LOCAL",
                url = profileName,
                success = success,
                errorMessage = if (success) null else detail,
                suggestedCause = buildString {
                    append(detail)
                    if (ignoredForbiddenFields.isNotEmpty()) {
                        append(" Ignored forbidden fields: ")
                        append(ignoredForbiddenFields.joinToString(", "))
                    }
                },
                suggestedFix = if (success) {
                    "Return to the main screen, confirm Active PC, run Check Local Setup, then test wake."
                } else {
                    "Correct the JSON payload, use schema powerbridge.local_setup.v1, and avoid forbidden secret fields."
                }
            )
        )
    }

    private fun attachMacAddressFormatter(input: TextInputEditText) {
        var isFormatting = false
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                val raw = s?.toString().orEmpty()
                val hex = raw.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                    .uppercase()
                    .take(12)
                val formatted = buildString {
                    hex.chunked(2).forEachIndexed { index, chunk ->
                        if (index > 0) append(':')
                        append(chunk)
                    }
                }
                if (formatted == raw) return
                isFormatting = true
                input.setText(formatted)
                input.setSelection(formatted.length)
                isFormatting = false
            }
        })
    }
}
