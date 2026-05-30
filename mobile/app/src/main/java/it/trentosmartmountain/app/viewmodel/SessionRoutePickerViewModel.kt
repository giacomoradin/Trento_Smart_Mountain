package it.trentosmartmountain.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.SentieroDestinazioneDto
import it.trentosmartmountain.app.data.remote.dto.SentieroDettaglioDto
import it.trentosmartmountain.app.data.remote.dto.SentieroListItemDto
import it.trentosmartmountain.app.data.sentieri.SentieroMappers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.IOException

/**
 * ViewModel del popup **"Scegli percorso sulla mappa"** (modalità DB sentieri).
 *
 * State machine a tre step:
 *  1. [Step.Destinations] — marker di tutte le destinazioni;
 *  2. [Step.TrailsForDestination] — sentieri che raggiungono la destinazione + start point;
 *  3. [Step.TrailDetail] — polyline completa + scheda info + conferma.
 *
 * Nessuna navigazione di screen: il dialog vive sopra la tab "Pianifica".
 */
class SessionRoutePickerViewModel : ViewModel() {

    enum class Step { Destinations, TrailsForDestination, TrailDetail }

    data class UiState(
        val step: Step = Step.Destinations,
        val destinations: List<SentieroDestinazioneDto> = emptyList(),
        val selectedDestination: SentieroDestinazioneDto? = null,
        val trailsForDestination: List<SentieroListItemDto> = emptyList(),
        val selectedTrailCode: String? = null,
        val selectedTrailDetail: SentieroDettaglioDto? = null,
        val selectedTrailPolyline: List<GeoPoint> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var destinationsLoaded = false

    /** Carica le destinazioni una sola volta all'apertura del dialog. */
    fun onOpen() {
        if (destinationsLoaded || _uiState.value.isLoading) return
        loadDestinations()
    }

    fun loadDestinations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = TsmApiClient.service().getSentieriDestinazioni()
                if (response.isSuccessful) {
                    val data = response.body()?.data.orEmpty()
                    destinationsLoaded = true
                    _uiState.update {
                        it.copy(isLoading = false, destinations = data, step = Step.Destinations)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Errore server (${response.code()}).") }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Nessuna connessione al server.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Errore imprevisto.") }
            }
        }
    }

    fun onDestinationClick(dest: SentieroDestinazioneDto) {
        _uiState.update {
            it.copy(
                step = Step.TrailsForDestination,
                selectedDestination = dest,
                trailsForDestination = emptyList(),
                selectedTrailCode = null,
                selectedTrailDetail = null,
                selectedTrailPolyline = emptyList(),
                error = null,
            )
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = TsmApiClient.service().getSentieriByDestinazione(dest.nome)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, trailsForDestination = response.body()?.data.orEmpty()) }
                } else if (response.code() == 404) {
                    _uiState.update { it.copy(isLoading = false, error = "Nessun sentiero per questa destinazione.") }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Errore server (${response.code()}).") }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Nessuna connessione al server.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Errore imprevisto.") }
            }
        }
    }

    fun onTrailClick(codice: String) {
        _uiState.update {
            it.copy(
                step = Step.TrailDetail,
                selectedTrailCode = codice,
                selectedTrailDetail = null,
                selectedTrailPolyline = emptyList(),
                error = null,
            )
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = TsmApiClient.service().getSentieroByCodice(codice)
                if (response.isSuccessful) {
                    val detail = response.body()?.data
                    if (detail == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Sentiero non trovato.") }
                        return@launch
                    }
                    val polyline = SentieroMappers.parsePercorsoToGeoPoints(detail.percorsoCoordinate, maxPoints = 1000)
                    _uiState.update {
                        it.copy(isLoading = false, selectedTrailDetail = detail, selectedTrailPolyline = polyline)
                    }
                } else if (response.code() == 404) {
                    // Torna all'elenco sentieri con messaggio.
                    _uiState.update {
                        it.copy(isLoading = false, step = Step.TrailsForDestination, error = "Sentiero non trovato.")
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, step = Step.TrailsForDestination, error = "Errore server (${response.code()}).")
                    }
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(isLoading = false, step = Step.TrailsForDestination, error = "Nessuna connessione al server.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, step = Step.TrailsForDestination, error = e.message ?: "Errore imprevisto.")
                }
            }
        }
    }

    /** Indietro: TrailDetail → TrailsForDestination; TrailsForDestination → Destinations. */
    fun onBack() {
        _uiState.update { state ->
            when (state.step) {
                Step.TrailDetail -> state.copy(
                    step = Step.TrailsForDestination,
                    selectedTrailCode = null,
                    selectedTrailDetail = null,
                    selectedTrailPolyline = emptyList(),
                    error = null,
                )
                Step.TrailsForDestination -> state.copy(
                    step = Step.Destinations,
                    selectedDestination = null,
                    trailsForDestination = emptyList(),
                    error = null,
                )
                Step.Destinations -> state
            }
        }
    }

    /** Reset completo: chiamato alla chiusura del dialog per ripartire pulito. */
    fun reset() {
        _uiState.update { UiState(destinations = it.destinations) }
    }
}
