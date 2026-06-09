package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.checklist.ChecklistMapper
import it.trentosmartmountain.app.data.checklist.ChecklistPersonalStore
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.session.SessionLiveController
import it.trentosmartmountain.app.repository.SessionCommandRepository
import it.trentosmartmountain.app.data.session.SessionParticipationResolver
import it.trentosmartmountain.app.data.session.SessionParticipationUi
import it.trentosmartmountain.app.data.session.UserSessionLiveState
import it.trentosmartmountain.app.ui.util.ApiErrorMessages
import it.trentosmartmountain.app.ui.util.SessionDateFormats
import it.trentosmartmountain.app.data.remote.dto.ChecklistDto
import it.trentosmartmountain.app.data.remote.dto.ChecklistGenerateRequest
import it.trentosmartmountain.app.data.remote.dto.ChecklistGetResponse
import it.trentosmartmountain.app.data.remote.dto.ChecklistMutationResponse
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
import java.time.Instant
import java.util.UUID

/**
 * Dettaglio sessione: caricamento da API, modifica capogruppo, checklist dinamica, meteo.
 */
class SessionDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val personalStore = ChecklistPersonalStore
    private val api = TsmApiClient.service()

    companion object {
        const val AUTO_REFRESH_MS = 60 * 60 * 1000L
    }

    private val liveController = SessionLiveController(application)
    var currentUserId: String = ""
        private set

    /** Elemento checklist: item server (dinamico) o personale (solo locale). */
    data class ChecklistItem(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val motivo: String? = null,
        val checked: Boolean = false,
        val isPersonal: Boolean = false,
        val livello: String? = null,
        val categoria: String? = null,
    )

    /** Stato per [it.trentosmartmountain.app.ui.screens.session.SessionDetailScreen]. */
    data class UiState(
        val session: SessionResponse? = null,
        val isLoading: Boolean = false,
        /** Pull-to-refresh: lista già visibile, solo indicatore in cima. */
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val editMode: Boolean = false,
        val checklist: List<ChecklistItem> = emptyList(),
        val checklistLoading: Boolean = false,
        val checklistError: String? = null,
        val checklistUnavailableReason: String? = null,
        val checklistLastUpdate: Long? = null,
        val checklistIsFrozen: Boolean = false,
        val checklistFreezeAtMillis: Long? = null,
        val checklistAcquaLitri: Double? = null,
        val checklistCalorie: Int? = null,
        val checklistCanRegenerate: Boolean = false,
        val checklistMeteoApplied: Boolean = false,
        val checklistMeteoLocationName: String? = null,
        val newItemText: String = "",
        val showAvviaConfirm: Boolean = false,
        /** Dialog di conferma "Chiudi sessione" (force-close del capogruppo). */
        val showCloseSessionConfirm: Boolean = false,
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
        val liveUiEpoch: Int = 0,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun bindCurrentUserId(userId: String) {
        currentUserId = userId
    }

    fun participationUi(): SessionParticipationUi? {
        val session = _uiState.value.session ?: return null
        val isCreator = session.creatorId?._id == currentUserId
        val local = liveController.localState(session._id)
        return SessionParticipationResolver.resolve(session, isCreator, local)
    }

    // ── Gestione partecipanti (Fase A) ──────────────────────────────────────
    // approve/reject: capogruppo o partecipante accettato. remove: solo capogruppo.
    // Il backend impone l'autorizzazione; la UI mostra solo le azioni consentite.
    private fun participantAction(
        failMsg: String,
        call: suspend (String) -> retrofit2.Response<SessionResponse>,
        targetUserId: String,
    ) {
        val sessionId = _uiState.value.session?._id ?: return
        viewModelScope.launch {
            runCatching { call(sessionId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful && resp.body() != null) {
                        _uiState.update {
                            it.copy(session = resp.body(), liveUiEpoch = it.liveUiEpoch + 1)
                        }
                    } else {
                        _uiState.update { it.copy(error = failMsg) }
                    }
                }
                .onFailure { _uiState.update { it.copy(error = "Errore di rete. Riprova.") } }
        }
    }

    fun approveParticipant(targetUserId: String) =
        participantAction("Impossibile approvare la richiesta.", { api.approveParticipant(it, targetUserId) }, targetUserId)

    fun rejectParticipant(targetUserId: String) =
        participantAction("Impossibile rifiutare la richiesta.", { api.rejectParticipant(it, targetUserId) }, targetUserId)

    fun removeParticipant(targetUserId: String) =
        participantAction("Impossibile rimuovere il partecipante.", { api.removeParticipant(it, targetUserId) }, targetUserId)

    fun loadSession(sessionId: String, manualRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                when {
                    manualRefresh && it.session != null ->
                        it.copy(isRefreshing = true, error = null)
                    else -> it.copy(isLoading = true, error = null)
                }
            }
            try {
                val response = api.getSessionById(sessionId)
                if (response.isSuccessful) {
                    val session = response.body()!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            session = session,
                        )
                    }
                    loadMeteo(session)
                    syncChecklist(session, regenerate = false)
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = "Sessione non trovata.")
                    }
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = "Nessuna connessione.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = e.message)
                }
            }
        }
    }

    /** Aggiornamento manuale: il capogruppo rigenera (PUT), i partecipanti sincronizzano (GET). */
    fun refreshChecklistManual() {
        val session = _uiState.value.session ?: return
        val isCreator = session.creatorId?._id == currentUserId
        syncChecklist(session, regenerate = isCreator && _uiState.value.checklistCanRegenerate)
    }

    /** Auto-refresh ogni 60 min: capogruppo rigenera se possibile, altrimenti GET. */
    fun refreshChecklistAuto() {
        val session = _uiState.value.session ?: return
        if (session.status != "PLANNED") return
        val isCreator = session.creatorId?._id == currentUserId
        syncChecklist(session, regenerate = isCreator && _uiState.value.checklistCanRegenerate, silent = true)
    }

    private fun syncChecklist(session: SessionResponse, regenerate: Boolean, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(checklistLoading = true, checklistError = null, checklistUnavailableReason = null) }
            }

            try {
                val body = buildChecklistRequest(session)
                val isCreator = session.creatorId?._id == currentUserId

                if (regenerate) {
                    val putResp = api.updateSessionChecklist(session._id, body)
                    when {
                        putResp.isSuccessful && putResp.body()?.checklist != null ->
                            applyChecklistMutation(session, putResp.body()!!)
                        putResp.code() == 403 -> {
                            val getResp = api.getSessionChecklist(session._id)
                            if (getResp.isSuccessful && getResp.body() != null) {
                                applyChecklistGet(session, getResp.body()!!)
                            } else {
                                _uiState.update {
                                    it.copy(
                                        checklistLoading = false,
                                        checklistError = "Checklist congelata: non è più possibile aggiornarla.",
                                        checklistIsFrozen = true,
                                        checklistCanRegenerate = false,
                                    )
                                }
                            }
                        }
                        else -> _uiState.update {
                            it.copy(
                                checklistLoading = false,
                                checklistError = ApiErrorMessages.fromResponse(putResp),
                            )
                        }
                    }
                    return@launch
                }

                val getResp = api.getSessionChecklist(session._id)
                when {
                    getResp.isSuccessful && getResp.body()?.checklist != null ->
                        applyChecklistGet(session, getResp.body()!!)
                    getResp.code() == 404 && isCreator -> {
                        val postResp = api.generateSessionChecklist(session._id, body)
                        if (postResp.isSuccessful && postResp.body()?.checklist != null) {
                            applyChecklistMutation(session, postResp.body()!!)
                        } else {
                            _uiState.update {
                                it.copy(
                                    checklistLoading = false,
                                    checklistError = ApiErrorMessages.fromResponse(postResp),
                                )
                            }
                        }
                    }
                    getResp.code() == 404 -> _uiState.update {
                        it.copy(
                            checklistLoading = false,
                            checklist = mergeWithPersonal(
                                emptyList(),
                                personalStore.load(getApplication(), session._id, currentUserId),
                                session._id,
                            ),
                            checklistError = "In attesa che il capogruppo generi la checklist.",
                        )
                    }
                    else -> _uiState.update {
                        it.copy(
                            checklistLoading = false,
                            checklistError = ApiErrorMessages.fromResponse(getResp),
                        )
                    }
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        checklistLoading = false,
                        checklistError = if (silent) it.checklistError else "Nessuna connessione al server.",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        checklistLoading = false,
                        checklistError = if (silent) it.checklistError else (e.message ?: "Errore checklist."),
                    )
                }
            }
        }
    }

    private fun applyChecklistMutation(session: SessionResponse, body: ChecklistMutationResponse) {
        applyChecklistDto(
            session = session,
            dto = body.checklist,
            isFrozen = body.checklist.isFrozen,
            freezeAtIso = body.checklist.frozenAt,
        )
    }

    private fun applyChecklistGet(session: SessionResponse, body: ChecklistGetResponse) {
        applyChecklistDto(
            session = session,
            dto = body.checklist,
            isFrozen = body.freeze.isFrozen || body.checklist.isFrozen,
            freezeAtIso = body.freeze.frozenAt ?: body.checklist.frozenAt,
        )
    }

    private fun applyChecklistDto(
        session: SessionResponse,
        dto: ChecklistDto,
        isFrozen: Boolean,
        freezeAtIso: String?,
    ) {
        val personal = personalStore.load(getApplication(), session._id, currentUserId)
        val serverItems = ChecklistMapper.flattenServerItems(dto)
        _uiState.update {
            it.copy(
                checklistLoading = false,
                checklistError = null,
                checklistUnavailableReason = null,
                checklist = mergeWithPersonal(serverItems, personal, session._id),
                checklistLastUpdate = parseIsoMillis(dto.updatedAt ?: dto.generatedAt),
                checklistIsFrozen = isFrozen,
                checklistFreezeAtMillis = parseIsoMillis(freezeAtIso),
                checklistAcquaLitri = dto.acquaLitri,
                checklistCalorie = dto.calorieFabbisogno,
                checklistCanRegenerate = session.creatorId?._id == currentUserId &&
                    session.status == "PLANNED" &&
                    !isFrozen,
                checklistMeteoApplied = !dto.meteoSnapshot?.locationId.isNullOrBlank(),
                checklistMeteoLocationName = dto.meteoSnapshot?.locationName?.takeIf { it.isNotBlank() },
            )
        }
    }

    /** Dopo il caricamento meteo, rigenera la checklist (solo capogruppo) se mancava il forecast. */
    private fun refreshChecklistWithMeteoIfNeeded(session: SessionResponse, forceRegenerate: Boolean = false) {
        val isCreator = session.creatorId?._id == currentUserId
        val meteoReady = !_uiState.value.weatherForecast?.location?.externalId.isNullOrBlank()
        if (!meteoReady || !isCreator || !_uiState.value.checklistCanRegenerate) return
        if (forceRegenerate || !_uiState.value.checklistMeteoApplied) {
            syncChecklist(session, regenerate = true, silent = true)
        }
    }

    private fun buildChecklistRequest(session: SessionResponse): ChecklistGenerateRequest {
        val locationId = _uiState.value.weatherForecast?.location?.externalId
        return ChecklistGenerateRequest(
            sentieroCode = session.sentieroCode,
            locationId = locationId,
            partenza = buildPartenzaIso(session),
        )
    }

    private fun buildPartenzaIso(session: SessionResponse): String? {
        val date = session.meetingDate ?: return null
        val time = session.meetingTime?.trim().orEmpty()
        return if (time.matches(Regex("""\d{1,2}:\d{2}"""))) {
            val parts = time.split(":")
            "${date}T${parts[0].padStart(2, '0')}:${parts[1]}:00Z"
        } else {
            "${date}T08:00:00Z"
        }
    }

    private fun mergeWithPersonal(
        serverItems: List<ChecklistItem>,
        personal: ChecklistPersonalStore.Snapshot,
        sessionId: String,
    ): List<ChecklistItem> = ChecklistMapper.merge(serverItems, personal)

    private fun persistPersonalState(sessionId: String, items: List<ChecklistItem>) {
        if (currentUserId.isBlank()) return
        personalStore.save(
            getApplication(),
            sessionId,
            currentUserId,
            ChecklistMapper.toPersonalSnapshot(items),
        )
    }

    private fun parseIsoMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
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
                val meetingDateApi = SessionDateFormats.toApiOrNull(state.editDate)
                if (state.editDate.isNotBlank() && meetingDateApi == null) {
                    _uiState.update {
                        it.copy(isSaving = false, error = "Data non valida. Selezionala di nuovo dal calendario.")
                    }
                    return@launch
                }
                val body = UpdateSessionRequest(
                    routeDetails = UpdateRouteDetails(name = state.editName, difficultyLevel = state.editDifficulty),
                    meetingDate = meetingDateApi,
                    meetingTime = state.editTime.ifBlank { null },
                    maxParticipants = state.editMaxParticipants,
                )
                
                // 2. Chiamata I/O Server
                val response = api.updateSession(sessionId, body)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = null
                        )
                    }
                    silentReloadSession(sessionId) // Ricarica in background
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = ApiErrorMessages.fromResponse(response),
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
                val response = api.getSessionById(sessionId)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(session = response.body()) }
                }
            } catch (e: Exception) {
                // Il buffer rimane inalterato in caso di drop dei pacchetti
            }
        }
    }

    // --- Checklist ---

    private fun updateChecklistItems(transform: (List<ChecklistItem>) -> List<ChecklistItem>) {
        val sessionId = _uiState.value.session?._id ?: return
        _uiState.update { state ->
            val updated = transform(state.checklist)
            persistPersonalState(sessionId, updated)
            state.copy(checklist = updated)
        }
    }

    fun onToggleCheck(id: String) {
        updateChecklistItems { items ->
            items.map { if (it.id == id) it.copy(checked = !it.checked) else it }
        }
    }

    fun onNewItemTextChange(v: String) = _uiState.update { it.copy(newItemText = v) }

    fun onAddItem() {
        val text = _uiState.value.newItemText.trim()
        if (text.isBlank()) return
        updateChecklistItems { items ->
            items + ChecklistItem(text = text, isPersonal = true)
        }
        _uiState.update { it.copy(newItemText = "") }
    }

    fun onRemoveItem(id: String) {
        updateChecklistItems { items ->
            items.filter { it.id != id || !it.isPersonal }
        }
    }

    /**
     * Sposta l'item dalla posizione [fromIndex] alla posizione [toIndex] (drag-and-drop).
     * Usato da [sh.calvin.reorderable.ReorderableColumn] tramite callback onMove.
     */
    fun onChecklistMove(fromIndex: Int, toIndex: Int) {
        updateChecklistItems { items ->
            val list = items.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                list.add(toIndex, list.removeAt(fromIndex))
            }
            list
        }
    }

    /** Riordina gli item all'interno di un sottoinsieme preservando l'ordine globale delle altre sezioni. */
    fun onChecklistMoveInSubset(subsetIds: List<String>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in subsetIds.indices || toIndex !in subsetIds.indices) return
        updateChecklistItems { all ->
            val idOrder = all.map { it.id }.toMutableList()
            val subsetPositions = subsetIds.mapNotNull { id ->
                idOrder.indexOf(id).takeIf { it >= 0 }
            }
            if (fromIndex !in subsetPositions.indices || toIndex !in subsetPositions.indices) return@updateChecklistItems all
            val globalFrom = subsetPositions[fromIndex]
            val globalTo = subsetPositions[toIndex]
            idOrder.add(globalTo, idOrder.removeAt(globalFrom))
            val byId = all.associateBy { it.id }
            idOrder.mapNotNull { byId[it] }
        }
    }

    // --- Live sessione (stato locale) ---

    fun requestLeaderStart(onNavigate: () -> Unit) {
        val session = _uiState.value.session ?: return
        if (SessionDateFormats.isTodayApi(session.meetingDate)) {
            liveController.leaderStart(session._id)
            onNavigate()
        } else {
            _uiState.update { it.copy(showAvviaConfirm = true) }
        }
    }

    fun confirmLeaderStartEarly(onNavigate: () -> Unit) {
        val session = _uiState.value.session ?: return
        liveController.leaderStart(session._id)
        _uiState.update { it.copy(showAvviaConfirm = false) }
        onNavigate()
    }

    fun dismissAvviaConfirm() = _uiState.update { it.copy(showAvviaConfirm = false) }

    fun leaderStop() {
        val sessionId = _uiState.value.session?._id ?: return
        // 1) Ferma/salva il tracking del leader (dialog Salva/Scarta via coordinator,
        //    se sta tracciando su questo device).
        liveController.leaderStop(viewModelScope, sessionId)
        // 2) ADR-001: "Arresta" del capogruppo CHIUDE la sessione per TUTTI (force).
        //    Vale sempre, anche se un partecipante è ancora live/non concluso (niente
        //    più ghost che bloccano). Non dipende dall'activeSessionId del tab Registra.
        viewModelScope.launch {
            SessionCommandRepository(getApplication()).forceCloseSession(sessionId)
            bumpLiveUi()
            reloadSessionQuiet(sessionId)
        }
    }

    fun joinLive(onNavigate: () -> Unit) {
        val sessionId = _uiState.value.session?._id ?: return
        liveController.joinLive(sessionId)
        bumpLiveUi()
        onNavigate()
    }

    fun leaveLive() {
        val session = _uiState.value.session ?: return
        val local = liveController.localState(session._id)
        if (local == UserSessionLiveState.SOLO_PRACTICE) {
            liveController.endSoloPractice(session._id)
        } else {
            liveController.leaveLive(session._id)
        }
        bumpLiveUi()
    }

    fun startSoloPractice(onNavigate: () -> Unit) {
        val sessionId = _uiState.value.session?._id ?: return
        liveController.startSoloPractice(sessionId)
        bumpLiveUi()
        onNavigate()
    }

    /**
     * "Chiudi sessione" (capogruppo, modello Ibrido): forza COMPLETED per tutti,
     * anche se qualche partecipante non ha ancora concluso. Ricarica il dettaglio.
     */
    fun closeSessionForAll() {
        val sessionId = _uiState.value.session?._id ?: return
        viewModelScope.launch {
            val ok = SessionCommandRepository(getApplication()).forceCloseSession(sessionId)
            if (ok) reloadSessionQuiet(sessionId)
            _uiState.update { it.copy(showCloseSessionConfirm = false) }
        }
    }

    fun requestCloseSession() = _uiState.update { it.copy(showCloseSessionConfirm = true) }
    fun dismissCloseSession() = _uiState.update { it.copy(showCloseSessionConfirm = false) }

    private fun bumpLiveUi() {
        _uiState.update { it.copy(liveUiEpoch = it.liveUiEpoch + 1) }
    }

    private fun reloadSessionQuiet(sessionId: String) {
        viewModelScope.launch {
            runCatching {
                val response = api.getSessionById(sessionId)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(session = response.body()) }
                }
            }
        }
    }

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
                val nearbyResp = api.getWeatherLocationsNearby(
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
                val forecastResp = api.getWeatherForecast(
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
                    refreshChecklistWithMeteoIfNeeded(session, forceRegenerate = forceRefresh)
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
