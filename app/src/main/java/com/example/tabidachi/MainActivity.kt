package com.example.tabidachi

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.example.tabidachi.navigation.TabidachiNavHost
import com.example.tabidachi.ui.theme.TabidachiTheme

class MainActivity : FragmentActivity() {

    var isOled by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TabidachiApp

        isOled = app.prefsManager.isOledEnabled

        // Immersive mode — hide the status bar for full screen real estate.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (app.secureStorage.isPinConfigured()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        setContent {
            TabidachiTheme(isOled = isOled) {
                TabidachiNavHost(app = app, activity = this)
            }
        }
    }

    fun updateOled(enabled: Boolean) {
        isOled = enabled
    }

    fun updateSecureFlag(pinEnabled: Boolean) {
        if (pinEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
