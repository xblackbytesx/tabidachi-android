package com.example.tabidachi.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.auth.BiometricHelper
import com.example.tabidachi.ui.components.PinInput
import com.example.tabidachi.ui.theme.IndigoAccent
import com.example.tabidachi.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    app: TabidachiApp,
    activity: FragmentActivity,
    onUnlocked: () -> Unit,
) {
    val viewModel = remember { LockViewModel(app) }
    val uiState by viewModel.uiState.collectAsState()
    val biometricHelper = remember { BiometricHelper() }

    // Navigate when auth succeeds (from PIN or biometric)
    LaunchedEffect(Unit) {
        viewModel.authenticated.collect { authenticated ->
            if (authenticated) {
                onUnlocked()
            }
        }
    }

    // Trigger biometric on launch
    LaunchedEffect(Unit) {
        if (app.authManager.isBiometricEnabled() && biometricHelper.canAuthenticate(activity)) {
            biometricHelper.authenticate(
                activity = activity,
                onSuccess = {
                    app.isAuthenticated = true
                    viewModel.notifyAuthenticated()
                },
                onError = { /* User will use PIN */ },
            )
        }
    }

    // Periodically check lockout state
    LaunchedEffect(uiState.lockedOut) {
        if (uiState.lockedOut) {
            while (true) {
                delay(1000)
                viewModel.checkLockout()
                if (!viewModel.uiState.value.lockedOut) break
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Flight,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = IndigoAccent,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tabidachi",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Enter your PIN to unlock",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        PinInput(
            pin = uiState.pin,
            error = uiState.error,
            onDigit = { viewModel.onDigit(it) },
            onDelete = { viewModel.onDelete() },
        )
    }
}
