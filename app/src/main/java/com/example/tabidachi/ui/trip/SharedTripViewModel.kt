package com.example.tabidachi.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.network.ApiResult
import com.example.tabidachi.network.ApiTripDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SharedTripUiState(
    val detail: ApiTripDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isPinned: Boolean = false,
    val isSaving: Boolean = false,
)

class SharedTripViewModel(
    private val app: TabidachiApp,
    private val serverUrl: String,
    private val shareToken: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedTripUiState())
    val uiState: StateFlow<SharedTripUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = SharedTripUiState(isLoading = true)
            when (val result = app.api.getSharedTrip(serverUrl, shareToken)) {
                is ApiResult.Success -> {
                    // Preserve isPinned=true if pin() was called concurrently while load was in-flight.
                    val alreadyPinned = _uiState.value.isPinned || app.tripRepository.isSharedTripPinned(result.data.id)
                    _uiState.value = SharedTripUiState(
                        detail = result.data,
                        isLoading = false,
                        isPinned = alreadyPinned,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = SharedTripUiState(
                        isLoading = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun pin() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            app.tripRepository.pinSharedTrip(detail, serverUrl, shareToken)
            _uiState.value = _uiState.value.copy(isSaving = false, isPinned = true)
        }
    }

    fun unpin() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            app.tripRepository.removeSharedTrip(detail.id)
            _uiState.value = _uiState.value.copy(isPinned = false)
        }
    }
}
