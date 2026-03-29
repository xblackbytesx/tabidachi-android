package com.example.tabidachi.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tabidachi_prefs", Context.MODE_PRIVATE)

    var isOledEnabled: Boolean
        get() = prefs.getBoolean(KEY_OLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OLED, value).apply()

    var defaultTripId: String?
        get() = prefs.getString(KEY_DEFAULT_TRIP_ID, null)
        set(value) = prefs.edit().putString(KEY_DEFAULT_TRIP_ID, value).apply()

    var isDefaultTripEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_TRIP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_TRIP_ENABLED, value).apply()

    var autoLockTimeoutMs: Long
        get() = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_TIMEOUT_MS)
        set(value) = prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, value).apply()

    var setupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var hasPinnedSharedTrips: Boolean
        get() = prefs.getBoolean(KEY_HAS_PINNED_SHARED, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_PINNED_SHARED, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_OLED = "oled_enabled"
        private const val KEY_DEFAULT_TRIP_ID = "default_trip_id"
        private const val KEY_DEFAULT_TRIP_ENABLED = "default_trip_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_HAS_PINNED_SHARED = "has_pinned_shared_trips"
        const val DEFAULT_TIMEOUT_MS = 60_000L
    }
}
