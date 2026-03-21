package com.example.tabidachi.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SetupUiState(
    val serverUrl: String = "",
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class SetupViewModel(private val app: TabidachiApp) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun updateToken(token: String) {
        _uiState.value = _uiState.value.copy(token = token, error = null)
    }

    fun connect(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.token.isBlank()) {
            _uiState.value = state.copy(error = "Please fill in both fields")
            return
        }

        var url = state.serverUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = app.api.testConnection(url, state.token.trim())) {
                is ApiResult.Success -> {
                    app.secureStorage.serverUrl = url
                    app.secureStorage.apiToken = state.token.trim()
                    // Initial sync — cache all trips
                    app.tripRepository.refreshTrips()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Connection failed: ${result.message}",
                    )
                }
            }
        }
    }
}
