package com.example.tabidachi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.data.SyncStatus
import com.example.tabidachi.data.TripSummary
import com.example.tabidachi.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val ownedTrips: List<TripSummary> = emptyList(),
    val sharedTrips: List<TripSummary> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastSyncedAt: Long? = null,
)

class DashboardViewModel(private val app: TabidachiApp) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    val syncStatus = app.tripRepository.syncStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncStatus.Idle)

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
            when (val result = app.tripRepository.refreshTrips()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = if (_uiState.value.ownedTrips.isEmpty()) result.message else null,
                    )
                }
            }
        }
    }

    fun removeSharedTrip(id: String) {
        viewModelScope.launch {
            app.tripRepository.removeSharedTrip(id)
        }
    }
}
