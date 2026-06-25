package com.powerbridge.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfigStore.forceDarkMode()
        super.onCreate(savedInstanceState)

        prefs = AppConfigStore.createPrefs(this)
        if (AppConfigStore.hasCompletedWelcome(prefs)) {
            openMainAndFinish()
            return
        }

        setContentView(R.layout.activity_welcome)
        configureSystemBars()

        findViewById<MaterialButton>(R.id.getStartedButton).setOnClickListener {
            AppConfigStore.markWelcomeCompleted(prefs)
            startActivity(Intent(this, GuidedSetupActivity::class.java))
            finish()
        }

        findViewById<MaterialButton>(R.id.continueToAppButton).setOnClickListener {
            AppConfigStore.markWelcomeCompleted(prefs)
            openMainAndFinish()
        }
    }

    private fun configureSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.cp_system_bar)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    private fun openMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
