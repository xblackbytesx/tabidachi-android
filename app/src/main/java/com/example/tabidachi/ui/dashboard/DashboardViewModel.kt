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
    val trips: List<TripSummary> = emptyList(),
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
        // Observe cached trips from Room
        viewModelScope.launch {
            app.tripRepository.observeTrips().collect { trips ->
                _uiState.value = _uiState.value.copy(
                    trips = trips,
                    isLoading = false,
                    lastSyncedAt = trips.maxOfOrNull { it.lastSyncedAt },
                )
            }
        }
        // Initial refresh from API
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            when (val result = app.tripRepository.refreshTrips()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = if (_uiState.value.trips.isEmpty()) result.message else null,
                    )
                }
            }
        }
    }
}
