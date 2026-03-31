package com.example.tabidachi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.data.TripSummary
import com.example.tabidachi.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val ownedTrips: List<TripSummary> = emptyList(),
    val sharedTrips: List<TripSummary> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val refreshFailed: Boolean = false,
    val lastSyncedAt: Long? = null,
)

class DashboardViewModel(private val app: TabidachiApp) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        viewModelScope.launch {
            app.tripRepository.observeTrips().collect { trips ->
                val owned = trips.filter { !it.isShared }
                val shared = trips.filter { it.isShared }
                _uiState.value = _uiState.value.copy(
                    ownedTrips = owned,
                    sharedTrips = shared,
                    isLoading = false,
                    lastSyncedAt = owned.maxOfOrNull { it.lastSyncedAt },
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            // Refresh shared trips in the background — best-effort, failures keep the offline cache intact
            launch { app.tripRepository.refreshSharedTrips() }
            // Only call the authenticated list endpoint when an account is configured.
            // Users with only pinned shared trips have no token and the call would always fail.
            if (app.secureStorage.serverUrl.isNotBlank() && app.secureStorage.apiToken.isNotBlank()) {
                when (val result = app.tripRepository.refreshTrips()) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(isRefreshing = false)
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = if (_uiState.value.ownedTrips.isEmpty()) result.message else null,
                            refreshFailed = _uiState.value.ownedTrips.isNotEmpty(),
                        )
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun consumeRefreshError() {
        _uiState.value = _uiState.value.copy(refreshFailed = false)
    }

    fun removeSharedTrip(id: String) {
        viewModelScope.launch {
            app.tripRepository.removeSharedTrip(id)
            if (_uiState.value.sharedTrips.all { it.id == id }) {
                app.prefsManager.hasPinnedSharedTrips = false
            }
        }
    }
}
