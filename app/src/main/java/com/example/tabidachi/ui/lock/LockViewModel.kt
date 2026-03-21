package com.example.tabidachi.ui.lock

import androidx.lifecycle.ViewModel
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.auth.PinVerifyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LockUiState(
    val pin: String = "",
    val error: Boolean = false,
    val errorMessage: String? = null,
    val lockedOut: Boolean = false,
    val lockoutRemainingMs: Long = 0L,
)

class LockViewModel(private val app: TabidachiApp) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.lockedOut || state.pin.length >= 4) return

        val newPin = state.pin + digit
        _uiState.value = state.copy(pin = newPin, error = false, errorMessage = null)

        if (newPin.length == 4) {
            verifyPin(newPin)
        }
    }

    fun onDelete() {
        val state = _uiState.value
        if (state.pin.isNotEmpty()) {
            _uiState.value = state.copy(pin = state.pin.dropLast(1), error = false, errorMessage = null)
        }
    }

    private fun verifyPin(pin: String) {
        when (val result = app.authManager.verifyPin(pin)) {
            is PinVerifyResult.Success -> {
                app.isAuthenticated = true
                _uiState.value = _uiState.value.copy(pin = "")
            }
            is PinVerifyResult.Wrong -> {
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = true,
                    errorMessage = "Wrong PIN",
                )
            }
            is PinVerifyResult.LockedOut -> {
                val seconds = (result.remainingMs / 1000).coerceAtLeast(1)
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = true,
                    lockedOut = true,
                    lockoutRemainingMs = result.remainingMs,
                    errorMessage = "Too many attempts. Try again in ${seconds}s",
                )
            }
        }
    }

    fun checkLockout() {
        val lockoutUntil = app.secureStorage.lockoutUntil
        if (lockoutUntil > 0L) {
            val remaining = lockoutUntil - System.currentTimeMillis()
            if (remaining > 0) {
                val seconds = (remaining / 1000).coerceAtLeast(1)
                _uiState.value = _uiState.value.copy(
                    lockedOut = true,
                    lockoutRemainingMs = remaining,
                    errorMessage = "Too many attempts. Try again in ${seconds}s",
                )
            } else {
                _uiState.value = _uiState.value.copy(lockedOut = false, errorMessage = null)
            }
        }
    }

    fun isAuthenticated(): Boolean = app.isAuthenticated
}
