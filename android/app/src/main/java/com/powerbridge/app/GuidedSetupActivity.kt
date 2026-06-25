package com.powerbridge.app

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class GuidedSetupActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var importStatusText: TextView
    private lateinit var currentProfileText: TextView
    private lateinit var currentMethodText: TextView
    private lateinit var locationOptionsGroup: RadioGroup
    private lateinit var methodOptionsGroup: RadioGroup
    private lateinit var remoteMethodsCard: MaterialCardView
    private lateinit var recommendationTitleText: TextView
    private lateinit var recommendationDetailText: TextView
    private lateinit var recommendationStatusText: TextView
    private lateinit var primaryActionButton: MaterialButton
    private lateinit var setupStatusCard: MaterialCardView
    private lateinit var finalStatusTitleText: TextView
    private lateinit var finalStatusStateText: TextView
    private lateinit var finalStatusNextStepText: TextView

    private var importedProfileConfirmed: Boolean = false
    private var selectedLocationChoice: WakeLocationChoice = WakeLocationChoice.HOME_WIFI_ONLY
    private var selectedRemoteChoice: RemoteMethodChoice = RemoteMethodChoice.LEGACY_MOBILE

    private val importQrLauncher = registerForActivityResult(ScanContract(), ::handleQrImportResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guided_setup)

        prefs = AppConfigStore.createPrefs(this)
        bindViews()
        configureSystemBars()
        refreshCurrentSetup()
        wireActions()
        initializeState()
    }

    private fun bindViews() {
        importStatusText = findViewById(R.id.importStatusText)
        currentProfileText = findViewById(R.id.currentProfileText)
        currentMethodText = findViewById(R.id.currentMethodText)
        locationOptionsGroup = findViewById(R.id.locationOptionsGroup)
        methodOptionsGroup = findViewById(R.id.methodOptionsGroup)
        remoteMethodsCard = findViewById(R.id.remoteMethodsCard)
        recommendationTitleText = findViewById(R.id.recommendationTitleText)
        recommendationDetailText = findViewById(R.id.recommendationDetailText)
        recommendationStatusText = findViewById(R.id.recommendationStatusText)
        primaryActionButton = findViewById(R.id.primaryActionButton)
        setupStatusCard = findViewById(R.id.setupStatusCard)
        finalStatusTitleText = findViewById(R.id.finalStatusTitleText)
        finalStatusStateText = findViewById(R.id.finalStatusStateText)
        finalStatusNextStepText = findViewById(R.id.finalStatusNextStepText)
    }

    private fun wireActions() {
        findViewById<MaterialButton>(R.id.scanQrButton).setOnClickListener {
            launchQrImport()
        }

        locationOptionsGroup.setOnCheckedChangeListener { _, checkedId ->
            val choice = when (checkedId) {
                R.id.locationAwayOption -> WakeLocationChoice.AWAY_FROM_HOME
                R.id.locationNotSureOption -> WakeLocationChoice.NOT_SURE
                else -> WakeLocationChoice.HOME_WIFI_ONLY
            }
            showLocationChoice(choice)
        }

        methodOptionsGroup.setOnCheckedChangeListener { _, checkedId ->
            val choice = when (checkedId) {
                R.id.mediaClientOption -> RemoteMethodChoice.MEDIA_CLIENT
                R.id.voiceOption -> RemoteMethodChoice.VOICE_ECOSYSTEM
                R.id.hardwareOption -> RemoteMethodChoice.HARDWARE_BYPASS
                R.id.serverOption -> RemoteMethodChoice.PERSISTENT_SERVER
                else -> RemoteMethodChoice.LEGACY_MOBILE
            }
            selectedRemoteChoice = choice
            if (selectedLocationChoice == WakeLocationChoice.AWAY_FROM_HOME) {
                showRemoteChoice(choice)
            }
        }

        primaryActionButton.setOnClickListener {
            chooseCurrentPath()
        }
    }

    private fun initializeState() {
        findViewById<RadioButton>(R.id.locationHomeOption).isChecked = true
        findViewById<RadioButton>(R.id.legacyMobileOption).isChecked = true
        showLocationChoice(WakeLocationChoice.HOME_WIFI_ONLY)
    }

    private fun configureSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    private fun refreshCurrentSetup() {
        val profile = ProfileStore.loadActiveProfile(prefs)
        currentProfileText.text = profile.name
        currentMethodText.text = getString(
            R.string.guided_setup_current_method_template,
            friendlyMethodName(profile.config.selectedWakeMethod),
            if (AppConfigStore.isSetupNeeded(profile.config)) {
                getString(R.string.main_profile_setup_needed)
            } else {
                getString(R.string.main_profile_ready)
            }
        )
    }

    private fun showLocationChoice(choice: WakeLocationChoice) {
        selectedLocationChoice = choice
        setupStatusCard.visibility = View.GONE

        when (choice) {
            WakeLocationChoice.HOME_WIFI_ONLY -> {
                remoteMethodsCard.visibility = View.GONE
                showRecommendation(
                    title = getString(R.string.guided_setup_home_result_title),
                    detail = getString(R.string.guided_setup_home_result_detail),
                    status = buildHomeWifiStatusLabel(),
                    success = !AppConfigStore.isSetupNeeded(ProfileStore.loadActiveProfile(prefs).config),
                    actionLabel = getString(R.string.guided_setup_home_action)
                )
            }

            WakeLocationChoice.AWAY_FROM_HOME -> {
                remoteMethodsCard.visibility = View.VISIBLE
                showRemoteChoice(selectedRemoteChoice)
            }

            WakeLocationChoice.NOT_SURE -> {
                remoteMethodsCard.visibility = View.GONE
                showRecommendation(
                    title = getString(R.string.guided_setup_not_sure_result_title),
                    detail = getString(R.string.guided_setup_not_sure_result_detail),
                    status = getString(R.string.guided_setup_not_sure_result_status),
                    success = false,
                    actionLabel = getString(R.string.guided_setup_not_sure_action)
                )
            }
        }
    }

    private fun showRemoteChoice(choice: RemoteMethodChoice) {
        selectedRemoteChoice = choice
        setupStatusCard.visibility = View.GONE
        showRecommendation(
            title = getString(choice.resultTitleRes),
            detail = getString(choice.resultDetailRes),
            status = getString(choice.resultStatusRes),
            success = choice.isLiveOrAdvancedNow,
            actionLabel = getString(R.string.guided_setup_action_choose_method)
        )
    }

    private fun showRecommendation(
        title: String,
        detail: String,
        status: String,
        success: Boolean,
        actionLabel: String
    ) {
        recommendationTitleText.text = title
        recommendationDetailText.text = detail
        recommendationStatusText.text = status
        recommendationStatusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (success) R.color.cp_success else R.color.cp_warning
            )
        )
        primaryActionButton.text = actionLabel
    }

    private fun chooseCurrentPath() {
        if (!importedProfileConfirmed) {
            importStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            importStatusText.text = getString(R.string.guided_setup_status_scan_first)
            return
        }

        when (selectedLocationChoice) {
            WakeLocationChoice.HOME_WIFI_ONLY -> applyHomeWifiChoice()
            WakeLocationChoice.AWAY_FROM_HOME -> applyRemoteChoice(selectedRemoteChoice)
            WakeLocationChoice.NOT_SURE -> applyNotSureChoice()
        }
    }

    private fun applyHomeWifiChoice() {
        val updated = ProfileStore.setActiveProfileWakeMethod(prefs, WakeMethodId.LOCAL_WIFI)
        refreshCurrentSetup()
        val setupNeeded = AppConfigStore.isSetupNeeded(updated.config)
        showFinalStatus(
            title = getString(R.string.guided_setup_home_final_title),
            state = if (setupNeeded) {
                getString(R.string.guided_setup_label_needs_setup)
            } else {
                getString(R.string.guided_setup_label_ready_now)
            },
            nextStep = if (setupNeeded) {
                getString(R.string.guided_setup_home_final_next_step_needs_setup, updated.name)
            } else {
                getString(R.string.guided_setup_home_final_next_step_ready, updated.name)
            },
            success = !setupNeeded
        )
    }

    private fun applyRemoteChoice(choice: RemoteMethodChoice) {
        when (choice) {
            RemoteMethodChoice.PERSISTENT_SERVER -> {
                val updated = ProfileStore.setActiveProfileWakeMethod(prefs, WakeMethodId.HOME_RELAY)
                refreshCurrentSetup()
                val setupNeeded = AppConfigStore.isSetupNeeded(updated.config)
                showFinalStatus(
                    title = getString(R.string.guided_setup_server_final_title),
                    state = if (setupNeeded) {
                        getString(R.string.guided_setup_label_needs_setup)
                    } else {
                        getString(R.string.guided_setup_label_ready_now)
                    },
                    nextStep = if (setupNeeded) {
                        getString(R.string.guided_setup_server_final_next_step_needs_setup, updated.name)
                    } else {
                        getString(R.string.guided_setup_server_final_next_step_ready, updated.name)
                    },
                    success = !setupNeeded
                )
            }

            RemoteMethodChoice.LEGACY_MOBILE -> {
                showFinalStatus(
                    title = getString(R.string.guided_setup_choice_legacy_mobile_title),
                    state = getString(R.string.guided_setup_label_prototype),
                    nextStep = getString(R.string.guided_setup_legacy_mobile_final_next_step),
                    success = false
                )
            }

            RemoteMethodChoice.MEDIA_CLIENT -> {
                showFinalStatus(
                    title = getString(R.string.guided_setup_choice_media_client_title),
                    state = getString(R.string.guided_setup_label_coming_later),
                    nextStep = getString(R.string.guided_setup_media_client_final_next_step),
                    success = false
                )
            }

            RemoteMethodChoice.VOICE_ECOSYSTEM -> {
                showFinalStatus(
                    title = getString(R.string.guided_setup_choice_voice_title),
                    state = getString(R.string.guided_setup_label_coming_later),
                    nextStep = getString(R.string.guided_setup_voice_final_next_step),
                    success = false
                )
            }

            RemoteMethodChoice.HARDWARE_BYPASS -> {
                showFinalStatus(
                    title = getString(R.string.guided_setup_choice_hardware_title),
                    state = getString(R.string.guided_setup_label_coming_later),
                    nextStep = getString(R.string.guided_setup_hardware_final_next_step),
                    success = false
                )
            }
        }
    }

    private fun applyNotSureChoice() {
        val updated = ProfileStore.setActiveProfileWakeMethod(prefs, WakeMethodId.LOCAL_WIFI)
        refreshCurrentSetup()
        val setupNeeded = AppConfigStore.isSetupNeeded(updated.config)
        showFinalStatus(
            title = getString(R.string.guided_setup_not_sure_final_title),
            state = if (setupNeeded) {
                getString(R.string.guided_setup_label_needs_setup)
            } else {
                getString(R.string.guided_setup_label_ready_now)
            },
            nextStep = if (setupNeeded) {
                getString(R.string.guided_setup_not_sure_final_next_step_needs_setup, updated.name)
            } else {
                getString(R.string.guided_setup_not_sure_final_next_step_ready, updated.name)
            },
            success = !setupNeeded
        )
    }

    private fun showFinalStatus(
        title: String,
        state: String,
        nextStep: String,
        success: Boolean
    ) {
        setupStatusCard.visibility = View.VISIBLE
        finalStatusTitleText.text = title
        finalStatusStateText.text = state
        finalStatusNextStepText.text = nextStep
        finalStatusStateText.setTextColor(
            ContextCompat.getColor(
                this,
                if (success) R.color.cp_success else R.color.cp_warning
            )
        )
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
            importStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            importStatusText.text = getString(R.string.guided_setup_import_cancelled_status)
            return
        }

        when (val parseResult = SetupImportContract.parse(contents)) {
            is SetupImportParseResult.Error -> {
                importStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_error))
                importStatusText.text = parseResult.message.ifBlank {
                    getString(R.string.guided_setup_import_failed_status)
                }
            }

            is SetupImportParseResult.Success -> {
                showImportPreviewDialog(parseResult.preview)
            }
        }
    }

    private fun showImportPreviewDialog(preview: SetupImportPreview) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val summary = TextView(this).apply {
            text = preview.lines.joinToString("\n")
            setTextColor(ContextCompat.getColor(this@GuidedSetupActivity, R.color.cp_text_secondary))
        }
        val nameInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(preview.profileName)
            hint = getString(R.string.guided_setup_import_name_hint)
        }
        container.addView(summary)
        container.addView(nameInput)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.guided_setup_import_preview_title))
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(getString(R.string.guided_setup_import_confirm_button)) { _, _ ->
                val chosenName = nameInput.text?.toString()?.trim().orEmpty().ifBlank { preview.profileName }
                applyImportedProfile(preview.importData, chosenName)
                importedProfileConfirmed = true
                importStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
                importStatusText.text = getString(R.string.guided_setup_scan_complete_status)
                refreshCurrentSetup()
                showLocationChoice(selectedLocationChoice)
            }
            .show()
    }

    private fun applyImportedProfile(importData: PowerBridgeSetupImport, profileName: String): PowerBridgeProfile {
        val currentProfile = ProfileStore.loadActiveProfile(prefs)
        val importedProfile = SetupImportContract.applyToExistingProfile(
            prefs = prefs,
            currentProfile = currentProfile,
            importData = importData
        )
        val renamedProfile = importedProfile.copy(name = profileName)
        return ProfileStore.upsertProfile(prefs, renamedProfile, setActive = true)
    }

    private fun buildHomeWifiStatusLabel(): String {
        val profile = ProfileStore.loadActiveProfile(prefs)
        return if (AppConfigStore.isSetupNeeded(profile.config)) {
            getString(R.string.guided_setup_home_result_status_needs_setup)
        } else {
            getString(R.string.guided_setup_home_result_status_ready)
        }
    }

    private fun friendlyMethodName(methodId: WakeMethodId): String {
        return when (methodId) {
            WakeMethodId.LOCAL_WIFI -> getString(R.string.guided_setup_location_home_title)
            WakeMethodId.HOME_RELAY -> getString(R.string.guided_setup_choice_server_title)
            WakeMethodId.SPARE_ANDROID -> getString(R.string.guided_setup_choice_legacy_mobile_title)
            WakeMethodId.SMART_PLUG -> getString(R.string.guided_setup_choice_hardware_title)
            WakeMethodId.ADVANCED_ROUTER_VPN -> getString(R.string.guided_setup_other_router_vpn)
            WakeMethodId.SMART_HOME -> getString(R.string.guided_setup_choice_voice_title)
        }
    }

    private enum class WakeLocationChoice {
        HOME_WIFI_ONLY,
        AWAY_FROM_HOME,
        NOT_SURE
    }

    private enum class RemoteMethodChoice(
        val resultTitleRes: Int,
        val resultDetailRes: Int,
        val resultStatusRes: Int,
        val isLiveOrAdvancedNow: Boolean
    ) {
        LEGACY_MOBILE(
            R.string.guided_setup_legacy_mobile_result_title,
            R.string.guided_setup_legacy_mobile_result_detail,
            R.string.guided_setup_legacy_mobile_result_status,
            false
        ),
        MEDIA_CLIENT(
            R.string.guided_setup_media_client_result_title,
            R.string.guided_setup_media_client_result_detail,
            R.string.guided_setup_media_client_result_status,
            false
        ),
        VOICE_ECOSYSTEM(
            R.string.guided_setup_voice_result_title,
            R.string.guided_setup_voice_result_detail,
            R.string.guided_setup_voice_result_status,
            false
        ),
        HARDWARE_BYPASS(
            R.string.guided_setup_hardware_result_title,
            R.string.guided_setup_hardware_result_detail,
            R.string.guided_setup_hardware_result_status,
            false
        ),
        PERSISTENT_SERVER(
            R.string.guided_setup_server_result_title,
            R.string.guided_setup_server_result_detail,
            R.string.guided_setup_server_result_status,
            true
        )
    }
}
