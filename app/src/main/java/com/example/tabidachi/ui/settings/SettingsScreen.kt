package com.example.tabidachi.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.tabidachi.MainActivity
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.auth.BiometricHelper
import com.example.tabidachi.ui.components.PinInput
import com.example.tabidachi.ui.theme.TextMuted
import com.example.tabidachi.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: TabidachiApp,
    activity: FragmentActivity,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onConnectAccount: () -> Unit = {},
) {
    val viewModel = viewModel { SettingsViewModel(app) }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val biometricHelper = remember { BiometricHelper() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showTripPicker by remember { mutableStateOf(false) }
    var showTimeoutPicker by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setBiometricAvailable(biometricHelper.canAuthenticate(activity))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // --- Appearance ---
            SectionHeader("Appearance")

            SettingsRow(
                title = "OLED Black",
                subtitle = "Pure black backgrounds for battery savings",
            ) {
                Switch(
                    checked = uiState.isOled,
                    onCheckedChange = {
                        viewModel.setOled(it)
                        (activity as? MainActivity)?.updateOled(it)
                    },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // --- Default Trip ---
            SectionHeader("Quick Access")

            SettingsRow(
                title = "Open to trip",
                subtitle = "Skip dashboard and go straight to a trip",
            ) {
                Switch(
                    checked = uiState.isDefaultTripEnabled,
                    onCheckedChange = { viewModel.setDefaultTripEnabled(it) },
                )
            }

            if (uiState.isDefaultTripEnabled) {
                val selectedTrip = uiState.trips.find { it.id == uiState.defaultTripId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTripPicker = true }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedTrip?.title ?: "Select a trip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedTrip != null) MaterialTheme.colorScheme.onSurface
                        else TextMuted,
                    )

                    DropdownMenu(
                        expanded = showTripPicker,
                        onDismissRequest = { showTripPicker = false },
                    ) {
                        uiState.trips.forEach { trip ->
                            DropdownMenuItem(
                                text = { Text(trip.title) },
                                onClick = {
                                    viewModel.setDefaultTrip(trip.id)
                                    showTripPicker = false
                                },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // --- Security ---
            SectionHeader("Security")

            SettingsRow(
                title = "Lock with PIN",
                subtitle = if (uiState.isPinEnabled) "PIN is set" else "No PIN configured",
            ) {
                Switch(
                    checked = uiState.isPinEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinSetup = true
                        } else {
                            viewModel.disablePin()
                            (activity as? MainActivity)?.updateSecureFlag(false)
                        }
                    },
                )
            }

            if (uiState.isPinEnabled && uiState.isBiometricAvailable) {
                SettingsRow(
                    title = "Biometric unlock",
                    subtitle = "Use fingerprint or face to unlock",
                ) {
                    Switch(
                        checked = uiState.isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                    )
                }
            }

            if (uiState.isPinEnabled) {
                val selectedTimeout = when (uiState.autoLockTimeout) {
                    30_000L -> "30 seconds"
                    60_000L -> "1 minute"
                    120_000L -> "2 minutes"
                    300_000L -> "5 minutes"
                    else -> "1 minute"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimeoutPicker = true }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Auto-lock timeout",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            selectedTimeout,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }

                    DropdownMenu(
                        expanded = showTimeoutPicker,
                        onDismissRequest = { showTimeoutPicker = false },
                    ) {
                        val options = listOf(
                            30_000L to "30 seconds",
                            60_000L to "1 minute",
                            120_000L to "2 minutes",
                            300_000L to "5 minutes",
                        )
                        options.forEach { (ms, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.setAutoLockTimeout(ms)
                                    showTimeoutPicker = false
                                },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // --- Account ---
            SectionHeader("Account")

            if (uiState.serverUrl.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                ) {
                    Column {
                        Text(
                            "Server",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            uiState.serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            } else {
                SettingsRow(
                    title = "No account connected",
                    subtitle = "Add a Tabidachi account to manage your own trips",
                ) {}

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onConnectAccount,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Connect account")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Disconnect?") },
            text = { Text("This will clear all cached data and credentials. You'll need to set up again.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    scope.launch {
                        viewModel.logout()
                        onLogout()
                    }
                }) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onPinSet = { pin ->
                viewModel.enablePin(pin)
                (activity as? MainActivity)?.updateSecureFlag(true)
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false },
        )
    }
}

@Composable
private fun PinSetupDialog(
    onPinSet: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(0) } // 0 = enter, 1 = confirm
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (step == 0) "Set a PIN" else "Confirm your PIN")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (step == 0) "Enter a 4-digit PIN"
                    else "Enter the same PIN again",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
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
                                    0 -> {
                                        firstPin = pin
                                        pin = ""
                                        step = 1
                                    }
                                    1 -> {
                                        if (pin == firstPin) {
                                            onPinSet(pin)
                                        } else {
                                            error = true
                                            errorMessage = "PINs don't match. Try again."
                                            pin = ""
                                            firstPin = ""
                                            step = 0
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
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        trailing()
    }
}
