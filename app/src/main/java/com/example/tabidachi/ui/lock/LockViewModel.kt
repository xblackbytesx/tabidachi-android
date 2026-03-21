package com.example.tabidachi.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.auth.PinVerifyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LockUiState(
    val pin: String = "",
    val error: Boolean = false,
    val errorMessage: String? = null,
    val lockedOut: Boolean = false,
    val lockoutRemainingMs: Long = 0L,
    val isVerifying: Boolean = false,
)

class LockViewModel(private val app: TabidachiApp) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState

    private val _authenticated = MutableStateFlow(false)
    val authenticated: StateFlow<Boolean> = _authenticated

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.lockedOut || state.pin.length >= 4 || state.isVerifying) return

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
        _uiState.value = _uiState.value.copy(isVerifying = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                app.authManager.verifyPin(pin)
            }
            when (result) {
                is PinVerifyResult.Success -> {
                    app.isAuthenticated = true
                    _uiState.value = _uiState.value.copy(pin = "", isVerifying = false)
                    _authenticated.value = true
                }
                is PinVerifyResult.Wrong -> {
                    _uiState.value = _uiState.value.copy(
                        pin = "",
                        error = true,
                        errorMessage = "Wrong PIN",
                        isVerifying = false,
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
                        isVerifying = false,
                    )
                }
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

    fun notifyAuthenticated() {
        _authenticated.value = true
    }
}
