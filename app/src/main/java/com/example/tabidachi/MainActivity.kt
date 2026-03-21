package com.example.tabidachi

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
