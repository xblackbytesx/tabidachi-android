package com.example.tabidachi.auth

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed class PinVerifyResult {
    data object Success : PinVerifyResult()
    data object Wrong : PinVerifyResult()
    data class LockedOut(val remainingMs: Long) : PinVerifyResult()
}

class PinManager {

    companion object {
        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH = 256
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private val secureRandom = SecureRandom()

        private val LOCKOUT_THRESHOLDS = listOf(
            5 to 30_000L,
            6 to 60_000L,
            7 to 300_000L,
            8 to 600_000L,
        )
    }

    fun hashPin(pin: String): Pair<String, String> {
        val saltBytes = ByteArray(32)
        secureRandom.nextBytes(saltBytes)
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)

        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hashBytes = factory.generateSecret(spec).encoded
            val hash = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            return Pair(hash, salt)
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPin(pin: String, storedHash: String, storedSalt: String): Boolean {
        val saltBytes = Base64.decode(storedSalt, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hashBytes = factory.generateSecret(spec).encoded
            val computedHash = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            return java.security.MessageDigest.isEqual(
                computedHash.toByteArray(),
                storedHash.toByteArray(),
            )
        } finally {
            spec.clearPassword()
        }
    }

    fun getLockoutDuration(failedAttempts: Int): Long {
        var duration = 0L
        for ((threshold, lockoutMs) in LOCKOUT_THRESHOLDS) {
            if (failedAttempts >= threshold) {
                duration = lockoutMs
            }
        }
        return duration
    }
}
