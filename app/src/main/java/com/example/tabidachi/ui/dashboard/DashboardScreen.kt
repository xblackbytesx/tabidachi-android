package com.example.tabidachi.ui.dashboard

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.data.SyncStatus
import com.example.tabidachi.ui.components.TripCard
import com.example.tabidachi.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    app: TabidachiApp,
    onTripClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenSharedTrip: (serverUrl: String, shareToken: String) -> Unit = { _, _ -> },
) {
    val viewModel = remember { DashboardViewModel(app) }
    val uiState by viewModel.uiState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showShareDialog by remember { mutableStateOf(false) }

    val allTrips = uiState.ownedTrips + uiState.sharedTrips

    // Show snackbar on sync error when we have cached data
    LaunchedEffect(syncStatus) {
        if (syncStatus is SyncStatus.Error && allTrips.isNotEmpty()) {
            snackbarHostState.showSnackbar("Couldn't refresh — showing cached data")
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Tabidachi")
                            if (uiState.lastSyncedAt != null) {
                                val ago = formatTimeAgo(uiState.lastSyncedAt!!)
                                Text(
                                    text = "Last synced $ago",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Open shared trip",
                            )
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            uiState.isLoading && allTrips.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.error != null && allTrips.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Couldn't load trips",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            allTrips.isEmpty() && !uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextMuted,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No trips yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create your first trip on the web",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.ownedTrips, key = { it.id }) { trip ->
                            TripCard(
                                trip = trip,
                                onClick = { onTripClick(trip.id) },
                            )
                        }

                        if (uiState.sharedTrips.isNotEmpty()) {
                            item(key = "shared_header") {
                                Text(
                                    text = "Shared with me",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (uiState.ownedTrips.isNotEmpty()) 8.dp else 0.dp, bottom = 4.dp),
                                )
                            }
                            items(uiState.sharedTrips, key = { it.id }) { trip ->
                                TripCard(
                                    trip = trip,
                                    onClick = { onTripClick(trip.id) },
                                    onRemove = { viewModel.removeSharedTrip(trip.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
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
                    text = "Paste a share link to view a trip without an account.",
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

/** Parses a share URL like https://host/share/tbd_share_xxx into (baseUrl, token). */
private fun parseShareUrl(url: String): Pair<String, String>? {
    return try {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: "https"
        if (scheme != "http" && scheme != "https") return null
        val path = uri.path ?: return null
        val shareIdx = path.indexOf("/share/")
        if (shareIdx < 0) return null
        val token = path.substring(shareIdx + 7) // strip "/share/"
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

private fun formatTimeAgo(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMs
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
