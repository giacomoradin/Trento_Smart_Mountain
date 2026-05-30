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
 * ViewModel del popup **"Scegli tra i percorsi suggeriti"** (modalità DB sentieri).
 *
 * State machine a tre step:
 *  1. [Step.Destinations] — marker delle destinazioni;
 *  2. [Step.TrailsForDestination] — sentieri che raggiungono la destinazione + start point;
 *  3. [Step.TrailDetail] — polyline completa + scheda info + conferma.
 *
 * **Sorgente dati**: all'apertura scarica una sola volta tutti i sentieri (senza coordinate)
 * via `getAllSentieri`. Destinazioni, conteggi, filtri e ricerca sono calcolati **client-side**
 * da questa lista; solo il dettaglio del sentiero scelto ([onTrailClick]) richiama il backend
 * per ottenere `percorsoCoordinate`. Nessuna navigazione di screen: il dialog vive sopra "Pianifica".
 */
class SessionRoutePickerViewModel : ViewModel() {

    enum class Step { Destinations, TrailsForDestination, TrailDetail }

    /**
     * Filtri applicati client-side ai sentieri: una destinazione è mostrata solo se ha almeno
     * un sentiero che li soddisfa. I valori `null`/insieme vuoto significano "nessun limite".
     * Vedi [RouteFilter.isActive] per il badge UI.
     */
    data class RouteFilter(
        val difficolta: Set<String> = emptySet(),
        val dislivelloMax: Int? = null,   // metri
        val distanzaMaxKm: Int? = null,   // km
        val tempoMaxMin: Int? = null,     // minuti (andata)
    ) {
        val activeCount: Int
            get() = listOf(
                difficolta.isNotEmpty(),
                dislivelloMax != null,
                distanzaMaxKm != null,
                tempoMaxMin != null,
            ).count { it }

        val isActive: Boolean get() = activeCount > 0
    }

    data class UiState(
        val step: Step = Step.Destinations,
        val destinations: List<SentieroDestinazioneDto> = emptyList(),
        val filter: RouteFilter = RouteFilter(),
        val searchQuery: String = "",
        val selectedDestination: SentieroDestinazioneDto? = null,
        val trailsForDestination: List<SentieroListItemDto> = emptyList(),
        val selectedTrailCode: String? = null,
        val selectedTrailDetail: SentieroDettaglioDto? = null,
        val selectedTrailPolyline: List<GeoPoint> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
    ) {
        /** Destinazioni filtrate client-side per la barra di ricerca (sul nome). */
        val visibleDestinations: List<SentieroDestinazioneDto>
            get() = if (searchQuery.isBlank()) {
                destinations
            } else {
                destinations.filter { it.nome.contains(searchQuery.trim(), ignoreCase = true) }
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Cache locale di TUTTI i sentieri (senza coordinate): sorgente per destinazioni/filtri/ricerca. */
    private var allTrails: List<SentieroListItemDto> = emptyList()
    private var trailsLoaded = false

    /** Carica i sentieri una sola volta all'apertura del dialog. */
    fun onOpen() {
        if (trailsLoaded || _uiState.value.isLoading) return
        loadDestinations()
    }

    /** Aggiorna i filtri e ricalcola le destinazioni client-side (nessuna chiamata di rete). */
    fun applyFilter(filter: RouteFilter) {
        if (filter == _uiState.value.filter) return
        _uiState.update { it.copy(filter = filter, destinations = computeDestinations(filter)) }
    }

    /** Azzera tutti i filtri (la ricerca testuale resta separata). */
    fun clearFilter() = applyFilter(RouteFilter())

    /** Aggiorna la query di ricerca (filtro client-side, nessuna chiamata di rete). */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** Scarica tutti i sentieri (una volta) e ricava le destinazioni con i filtri correnti. */
    fun loadDestinations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = TsmApiClient.service().getAllSentieri()
                if (response.isSuccessful) {
                    allTrails = response.body()?.data.orEmpty()
                    trailsLoaded = true
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            destinations = computeDestinations(it.filter),
                            step = Step.Destinations,
                        )
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

    /**
     * Ricava le destinazioni dai sentieri in cache applicando [filter]: una destinazione è
     * presente solo se ha ≥1 sentiero compatibile, e [SentieroDestinazioneDto.numeroSentieri]
     * conta i sentieri compatibili. Ordinate per nome.
     */
    private fun computeDestinations(filter: RouteFilter): List<SentieroDestinazioneDto> =
        allTrails
            .filter { matchesFilter(it, filter) }
            .groupBy { it.puntoFine?.nome }
            .mapNotNull { (nome, trails) ->
                if (nome.isNullOrBlank()) return@mapNotNull null
                val pf = trails.first().puntoFine
                SentieroDestinazioneDto(
                    nome = nome,
                    quota = pf?.quota,
                    numeroSentieri = trails.size,
                    coordinate = pf?.coordinate,
                )
            }
            .sortedBy { it.nome }

    /** Verifica che un sentiero soddisfi tutti i criteri del filtro (dato mancante = escluso). */
    private fun matchesFilter(t: SentieroListItemDto, f: RouteFilter): Boolean {
        if (f.difficolta.isNotEmpty() && t.difficolta?.uppercase() !in f.difficolta) return false
        f.distanzaMaxKm?.let { maxKm ->
            val km = (t.lunghezzaPlanimetrica ?: return false) / 1000.0
            if (km > maxKm) return false
        }
        f.dislivelloMax?.let { maxD ->
            val min = t.quotaMinima ?: return false
            val max = t.quotaMassima ?: return false
            if ((max - min).coerceAtLeast(0) > maxD) return false
        }
        f.tempoMaxMin?.let { maxT ->
            val minutes = SentieroMappers.parseTempoToMinutes(t.tempoAndata) ?: return false
            if (minutes > maxT) return false
        }
        return true
    }

    fun onDestinationClick(dest: SentieroDestinazioneDto) {
        // Sentieri della destinazione presi dalla cache (nessuna chiamata di rete).
        val trails = allTrails.filter { it.puntoFine?.nome == dest.nome }
        _uiState.update {
            it.copy(
                step = Step.TrailsForDestination,
                selectedDestination = dest,
                trailsForDestination = trails,
                selectedTrailCode = null,
                selectedTrailDetail = null,
                selectedTrailPolyline = emptyList(),
                error = if (trails.isEmpty()) "Nessun sentiero per questa destinazione." else null,
            )
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

    /** Reset alla chiusura del dialog: torna allo step destinazioni mantenendo cache, filtri e ricerca. */
    fun reset() {
        _uiState.update {
            UiState(destinations = it.destinations, filter = it.filter, searchQuery = it.searchQuery)
        }
    }
}
