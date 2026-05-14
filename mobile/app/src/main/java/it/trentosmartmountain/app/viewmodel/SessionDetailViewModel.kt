package it.trentosmartmountain.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.data.remote.dto.UpdateRouteDetails
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class SessionDetailViewModel : ViewModel() {

    data class ChecklistItem(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val checked: Boolean = false,
    )

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
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val body = UpdateSessionRequest(
                    routeDetails = UpdateRouteDetails(name = state.editName, difficultyLevel = state.editDifficulty),
                    meetingDate = state.editDate.ifBlank { null },
                    meetingTime = state.editTime.ifBlank { null },
                    maxParticipants = state.editMaxParticipants,
                )
                val response = TsmApiClient.service().updateSession(sessionId, body)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSaving = false, editMode = false, session = response.body()) }
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
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

    fun onMoveItemUp(id: String) {
        _uiState.update { state ->
            val list = state.checklist.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx > 0) { val tmp = list[idx]; list[idx] = list[idx - 1]; list[idx - 1] = tmp }
            state.copy(checklist = list)
        }
    }

    fun onMoveItemDown(id: String) {
        _uiState.update { state ->
            val list = state.checklist.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0 && idx < list.size - 1) { val tmp = list[idx]; list[idx] = list[idx + 1]; list[idx + 1] = tmp }
            state.copy(checklist = list)
        }
    }

    // --- AVVIA ---

    fun onAvviaClick(todayFormatted: String) {
        val meetingDate = _uiState.value.session?.meetingDate ?: ""
        if (meetingDate.isBlank() || meetingDate == todayFormatted) {
            // Same day or no date set → proceed directly
            _uiState.update { it.copy(showAvviaConfirm = false) }
        } else {
            // Different day → ask confirmation
            _uiState.update { it.copy(showAvviaConfirm = true) }
        }
    }

    fun dismissAvviaConfirm() = _uiState.update { it.copy(showAvviaConfirm = false) }
}
