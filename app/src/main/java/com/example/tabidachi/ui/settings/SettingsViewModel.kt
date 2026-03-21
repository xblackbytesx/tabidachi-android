package com.example.tabidachi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.data.TripSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isOled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isDefaultTripEnabled: Boolean = false,
    val defaultTripId: String? = null,
    val autoLockTimeout: Long = 60_000L,
    val serverUrl: String = "",
    val trips: List<TripSummary> = emptyList(),
)

class SettingsViewModel(private val app: TabidachiApp) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isOled = app.prefsManager.isOledEnabled,
            isPinEnabled = app.secureStorage.isPinConfigured(),
            isBiometricEnabled = app.authManager.isBiometricEnabled(),
            isDefaultTripEnabled = app.prefsManager.isDefaultTripEnabled,
            defaultTripId = app.prefsManager.defaultTripId,
            autoLockTimeout = app.prefsManager.autoLockTimeoutMs,
            serverUrl = app.secureStorage.serverUrl,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            app.tripRepository.observeTrips().collect { trips ->
                _uiState.value = _uiState.value.copy(trips = trips)
            }
        }
    }

    fun setOled(enabled: Boolean) {
        app.prefsManager.isOledEnabled = enabled
        _uiState.value = _uiState.value.copy(isOled = enabled)
    }

    fun setDefaultTripEnabled(enabled: Boolean) {
        app.prefsManager.isDefaultTripEnabled = enabled
        if (!enabled) {
            app.prefsManager.defaultTripId = null
        }
        _uiState.value = _uiState.value.copy(
            isDefaultTripEnabled = enabled,
            defaultTripId = if (enabled) _uiState.value.defaultTripId else null,
        )
    }

    fun setDefaultTrip(tripId: String?) {
        app.prefsManager.defaultTripId = tripId
        _uiState.value = _uiState.value.copy(defaultTripId = tripId)
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        app.prefsManager.autoLockTimeoutMs = timeoutMs
        _uiState.value = _uiState.value.copy(autoLockTimeout = timeoutMs)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        app.authManager.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
    }

    fun setBiometricAvailable(available: Boolean) {
        _uiState.value = _uiState.value.copy(isBiometricAvailable = available)
    }

    fun enablePin(pin: String) {
        app.authManager.setupPin(pin)
        app.isAuthenticated = true
        _uiState.value = _uiState.value.copy(isPinEnabled = true)
    }

    fun disablePin() {
        app.authManager.clearPin()
        _uiState.value = _uiState.value.copy(
            isPinEnabled = false,
            isBiometricEnabled = false,
        )
    }

    suspend fun logout() {
        app.tripRepository.clearAll()
        app.secureStorage.clearAll()
        app.prefsManager.clearAll()
        app.isAuthenticated = false
    }
}
