package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.RefugeDashboardResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RefugeDashboardState(
    val data: RefugeDashboardResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/** ViewModel della Dashboard IoT del rifugio (dati mock dal backend). */
class RefugeDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val _state = MutableStateFlow(RefugeDashboardState())
    val state: StateFlow<RefugeDashboardState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.getRefugeDashboard() }
                .onSuccess { resp ->
                    if (resp.isSuccessful && resp.body() != null) {
                        _state.update { it.copy(isLoading = false, data = resp.body()) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Impossibile caricare la dashboard.") }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Errore di rete.") }
                }
        }
    }
}
