package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.gamification.LevelCalculator
import it.trentosmartmountain.app.data.gamification.LevelResult
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.WeeklyGoals
import it.trentosmartmountain.app.repository.ProfileObserveState
import it.trentosmartmountain.app.repository.ProfileRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class ProfileUiState(
    val username: String? = null,
    val email: String? = null,
    val isVerified: Boolean? = null,
    val showBlockingLoading: Boolean = true,
    val showInlineRefresh: Boolean = false,
    val errorMessage: String? = null,
    val offlineWithCachedProfile: Boolean = false,

    // Gamification
    val socialCredits: Int = 0,
    val level: LevelResult? = null,
    val nfcScansCount: Int = 0,
    val nfcScansCredits: Int = 0,

    // Quiz summary
    val totalQuizzes: Int = 0,
    val passedQuizzes: Int = 0,
    val quizCreditsEarned: Int = 0,
    val quizCreditsTotal: Int = 0,

    // Weekly stats
    val totalActivities: Int = 0,
    val totalDistanceKm: Float = 0f,
    val totalElevationM: Int = 0,

    // Goals
    val weeklyGoals: WeeklyGoals? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TsmApplication
    private val tokenStorage = app.tokenStorage
    private val api = TsmApiClient.service()

    private val profileRepository = ProfileRepositoryImpl(
        api, tokenStorage, app.database.profileDao(),
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val loadSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, replay = 1)

    init {
        loadSignal.tryEmit(Unit)
        viewModelScope.launch {
            loadSignal
                .flatMapLatest { profileRepository.observeCurrentProfile() }
                .collect { s -> _uiState.value = s.toUiState() }
        }
        loadExtendedProfile()
    }

    fun loadProfile() {
        loadSignal.tryEmit(Unit)
        loadExtendedProfile()
    }

    /**
     * Carica in parallelo: credits, quiz categories, activity stats.
     * Ogni chiamata ha un `runCatching` separato così un endpoint rotto (es. 500 sul
     * credits service) non azzera le altre sezioni della schermata.
     */
    private fun loadExtendedProfile() {
        viewModelScope.launch {
            val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val creditsDeferred = async { runCatching { api.getMyCredits() } }
            val categoriesDeferred = async { runCatching { api.getQuizCategories() } }
            val statsDeferred = async { runCatching { api.getActivityStats(year) } }

            // Credits → socialCredits + level
            creditsDeferred.await()
                .mapCatching { it.body() ?: error("empty") }
                .onSuccess { credits ->
                    _uiState.value = _uiState.value.copy(
                        socialCredits = credits.total,
                        level = LevelCalculator.compute(credits.total),
                    )
                }

            // Quiz categories → aggregati Formazione card
            categoriesDeferred.await()
                .mapCatching { it.body() ?: error("empty") }
                .onSuccess { categories ->
                    _uiState.value = _uiState.value.copy(
                        totalQuizzes = categories.sumOf { it.totalQuizzes },
                        passedQuizzes = categories.sumOf { it.passedByMe },
                        quizCreditsEarned = categories.sumOf { it.earnedByMe },
                        quizCreditsTotal = categories.sumOf { it.totalCredits },
                    )
                }

            // Activity stats → KPI escursioni/km/dislivello
            statsDeferred.await()
                .mapCatching { it.body() ?: error("empty") }
                .onSuccess { stats ->
                    _uiState.value = _uiState.value.copy(
                        totalActivities = stats.totalActivities,
                        totalDistanceKm = stats.totalDistanceKm.toFloat(),
                        totalElevationM = stats.totalElevationGainM,
                    )
                }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            // Revoca server-side del refresh token (best-effort, PRIMA del clear
            // locale che lo cancellerebbe): senza, il token restava valido 30
            // giorni dopo il logout — incoerenza di sicurezza tra device e server.
            tokenStorage.getRefreshToken()?.takeIf { it.isNotBlank() }?.let { refresh ->
                runCatching {
                    api.logout(
                        it.trentosmartmountain.app.data.remote.dto.LogoutRequest(refreshToken = refresh),
                    )
                }
            }
            tokenStorage.clearToken()
            profileRepository.clearLocalCache()
            app.database.completedActivityDao().deleteAll()
            // F12: reset PreferencesHolder al logout così l'utente successivo
            // (es. login dopo switch account) non eredita le unità altrui.
            it.trentosmartmountain.app.data.preferences.PreferencesHolder.clear()
            // Pulizia cache app + store locali al logout: stato live di sessione,
            // avvisi bacheca nascosti e file temporanei (media storie/cattura, ecc.)
            // stantii hanno causato bug dopo il re-login → li azzeriamo.
            clearAppCacheAndLocalStores()
            _uiState.value = ProfileUiState()
            onDone()
        }
    }

    /** Best-effort: svuota cacheDir + SharedPreferences degli store locali. */
    private fun clearAppCacheAndLocalStores() {
        runCatching { app.cacheDir?.listFiles()?.forEach { it.deleteRecursively() } }
        // Store su SharedPreferences (per nome → non serve l'API del singolo store).
        listOf("tsm_session_live_state", "tsm_board_dismissed").forEach { prefs ->
            runCatching {
                app.getSharedPreferences(prefs, android.content.Context.MODE_PRIVATE)
                    .edit().clear().apply()
            }
        }
    }

    private fun ProfileObserveState.toUiState(): ProfileUiState =
        _uiState.value.copy(
            username = username,
            // Solo se il refresh di rete è andato a buon fine sostituiamo email/isVerified.
            // Durante il refresh manteniamo i valori precedenti per evitare flicker della UI.
            email = email ?: _uiState.value.email,
            isVerified = isVerified ?: _uiState.value.isVerified,
            showBlockingLoading = isRefreshing && username == null && errorMessage == null,
            showInlineRefresh = isRefreshing && username != null,
            errorMessage = if (!isRefreshing) errorMessage else null,
            offlineWithCachedProfile = !isRefreshing && isStale && username != null && errorMessage != null,
        )
}
