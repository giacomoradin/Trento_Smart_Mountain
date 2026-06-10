package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.WasteSimulationRequest
import it.trentosmartmountain.app.data.remote.dto.WasteSimulationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Simulatore Rifiuti & Logistica del rifugio (ADR-002, MVP read-only).
 * I campi del form restano String per una UX di editing libera; la conversione
 * (con default del simulatore) avviene solo al momento del calcolo.
 */
class WasteSimulatorViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val periodDays: String = "60",
        val beds: String = "50",
        val bedOccupancyPct: String = "60",
        val dayVisitors: String = "30",
        val wastePerGuestKg: String = "0.20",
        val wastePerVisitorKg: String = "0.10",
        val screeningPerGuestKg: String = "0.20",
        val compactorEnabled: Boolean = false,
        val isLoading: Boolean = false,
        val result: WasteSimulationResponse? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun update(transform: UiState.() -> UiState) {
        _uiState.update { it.transform().copy(error = null) }
    }

    fun simulate() {
        val s = _uiState.value
        val request = runCatching {
            WasteSimulationRequest(
                periodDays = s.periodDays.trim().toInt(),
                beds = s.beds.trim().toInt(),
                bedOccupancy = (s.bedOccupancyPct.trim().toDouble() / 100.0).coerceIn(0.0, 1.0),
                dayVisitors = s.dayVisitors.trim().toInt(),
                wastePerGuestKg = s.wastePerGuestKg.trim().toDouble(),
                wastePerVisitorKg = s.wastePerVisitorKg.trim().toDouble(),
                screeningPerGuestKg = s.screeningPerGuestKg.trim().toDouble(),
                compactorEnabled = s.compactorEnabled,
            )
        }.getOrElse {
            _uiState.update { st -> st.copy(error = "Controlla i campi: valori numerici non validi.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { TsmApiClient.service().simulateWaste(request) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.update { it.copy(isLoading = false, result = resp.body()) }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = "Errore server (${resp.code()}).")
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Rete non disponibile: ${err.message}")
                    }
                }
        }
    }
}
