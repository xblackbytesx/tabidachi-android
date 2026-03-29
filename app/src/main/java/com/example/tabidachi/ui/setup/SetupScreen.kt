package com.example.tabidachi.ui.setup

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.ui.theme.IndigoAccent
import com.example.tabidachi.ui.theme.TextMuted

@Composable
fun SetupScreen(
    app: TabidachiApp,
    onSetupComplete: () -> Unit,
    onOpenSharedTrip: (serverUrl: String, shareToken: String) -> Unit = { _, _ -> },
    onViewSavedTrips: () -> Unit = {},
) {
    val viewModel = remember { SetupViewModel(app) }
    val uiState by viewModel.uiState.collectAsState()
    var tokenVisible by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Flight,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = IndigoAccent,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tabidachi",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect to your Tabidachi server",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = uiState.serverUrl,
            onValueChange = viewModel::updateServerUrl,
            label = { Text("Server URL") },
            placeholder = { Text("https://tabidachi.example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            enabled = !uiState.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.token,
            onValueChange = viewModel::updateToken,
            label = { Text("Personal Access Token") },
            placeholder = { Text("tbd_...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (tokenVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                    Icon(
                        imageVector = if (tokenVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (tokenVisible) "Hide token" else "Show token",
                    )
                }
            },
            enabled = !uiState.isLoading,
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.connect(onSetupComplete) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !uiState.isLoading,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Connect")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "— or —",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { showShareDialog = true }) {
            Text("Received a share link?")
        }

        if (app.prefsManager.hasPinnedSharedTrips) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onViewSavedTrips) {
                Text("View saved trips →")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showShareDialog) {
        ShareLinkDialog(
            onDismiss = { showShareDialog = false },
            onOpen = { serverUrl, shareToken ->
                showShareDialog = false
                onOpenSharedTrip(serverUrl, shareToken)
            },
        )
    }
}

@Composable
private fun ShareLinkDialog(
    onDismiss: () -> Unit,
    onOpen: (serverUrl: String, shareToken: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun tryOpen() {
        val parsed = parseShareUrl(url.trim())
        if (parsed == null) {
            error = "Paste a valid Tabidachi share link"
        } else {
            onOpen(parsed.first, parsed.second)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open shared trip") },
        text = {
            Column {
                Text(
                    text = "Paste the share link you received.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    placeholder = { Text("https://…/share/tbd_share_…") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { tryOpen() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { tryOpen() }) { Text("Open") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun parseShareUrl(url: String): Pair<String, String>? {
    return try {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: "https"
        if (scheme != "http" && scheme != "https") return null
        val path = uri.path ?: return null
        val shareIdx = path.indexOf("/share/")
        if (shareIdx < 0) return null
        val token = path.substring(shareIdx + 7)
        if (token.isBlank()) return null
        val baseUrl = buildString {
            append(scheme)
            append("://")
            append(uri.authority ?: return null)
        }
        Pair(baseUrl, token)
    } catch (_: Exception) {
        null
    }
}
