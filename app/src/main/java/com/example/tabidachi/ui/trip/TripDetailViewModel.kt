package com.example.tabidachi.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.data.TripSummary
import com.example.tabidachi.network.ApiResult
import com.example.tabidachi.network.ApiTripData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TripDetailUiState(
    val summary: TripSummary? = null,
    val data: ApiTripData? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val refreshFailed: Boolean = false,
)

class TripDetailViewModel(
    private val app: TabidachiApp,
    private val tripId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState

    init {
        // Observe cached detail from Room
        viewModelScope.launch {
            app.tripRepository.observeTripDetail(tripId).collect { pair ->
                if (pair != null) {
                    _uiState.value = _uiState.value.copy(
                        summary = pair.first,
                        data = pair.second,
                        isLoading = false,
                    )
                }
            }
        }
        // Fetch from API
        refresh()
    }

    fun consumeRefreshError() {
        _uiState.value = _uiState.value.copy(refreshFailed = false)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            when (val result = app.tripRepository.refreshTrip(tripId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isLoading = false,
                        error = if (_uiState.value.data == null) result.message else null,
                        refreshFailed = _uiState.value.data != null,
                    )
                }
            }
        }
    }
}
