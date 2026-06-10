package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Metrica di ordinamento della classifica (toggle client-side, niente refetch). */
enum class LeaderboardMetric { KM, ELEVATION, POINTS }

/**
 * Stato della classifica settimanale.
 *
 * Il backend ritorna per ogni utente TUTTE le metriche (km/dislivello/punti):
 * il cambio metrica è quindi un semplice ri-ordinamento locale ([ranked]),
 * senza nuova chiamata di rete.
 */
data class LeaderboardState(
    val items: List<LeaderboardEntry> = emptyList(),
    val metric: LeaderboardMetric = LeaderboardMetric.KM,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /** Lista ordinata in base alla metrica selezionata (decrescente). */
    val ranked: List<LeaderboardEntry>
        get() = when (metric) {
            LeaderboardMetric.KM -> items.sortedByDescending { it.km }
            LeaderboardMetric.ELEVATION -> items.sortedByDescending { it.elevM }
            LeaderboardMetric.POINTS -> items.sortedByDescending { it.points }
        }
}

/** ViewModel della classifica settimanale tra i seguiti. */
class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val _state = MutableStateFlow(LeaderboardState())
    val state: StateFlow<LeaderboardState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.getWeeklyLeaderboard() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.update { it.copy(isLoading = false, items = resp.body()?.items.orEmpty()) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Impossibile caricare la classifica.") }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Errore di rete.") }
                }
        }
    }

    fun setMetric(m: LeaderboardMetric) {
        _state.update { it.copy(metric = m) }
    }
}
