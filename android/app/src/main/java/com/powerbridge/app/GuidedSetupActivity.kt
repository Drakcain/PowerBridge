package com.powerbridge.app

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class GuidedSetupActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var importStatusText: TextView
    private lateinit var currentProfileText: TextView
    private lateinit var currentMethodText: TextView
    private lateinit var recommendationTitleText: TextView
    private lateinit var recommendationDetailText: TextView
    private lateinit var recommendationStatusText: TextView
    private lateinit var primaryActionButton: MaterialButton
    private lateinit var methodOptionsGroup: RadioGroup
    private var importedProfileConfirmed: Boolean = false
    private var selectedChoice: GuidedMethodChoice = GuidedMethodChoice.LEGACY_MOBILE
    private val importQrLauncher = registerForActivityResult(ScanContract(), ::handleQrImportResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guided_setup)

        prefs = AppConfigStore.createPrefs(this)
        importStatusText = findViewById(R.id.importStatusText)
        currentProfileText = findViewById(R.id.currentProfileText)
        currentMethodText = findViewById(R.id.currentMethodText)
        recommendationTitleText = findViewById(R.id.recommendationTitleText)
        recommendationDetailText = findViewById(R.id.recommendationDetailText)
        recommendationStatusText = findViewById(R.id.recommendationStatusText)
        primaryActionButton = findViewById(R.id.primaryActionButton)
        methodOptionsGroup = findViewById(R.id.methodOptionsGroup)

        configureSystemBars()
        refreshCurrentSetup()
        showChoice(GuidedMethodChoice.LEGACY_MOBILE)

        findViewById<MaterialButton>(R.id.scanQrButton).setOnClickListener {
            launchQrImport()
        }

        methodOptionsGroup.setOnCheckedChangeListener { _, checkedId ->
            val choice = when (checkedId) {
                R.id.legacyMobileOption -> GuidedMethodChoice.LEGACY_MOBILE
                R.id.mediaClientOption -> GuidedMethodChoice.MEDIA_CLIENT
                R.id.voiceOption -> GuidedMethodChoice.VOICE_ECOSYSTEM
                R.id.hardwareOption -> GuidedMethodChoice.HARDWARE_BYPASS
                R.id.serverOption -> GuidedMethodChoice.PERSISTENT_SERVER
                else -> GuidedMethodChoice.LEGACY_MOBILE
            }
            showChoice(choice)
        }

        primaryActionButton.setOnClickListener {
            chooseMethod()
        }
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

    private fun showChoice(choice: GuidedMethodChoice) {
        selectedChoice = choice
        recommendationTitleText.text = getString(choice.resultTitleRes)
        recommendationDetailText.text = getString(choice.resultDetailRes)
        recommendationStatusText.text = getString(choice.resultStatusRes)
        recommendationStatusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (choice.liveNow) R.color.cp_success else R.color.cp_warning
            )
        )
    }

    private fun chooseMethod() {
        if (!importedProfileConfirmed) {
            importStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
            importStatusText.text = getString(R.string.guided_setup_status_scan_first)
            return
        }

        when (selectedChoice) {
            GuidedMethodChoice.PERSISTENT_SERVER -> {
                val updated = ProfileStore.setActiveProfileWakeMethod(prefs, WakeMethodId.HOME_RELAY)
                refreshCurrentSetup()
                recommendationStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
                recommendationStatusText.text = getString(
                    R.string.guided_setup_status_method_selected,
                    updated.name,
                    getString(R.string.guided_setup_choice_server_title)
                )
            }

            GuidedMethodChoice.LEGACY_MOBILE,
            GuidedMethodChoice.MEDIA_CLIENT,
            GuidedMethodChoice.VOICE_ECOSYSTEM,
            GuidedMethodChoice.HARDWARE_BYPASS -> {
                recommendationStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_warning))
                recommendationStatusText.text = getString(R.string.guided_setup_done_status)
            }
        }
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
                val updatedProfile = applyImportedProfile(preview.importData, chosenName)
                importedProfileConfirmed = true
                importStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
                importStatusText.text = getString(R.string.guided_setup_scan_complete_status)
                currentProfileText.text = updatedProfile.name
                currentMethodText.text = getString(
                    R.string.guided_setup_current_method_template,
                    getString(R.string.guided_setup_scan_ready_title),
                    getString(R.string.guided_setup_scan_ready_detail)
                )
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

    private fun friendlyMethodName(methodId: WakeMethodId): String {
        return when (methodId) {
            WakeMethodId.LOCAL_WIFI -> getString(R.string.guided_setup_scan_ready_title)
            WakeMethodId.HOME_RELAY -> getString(R.string.guided_setup_choice_server_title)
            WakeMethodId.SPARE_ANDROID -> getString(R.string.guided_setup_choice_legacy_mobile_title)
            WakeMethodId.SMART_PLUG -> getString(R.string.guided_setup_choice_hardware_title)
            WakeMethodId.ADVANCED_ROUTER_VPN -> getString(R.string.guided_setup_other_router_vpn)
            WakeMethodId.SMART_HOME -> getString(R.string.guided_setup_choice_voice_title)
        }
    }

    private enum class GuidedMethodChoice(
        val resultTitleRes: Int,
        val resultDetailRes: Int,
        val resultStatusRes: Int,
        val liveNow: Boolean
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
