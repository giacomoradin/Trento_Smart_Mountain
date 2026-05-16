package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.local.db.CompletedActivityEntity
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.ActivityStatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Calendar

enum class ActivitySort {
    MOST_RECENT, OLDEST, ALPHABETICAL, DISTANCE, DIFFICULTY, DURATION
}

class ActivityListViewModel(application: Application) : AndroidViewModel(application) {

    data class ActivityItem(
        val id: String,
        val sessionId: String?,
        val name: String,
        val activityType: String,
        val dateMs: Long,
        val distanceKm: Double,
        val movingSeconds: Long,
        val totalSeconds: Long,
        val elevationGainM: Int,
        val difficultyLevel: String?,
        val points: Int?,
        val estimatedCalories: Int?,
        val isSynced: Boolean,
        val hasLocalTrack: Boolean,
        /** Altitudine profilo (max 50 punti) per la mini-mappa della card. */
        val elevationProfile: List<Double>?,
    )

    data class UiState(
        val activities: List<ActivityItem> = emptyList(),
        val filteredActivities: List<ActivityItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val sort: ActivitySort = ActivitySort.MOST_RECENT,
        val selectedMonth: Int? = null,       // null = tutti i mesi
        val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        val yearlyStats: Map<Int, ActivityStatsResponse> = emptyMap(),
        val statsLoading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val dao get() = (getApplication() as TsmApplication).database.completedActivityDao()

    init {
        // 1. Osserva la tabella Room reattivamente.
        //    IMPORTANTE: il Flow aggiorna solo `activities` (lista master SENZA filtro anno/mese).
        //    `filteredActivities` è calcolata separatamente via `reapplyFilters()` per evitare
        //    la race condition Pager→onYearChanged→reapplyFilters(emptyList) che congelava la UI.
        viewModelScope.launch {
            dao.observeAll().collectLatest { entities ->
                val list = entities.map { it.toItem() }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        activities = list,
                        // Applica i filtri correnti. Se il filtro anno dà lista vuota
                        // (es. attività appena salvata in anno diverso o filtro stale),
                        // mostriamo comunque tutti i risultati senza filtro anno.
                        filteredActivities = applyFiltersWithFallback(list, state.sort, state.selectedMonth, state.selectedYear),
                    )
                }
            }
        }
        // 2. Carica le stats dal backend
        loadStats(Calendar.getInstance().get(Calendar.YEAR))
        // 3. Sync sessioni COMPLETED dal backend → il Flow sopra vedrà i nuovi record
        syncCompletedSessionsToRoom()
    }

    /**
     * Ricarica le attività da Room e aggiorna lo state.
     * Da chiamare quando il tab "Le Mie Attività" diventa visibile (LaunchedEffect in ActivityListScreen).
     */
    fun onTabEntered() {
        viewModelScope.launch {
            val list = dao.getAll().map { it.toItem() }
            _uiState.update { state ->
                state.copy(
                    activities = list,
                    filteredActivities = applyFiltersWithFallback(list, state.sort, state.selectedMonth, state.selectedYear),
                )
            }
        }
    }

    /**
     * Importa in Room le sessioni COMPLETED dal backend che non hanno ancora un record locale.
     * Garantisce che le sessioni a cui si è partecipato (via codice invito o pianificate)
     * appaiano nella lista "Le Mie Attività" anche senza aver usato il tracking locale.
     *
     * Un record creato qui ha [isSynced = true] e [trackLatLng = "[]"] (nessun tracciato locale).
     */
    private fun syncCompletedSessionsToRoom() {
        viewModelScope.launch {
            try {
                val resp = TsmApiClient.service().getMySessions()
                if (!resp.isSuccessful) return@launch
                val sessions = resp.body() ?: return@launch

                sessions
                    .filter { it.status == "COMPLETED" }
                    .forEach { session ->
                        // Salva solo se non esiste già un record con lo stesso sessionId
                        val existing = dao.getBySessionId(session._id)
                        if (existing == null) {
                            val endMs = parseIsoToMs(session.endTime ?: session.createdAt)
                            val distKm = session.gpxStats?.distanceKm ?: 0.0
                            val elevM = session.gpxStats?.elevationGainM ?: 0
                            val movingSec = estimateMovingSeconds(
                                session.gpxStats?.distanceKm,
                                session.gpxStats?.elevationGainM,
                            )
                            val cals = estimateCalories(distKm)
                            val pts = session.gpxStats?.estimatedPoints
                                ?: HikeEstimation.estimatedPoints(distKm, elevM)

                            dao.upsert(
                                it.trentosmartmountain.app.data.local.db.CompletedActivityEntity(
                                    id = session._id,   // usa sessionId come ID per evitare duplicati
                                    sessionId = session._id,
                                    name = session.routeDetails?.name ?: "Escursione",
                                    activityType = "hiking",
                                    startTimeMs = endMs - movingSec * 1000L,
                                    endTimeMs = endMs,
                                    movingSeconds = movingSec,
                                    totalSeconds = movingSec,
                                    distanceMeters = distKm * 1000.0,
                                    elevationGainMeters = elevM,
                                    currentAltitudeMeters = null,
                                    difficultyLevel = session.routeDetails?.difficultyLevel,
                                    trackLatLng = "[]",  // nessun tracciato locale
                                    estimatedCalories = cals,
                                    points = pts,
                                    isSynced = true,
                                    completedAt = endMs,
                                ),
                            )
                        }
                    }
            } catch (_: Exception) {
                // Silenzioso — il caricamento locale funziona comunque
            }
        }
    }

    /** Chiamata esplicita di refresh dal network (usata per pull-to-refresh futuro). */
    fun refreshFromNetwork() {
        syncCompletedSessionsToRoom()
        loadStats(_uiState.value.selectedYear)
    }

    fun loadStats(year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(statsLoading = true) }
            try {
                val resp = TsmApiClient.service().getActivityStats(year)
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { state ->
                        state.copy(
                            statsLoading = false,
                            yearlyStats = state.yearlyStats + (year to resp.body()!!),
                            selectedYear = year,
                        )
                    }
                } else {
                    _uiState.update { it.copy(statsLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(statsLoading = false) }
            }
        }
    }

    fun onYearChanged(year: Int) {
        _uiState.update { it.copy(selectedYear = year, selectedMonth = null) }
        if (!_uiState.value.yearlyStats.containsKey(year)) {
            loadStats(year)
        }
        reapplyFilters()
    }

    fun onSortChanged(sort: ActivitySort) {
        _uiState.update { it.copy(sort = sort) }
        reapplyFilters()
    }

    fun onMonthFilter(month: Int?) {
        _uiState.update { it.copy(selectedMonth = month) }
        reapplyFilters()
    }

    private fun reapplyFilters() {
        _uiState.update {
            it.copy(filteredActivities = applyFiltersWithFallback(it.activities, it.sort, it.selectedMonth, it.selectedYear))
        }
    }

    /**
     * Applica filtri anno/mese + ordinamento.
     * Se il filtro anno dà zero risultati (e la lista non è vuota), restituisce
     * TUTTI gli elementi senza filtro anno — evita che la schermata sembri vuota
     * a causa di una race condition sullo state del Pager.
     */
    private fun applyFiltersWithFallback(
        list: List<ActivityItem>,
        sort: ActivitySort,
        month: Int?,
        year: Int,
    ): List<ActivityItem> {
        val cal = Calendar.getInstance()
        val yearFiltered = list.filter { item ->
            cal.timeInMillis = item.dateMs
            val itemYear = cal.get(Calendar.YEAR)
            val itemMonth = cal.get(Calendar.MONTH)
            val yearMatch = itemYear == year
            val monthMatch = month == null || itemMonth == month
            yearMatch && monthMatch
        }
        // Se il filtro anno ha prodotto una lista vuota ma la lista master non lo è,
        // ignora il filtro anno (es. prima attività salvata, Pager non ancora stabile).
        val filtered = if (yearFiltered.isEmpty() && list.isNotEmpty()) list else yearFiltered
        return when (sort) {
            ActivitySort.MOST_RECENT -> filtered.sortedByDescending { it.dateMs }
            ActivitySort.OLDEST -> filtered.sortedBy { it.dateMs }
            ActivitySort.ALPHABETICAL -> filtered.sortedBy { it.name }
            ActivitySort.DISTANCE -> filtered.sortedByDescending { it.distanceKm }
            ActivitySort.DIFFICULTY -> filtered.sortedByDescending { diffScore(it.difficultyLevel) }
            ActivitySort.DURATION -> filtered.sortedByDescending { it.totalSeconds }
        }
    }

    // ── Helpers ──

    private fun CompletedActivityEntity.toItem() = ActivityItem(
        id = id,
        sessionId = sessionId,
        name = name,
        activityType = activityType,
        dateMs = completedAt,
        distanceKm = distanceMeters / 1000.0,
        movingSeconds = movingSeconds,
        totalSeconds = totalSeconds,
        elevationGainM = elevationGainMeters,
        difficultyLevel = difficultyLevel,
        points = points,
        estimatedCalories = estimatedCalories,
        isSynced = isSynced,
        hasLocalTrack = true,
        elevationProfile = null, // estratto dalla stringa JSON al momento del dettaglio
    )

    private fun diffScore(diff: String?): Int = when (diff) {
        "T" -> 1; "E" -> 2; "EE" -> 3; "EEA" -> 4; else -> 0
    }

    private fun estimateMovingSeconds(distanceKm: Double?, elevationGainM: Int?): Long {
        if (distanceKm == null || distanceKm <= 0) return 0L
        val hours = HikeEstimation.caiTimeHours(distanceKm, elevationGainM ?: 0)
        return (hours * 3600).toLong()
    }

    private fun estimateCalories(distanceKm: Double): Int =
        (70 * distanceKm * 0.85).toInt() // 70kg, METs hiking ~0.85 kcal/kg/km

    private fun parseIsoToMs(iso: String?): Long {
        if (iso == null) return System.currentTimeMillis()
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(iso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    }
}
