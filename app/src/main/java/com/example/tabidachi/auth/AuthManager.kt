package com.example.tabidachi.auth

import com.example.tabidachi.data.SecureStorage

class AuthManager(private val secureStorage: SecureStorage) {

    private val pinManager = PinManager()

    fun setupPin(pin: String) {
        val (hash, salt) = pinManager.hashPin(pin)
        secureStorage.pinHash = hash
        secureStorage.pinSalt = salt
        secureStorage.failedAttempts = 0
        secureStorage.lockoutUntil = 0L
    }

    fun verifyPin(pin: String): PinVerifyResult {
        val lockoutUntil = secureStorage.lockoutUntil
        if (lockoutUntil > 0L) {
            val remaining = lockoutUntil - System.currentTimeMillis()
            if (remaining > 0) {
                return PinVerifyResult.LockedOut(remaining)
            }
            secureStorage.lockoutUntil = 0L
        }

        val isValid = pinManager.verifyPin(pin, secureStorage.pinHash, secureStorage.pinSalt)
        if (isValid) {
            secureStorage.failedAttempts = 0
            secureStorage.lockoutUntil = 0L
            return PinVerifyResult.Success
        }

        val attempts = secureStorage.failedAttempts + 1
        secureStorage.failedAttempts = attempts
        val lockoutDuration = pinManager.getLockoutDuration(attempts)
        if (lockoutDuration > 0) {
            secureStorage.lockoutUntil = System.currentTimeMillis() + lockoutDuration
            return PinVerifyResult.LockedOut(lockoutDuration)
        }
        return PinVerifyResult.Wrong
    }

    fun clearPin() {
        secureStorage.pinHash = ""
        secureStorage.pinSalt = ""
        secureStorage.isBiometricEnabled = false
        secureStorage.failedAttempts = 0
        secureStorage.lockoutUntil = 0L
    }

    fun isPinConfigured(): Boolean = secureStorage.isPinConfigured()

    fun isBiometricEnabled(): Boolean = secureStorage.isBiometricEnabled

    fun setBiometricEnabled(enabled: Boolean) {
        secureStorage.isBiometricEnabled = enabled
    }
}
