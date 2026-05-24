package it.trentosmartmountain.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.data.remote.dto.UpdateRouteDetails
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionRequest
import it.trentosmartmountain.app.data.remote.dto.WeatherForecastResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

/**
 * Dettaglio sessione: caricamento da API, modifica capogruppo, checklist locale, meteo.
 */
class SessionDetailViewModel : ViewModel() {

    /** Elemento della checklist escursionistica (stato solo locale, non ancora persistito). */
    data class ChecklistItem(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val checked: Boolean = false,
    )

    /** Stato per [it.trentosmartmountain.app.ui.screens.session.SessionDetailScreen]. */
    data class UiState(
        val session: SessionResponse? = null,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val editMode: Boolean = false,
        val checklist: List<ChecklistItem> = emptyList(),
        val newItemText: String = "",
        val showAvviaConfirm: Boolean = false,
        val error: String? = null,
        // Edit fields (populated when entering edit mode)
        val editName: String = "",
        val editDate: String = "",
        val editTime: String = "",
        val editMaxParticipants: Int = 8,
        val editDifficulty: String = "E",
        // Meteo (dati reali da meteo.report/TINIA via backend di Marco)
        val weatherForecast: WeatherForecastResponse? = null,
        val meteoLoading: Boolean = false,
        val meteoError: String? = null,
        val meteoLastUpdate: Long? = null, // System.currentTimeMillis()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = TsmApiClient.service().getSessionById(sessionId)
                if (response.isSuccessful) {
                    val session = response.body()!!
                    /*
                     * CHECKLIST AUTO-GENERATION — DA IMPLEMENTARE
                     *
                     * Obiettivo (RF3): generare checklist dinamica in base al percorso.
                     *
                     * Input disponibili:
                     *   - session.routeDetails.difficultyLevel (T/E/EE/EEA)
                     *   - session.gpxStats.distanceKm
                     *   - session.gpxStats.elevationGainM
                     *   - session.meetingDate → meteo previsto (da API MeteoTrentino)
                     *
                     * Algoritmo suggerito (backend: GET /api/v1/sessions/{id}/checklist):
                     *   1. Base comune: acqua (1L/3h), kit primo soccorso, giacca emergency
                     *   2. Meteo: pioggia → impermeabile; T < 5° → guanti+cappello; vento > 30km/h → occhiali
                     *   3. Difficoltà:
                     *      T/E → scarpe da trekking leggere
                     *      EE  → scarponi alti + bastoncini + caschetto
                     *      EEA → set corde + ramponi + piccozza
                     *   4. Distanza > 15km o dislivello > 1000m → snack extra, pila frontale
                     *   5. Validazione partecipante: confronta item con profilo utente (saldoSc, badge)
                     *
                     * Per ora: checklist statica di default con possibilità di modifica manuale.
                     */
                    val defaultChecklist = buildDefaultChecklist(session)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = session,
                            checklist = defaultChecklist,
                        )
                    }
                    // Trigger fetch meteo per la stazione più vicina al punto di partenza
                    loadMeteo(session)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Sessione non trovata.") }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Nessuna connessione.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun buildDefaultChecklist(session: SessionResponse): List<ChecklistItem> {
        val items = mutableListOf(
            ChecklistItem(text = "Acqua (almeno 1.5L)"),
            ChecklistItem(text = "Kit primo soccorso"),
            ChecklistItem(text = "Giacca antipioggia"),
        )
        val diff = session.routeDetails?.difficultyLevel ?: "E"
        when (diff) {
            "T", "E" -> items.add(ChecklistItem(text = "Scarpe da trekking"))
            "EE" -> {
                items.addAll(listOf(
                    ChecklistItem(text = "Scarponi alti"),
                    ChecklistItem(text = "Bastoncini da trekking"),
                    ChecklistItem(text = "Caschetto"),
                ))
            }
            "EEA" -> {
                items.addAll(listOf(
                    ChecklistItem(text = "Scarponi alti"),
                    ChecklistItem(text = "Imbragatura + corde"),
                    ChecklistItem(text = "Piccozza"),
                ))
            }
        }
        val dist = session.gpxStats?.distanceKm ?: 0.0
        if (dist > 15.0) items.add(ChecklistItem(text = "Snack extra"))
        items.add(ChecklistItem(text = "Pila frontale"))
        items.add(ChecklistItem(text = "Crema solare"))
        return items
    }

    // --- Edit mode ---

    fun enterEditMode() {
        val s = _uiState.value.session ?: return
        _uiState.update {
            it.copy(
                editMode = true,
                editName = s.routeDetails?.name ?: "",
                editDate = s.meetingDate ?: "",
                editTime = s.meetingTime ?: "",
                editMaxParticipants = s.maxParticipants ?: 8,
                editDifficulty = s.routeDetails?.difficultyLevel ?: "E",
            )
        }
    }

    fun exitEditMode() = _uiState.update { it.copy(editMode = false) }

    fun onEditNameChange(v: String) = _uiState.update { it.copy(editName = v) }
    fun onEditDateChange(v: String) = _uiState.update { it.copy(editDate = v) }
    fun onEditTimeChange(v: String) = _uiState.update { it.copy(editTime = v) }
    fun onEditMaxParticipantsChange(v: Int) = _uiState.update { it.copy(editMaxParticipants = v.coerceIn(1, 50)) }
    fun onEditDifficultyChange(v: String) = _uiState.update { it.copy(editDifficulty = v) }

    fun saveEdit() {
        val state = _uiState.value
        val sessionId = state.session?._id ?: return

        // 1. ISOLAMENTO UI: Forza la distruzione del nodo visivo incondizionatamente
        _uiState.update { it.copy(isSaving = true, editMode = false) }

        viewModelScope.launch {
            try {
                val body = UpdateSessionRequest(
                    routeDetails = UpdateRouteDetails(name = state.editName, difficultyLevel = state.editDifficulty),
                    meetingDate = state.editDate.ifBlank { null },
                    meetingTime = state.editTime.ifBlank { null },
                    maxParticipants = state.editMaxParticipants,
                )
                
                // 2. Chiamata I/O Server
                val response = TsmApiClient.service().updateSession(sessionId, body)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = null
                        )
                    }
                    silentReloadSession(sessionId) // Ricarica in background
                } else {
                    // 3. SEGNALAZIONE FALLIMENTO (Senza riaprire la tendina)
                    val errorBody = response.errorBody()?.string() ?: "Errore Sconosciuto"
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            // Lasciamo editMode = false per confermare che l'input UI funziona.
                            error = "HTTP ${response.code()}: $errorBody" 
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Network Fault: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Modulo di sincronizzazione background.
     * Recupera il documento aggiornato dal server senza scatenare
     * il flag `isLoading` principale, garantendo stabilità grafica.
     */
    private fun silentReloadSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val response = TsmApiClient.service().getSessionById(sessionId)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(session = response.body()) }
                }
            } catch (e: Exception) {
                // Il buffer rimane inalterato in caso di drop dei pacchetti
            }
        }
    }

    // --- Checklist ---

    fun onToggleCheck(id: String) {
        _uiState.update { state ->
            state.copy(checklist = state.checklist.map { if (it.id == id) it.copy(checked = !it.checked) else it })
        }
    }

    fun onNewItemTextChange(v: String) = _uiState.update { it.copy(newItemText = v) }

    fun onAddItem() {
        val text = _uiState.value.newItemText.trim()
        if (text.isBlank()) return
        _uiState.update { it.copy(checklist = it.checklist + ChecklistItem(text = text), newItemText = "") }
    }

    fun onRemoveItem(id: String) {
        _uiState.update { it.copy(checklist = it.checklist.filter { item -> item.id != id }) }
    }

    /**
     * Sposta l'item dalla posizione [fromIndex] alla posizione [toIndex] (drag-and-drop).
     * Usato da [sh.calvin.reorderable.ReorderableColumn] tramite callback onMove.
     */
    fun onChecklistMove(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.checklist.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                list.add(toIndex, list.removeAt(fromIndex))
            }
            state.copy(checklist = list)
        }
    }

    // --- AVVIA ---

    fun onAvviaClick(todayFormatted: String, onNavigate: () -> Unit) {
        val meetingDate = _uiState.value.session?.meetingDate ?: ""
        if (meetingDate.isBlank() || meetingDate == todayFormatted) {
            onNavigate()
        } else {
            _uiState.update { it.copy(showAvviaConfirm = true) }
        }
    }

    fun dismissAvviaConfirm() = _uiState.update { it.copy(showAvviaConfirm = false) }

    // ── METEO (API meteo.report/TINIA via backend di Marco) ──

    /**
     * Carica le previsioni meteo per il punto di partenza della sessione.
     *
     * Flow:
     *   1. Estrae le coordinate dal GPX startPoint (GeoJSON [lon, lat])
     *   2. GET /weather/locations/nearby?lon=&lat=&type=town&limit=1 → town più vicina
     *   3. GET /weather/forecast/:externalId → slots3h (prossime 48h) + slots24h (7 giorni)
     *
     * Se il DB non ha stazioni nel raggio richiesto, la MeteoCard mostra un messaggio
     * di errore con bottone Riprova (DB è auto-seedato all'avvio backend con 601 towns
     * + 108 POI, vedi weatherService.seedLocations).
     *
     * Cache server-side: 1h. Il refresh manuale dalla UI chiama con forceRefresh=true.
     *
     * Enhancements futuri (Sprint 3):
     *   - Polling ogni 5 min con LaunchedEffect + delay(5 * 60_000L) per sessione attiva
     *   - Storico giorni precedenti via slot24h con filtro su validFrom
     *   - Room cache offline per le sessioni già visualizzate
     */
    fun loadMeteo(session: SessionResponse, forceRefresh: Boolean = false) {
        // GeoJSON coordinates = [lon, lat]
        val coords = session.routeDetails?.startPoint?.coordinates
        val lon = coords?.getOrNull(0)
        val lat = coords?.getOrNull(1)

        if (lon == null || lat == null || (lon == 0.0 && lat == 0.0)) {
            _uiState.update { it.copy(meteoLoading = false, meteoError = "Nessun punto GPS per il meteo.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(meteoLoading = true, meteoError = null) }
            try {
                // 1. Trova la town più vicina
                val nearbyResp = TsmApiClient.service().getWeatherLocationsNearby(
                    lon = lon,
                    lat = lat,
                    type = "town",
                    limit = 1,
                    maxDistance = 100_000, // 100 km — copre tutta la regione
                )
                if (!nearbyResp.isSuccessful || nearbyResp.body()?.results.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            meteoLoading = false,
                            meteoError = "Nessuna stazione meteo trovata nelle vicinanze (${nearbyResp.code()}).",
                        )
                    }
                    return@launch
                }
                val town = nearbyResp.body()!!.results.first()

                // 2. Recupera il forecast completo (cache 1h server-side)
                val forecastResp = TsmApiClient.service().getWeatherForecast(
                    externalId = town.externalId,
                    forceRefresh = if (forceRefresh) true else null,
                )
                if (forecastResp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            meteoLoading = false,
                            weatherForecast = forecastResp.body(),
                            meteoLastUpdate = System.currentTimeMillis(),
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            meteoLoading = false,
                            meteoError = "Errore forecast (${forecastResp.code()}). Il DB potrebbe non essere seedato.",
                        )
                    }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(meteoLoading = false, meteoError = "Nessuna connessione al server.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(meteoLoading = false, meteoError = "Errore meteo: ${e.javaClass.simpleName}") }
            }
        }
    }

    fun refreshMeteo() {
        _uiState.value.session?.let { loadMeteo(it, forceRefresh = true) }
    }
}
