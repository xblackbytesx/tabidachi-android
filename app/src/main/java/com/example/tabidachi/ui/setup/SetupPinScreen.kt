package com.example.tabidachi.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.auth.BiometricHelper
import com.example.tabidachi.ui.components.PinInput
import com.example.tabidachi.ui.theme.TextMuted

private enum class PinStep { ENTER, CONFIRM }

@Composable
fun SetupPinScreen(
    app: TabidachiApp,
    activity: FragmentActivity,
    onComplete: () -> Unit,
) {
    var step by remember { mutableStateOf(PinStep.ENTER) }
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(false) }

    val biometricHelper = remember { BiometricHelper() }
    val canUseBiometric = remember { biometricHelper.canAuthenticate(activity) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (step == PinStep.ENTER) "Set a PIN" else "Confirm your PIN",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (step == PinStep.ENTER) "Protect your travel data with a 4-digit PIN"
            else "Enter your PIN again to confirm",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        PinInput(
            pin = pin,
            error = error,
            onDigit = { digit ->
                if (pin.length < 4) {
                    pin += digit
                    error = false
                    errorMessage = null
                    if (pin.length == 4) {
                        when (step) {
                            PinStep.ENTER -> {
                                firstPin = pin
                                pin = ""
                                step = PinStep.CONFIRM
                            }
                            PinStep.CONFIRM -> {
                                if (pin == firstPin) {
                                    app.authManager.setupPin(pin)
                                    if (biometricEnabled) {
                                        app.authManager.setBiometricEnabled(true)
                                    }
                                    app.isAuthenticated = true
                                    onComplete()
                                } else {
                                    error = true
                                    errorMessage = "PINs don't match. Try again."
                                    pin = ""
                                    firstPin = ""
                                    step = PinStep.ENTER
                                }
                            }
                        }
                    }
                }
            },
            onDelete = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }
            },
        )

        if (canUseBiometric && step == PinStep.ENTER) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable biometric unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = {
            app.isAuthenticated = true
            onComplete()
        }) {
            Text("Skip for now", color = TextMuted)
        }
    }
}
