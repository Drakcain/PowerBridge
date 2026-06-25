package com.powerbridge.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton

class GuidedSetupActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var currentProfileText: TextView
    private lateinit var currentMethodText: TextView
    private lateinit var recommendationTitleText: TextView
    private lateinit var recommendationDetailText: TextView
    private lateinit var recommendationStatusText: TextView
    private lateinit var primaryActionButton: MaterialButton
    private lateinit var secondaryActionButton: MaterialButton
    private lateinit var currentRecommendation: GuidedRecommendation

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guided_setup)

        prefs = AppConfigStore.createPrefs(this)
        currentProfileText = findViewById(R.id.currentProfileText)
        currentMethodText = findViewById(R.id.currentMethodText)
        recommendationTitleText = findViewById(R.id.recommendationTitleText)
        recommendationDetailText = findViewById(R.id.recommendationDetailText)
        recommendationStatusText = findViewById(R.id.recommendationStatusText)
        primaryActionButton = findViewById(R.id.primaryActionButton)
        secondaryActionButton = findViewById(R.id.secondaryActionButton)

        configureSystemBars()
        refreshCurrentSetup()

        findViewById<MaterialButton>(R.id.homeWifiOptionButton).setOnClickListener {
            showRecommendation(GuidedRecommendation.homeWifi(this))
        }
        findViewById<MaterialButton>(R.id.oldPhoneOptionButton).setOnClickListener {
            showRecommendation(GuidedRecommendation.oldPhoneRelay(this))
        }
        findViewById<MaterialButton>(R.id.serverOptionButton).setOnClickListener {
            showRecommendation(GuidedRecommendation.ownServer(this))
        }
        findViewById<MaterialButton>(R.id.notSureOptionButton).setOnClickListener {
            showRecommendation(GuidedRecommendation.notSure(this))
        }

        primaryActionButton.setOnClickListener { runPrimaryAction() }
        secondaryActionButton.setOnClickListener { runSecondaryAction() }

        showRecommendation(GuidedRecommendation.notSure(this))
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentSetup()
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

    private fun showRecommendation(recommendation: GuidedRecommendation) {
        currentRecommendation = recommendation
        recommendationTitleText.text = recommendation.title
        recommendationDetailText.text = recommendation.detail
        recommendationStatusText.text = recommendation.status
        recommendationStatusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (recommendation.highlightAsReady) R.color.cp_success else R.color.cp_warning
            )
        )
        primaryActionButton.text = recommendation.primaryActionLabel
        secondaryActionButton.text = recommendation.secondaryActionLabel
        secondaryActionButton.visibility =
            if (recommendation.secondaryAction == SecondaryAction.NONE) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun runPrimaryAction() {
        when (currentRecommendation.primaryAction) {
            PrimaryAction.SET_LOCAL_WIFI -> {
                val updated = ProfileStore.setActiveProfileWakeMethod(prefs, WakeMethodId.LOCAL_WIFI)
                refreshCurrentSetup()
                recommendationStatusText.text = getString(
                    R.string.guided_setup_status_method_selected,
                    updated.name,
                    friendlyMethodName(WakeMethodId.LOCAL_WIFI)
                )
                recommendationStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
            }

            PrimaryAction.SET_HOME_RELAY_SERVER -> {
                val updated = ProfileStore.setActiveProfileWakeMethod(prefs, WakeMethodId.HOME_RELAY)
                refreshCurrentSetup()
                recommendationStatusText.text = getString(
                    R.string.guided_setup_status_method_selected,
                    updated.name,
                    friendlyMethodName(WakeMethodId.HOME_RELAY)
                )
                recommendationStatusText.setTextColor(ContextCompat.getColor(this, R.color.cp_success))
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            PrimaryAction.OPEN_HOME_RELAY_MODE -> {
                startActivity(Intent(this, HomeRelayActivity::class.java))
            }

            PrimaryAction.OPEN_SETTINGS -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
    }

    private fun runSecondaryAction() {
        when (currentRecommendation.secondaryAction) {
            SecondaryAction.OPEN_SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))
            SecondaryAction.OPEN_HOME_RELAY_MODE -> startActivity(Intent(this, HomeRelayActivity::class.java))
            SecondaryAction.NONE -> Unit
        }
    }

    private fun friendlyMethodName(methodId: WakeMethodId): String {
        return when (methodId) {
            WakeMethodId.LOCAL_WIFI -> getString(R.string.guided_setup_choice_home_wifi_title)
            WakeMethodId.HOME_RELAY -> getString(R.string.guided_setup_choice_server_title)
            WakeMethodId.SPARE_ANDROID -> getString(R.string.guided_setup_choice_old_phone_title)
            WakeMethodId.SMART_PLUG -> getString(R.string.guided_setup_other_smart_plug)
            WakeMethodId.ADVANCED_ROUTER_VPN -> getString(R.string.guided_setup_other_router_vpn)
            WakeMethodId.SMART_HOME -> getString(R.string.guided_setup_other_smart_home)
        }
    }

    private enum class PrimaryAction {
        SET_LOCAL_WIFI,
        SET_HOME_RELAY_SERVER,
        OPEN_HOME_RELAY_MODE,
        OPEN_SETTINGS
    }

    private enum class SecondaryAction {
        OPEN_SETTINGS,
        OPEN_HOME_RELAY_MODE,
        NONE
    }

    private data class GuidedRecommendation(
        val title: String,
        val detail: String,
        val status: String,
        val primaryActionLabel: String,
        val primaryAction: PrimaryAction,
        val secondaryActionLabel: String,
        val secondaryAction: SecondaryAction,
        val highlightAsReady: Boolean
    ) {
        companion object {
            fun homeWifi(activity: GuidedSetupActivity) = GuidedRecommendation(
                title = activity.getString(R.string.guided_setup_home_wifi_result_title),
                detail = activity.getString(R.string.guided_setup_home_wifi_result_detail),
                status = activity.getString(R.string.guided_setup_home_wifi_result_status),
                primaryActionLabel = activity.getString(R.string.guided_setup_action_use_home_wifi),
                primaryAction = PrimaryAction.SET_LOCAL_WIFI,
                secondaryActionLabel = activity.getString(R.string.guided_setup_action_open_settings),
                secondaryAction = SecondaryAction.OPEN_SETTINGS,
                highlightAsReady = true
            )

            fun oldPhoneRelay(activity: GuidedSetupActivity) = GuidedRecommendation(
                title = activity.getString(R.string.guided_setup_old_phone_result_title),
                detail = activity.getString(R.string.guided_setup_old_phone_result_detail),
                status = activity.getString(R.string.guided_setup_old_phone_result_status),
                primaryActionLabel = activity.getString(R.string.guided_setup_action_open_home_relay),
                primaryAction = PrimaryAction.OPEN_HOME_RELAY_MODE,
                secondaryActionLabel = activity.getString(R.string.guided_setup_action_open_settings),
                secondaryAction = SecondaryAction.OPEN_SETTINGS,
                highlightAsReady = false
            )

            fun ownServer(activity: GuidedSetupActivity) = GuidedRecommendation(
                title = activity.getString(R.string.guided_setup_server_result_title),
                detail = activity.getString(R.string.guided_setup_server_result_detail),
                status = activity.getString(R.string.guided_setup_server_result_status),
                primaryActionLabel = activity.getString(R.string.guided_setup_action_use_server),
                primaryAction = PrimaryAction.SET_HOME_RELAY_SERVER,
                secondaryActionLabel = activity.getString(R.string.guided_setup_action_open_settings),
                secondaryAction = SecondaryAction.OPEN_SETTINGS,
                highlightAsReady = true
            )

            fun notSure(activity: GuidedSetupActivity) = GuidedRecommendation(
                title = activity.getString(R.string.guided_setup_not_sure_result_title),
                detail = activity.getString(R.string.guided_setup_not_sure_result_detail),
                status = activity.getString(R.string.guided_setup_not_sure_result_status),
                primaryActionLabel = activity.getString(R.string.guided_setup_action_start_home_wifi),
                primaryAction = PrimaryAction.SET_LOCAL_WIFI,
                secondaryActionLabel = activity.getString(R.string.guided_setup_action_open_home_relay),
                secondaryAction = SecondaryAction.OPEN_HOME_RELAY_MODE,
                highlightAsReady = true
            )
        }
    }
}
