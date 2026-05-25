package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.preferences.PreferencesHolder
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.Experience
import it.trentosmartmountain.app.data.remote.dto.GoalsUpdateRequest
import it.trentosmartmountain.app.data.remote.dto.PersonalInfo
import it.trentosmartmountain.app.data.remote.dto.Preferences
import it.trentosmartmountain.app.data.remote.dto.WeeklyGoals
import it.trentosmartmountain.app.data.remote.dto.WeeklyStatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stato del profilo v2 — i tre sub-document personali, più due flag UX.
 *
 *  - `isLoadingProfile`: true al primo load, false dopo `loadProfile`. Le edit
 *    screen usano questo per mostrare uno spinner invece di campi vuoti.
 *  - `profileCompletedAt`: null = onboarding non terminato → ProfileScreen mostra banner.
 */
data class ProfileV2UiState(
    val isLoadingProfile: Boolean = true,
    val isSavingSection: Boolean = false,
    val personalInfo: PersonalInfo? = null,
    val experience: Experience? = null,
    val preferences: Preferences? = null,
    val weeklyGoals: WeeklyGoals? = null,
    val weeklyStats: WeeklyStatsResponse? = null,
    val profileCompletedAt: String? = null,
    val sectionSuccess: String? = null,
    val sectionError: String? = null,
    val onboardingCompletedTrigger: Boolean = false,
)

/**
 * ViewModel dedicato al profilo v2 (personalInfo, experience, preferences, onboarding).
 *
 * Tenuto separato da AccountEditViewModel per evitare di mischiare lo stato
 * username/email/password con quello dei dati personali. Ogni "Edit screen"
 * (PersonalInfoEditScreen, ExperienceEditScreen, ...) e ogni step di onboarding
 * condividono questo VM via `viewModel()` factory di default → al ritorno da
 * un edit il banner in ProfileScreen vede immediatamente i nuovi dati.
 */
class ProfileV2ViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TsmApplication
    private val tokenStorage = app.tokenStorage
    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow(ProfileV2UiState())
    val state: StateFlow<ProfileV2UiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    /** Ricarica il profilo dal server. Chiamato in init e via pull-to-refresh manuale. */
    fun loadProfile() {
        viewModelScope.launch {
            val userId = tokenStorage.getToken()?.let { JwtDecoder.userIdFrom(it) }
            if (userId.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoadingProfile = false)
                return@launch
            }
            _state.value = _state.value.copy(isLoadingProfile = true)
            runCatching { api.getUserById(userId) }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body != null) {
                        _state.value = _state.value.copy(
                            isLoadingProfile = false,
                            personalInfo = body.personalInfo,
                            experience = body.experience,
                            preferences = body.preferences,
                            weeklyGoals = body.weeklyGoals,
                            profileCompletedAt = body.profileCompletedAt,
                        )
                        // F12: sync delle preferenze nel singleton process-wide
                        // così UI come UnitsFormatter / theme reagiscono al cold start.
                        PreferencesHolder.update(
                            units = body.preferences?.units,
                            language = body.preferences?.language,
                        )
                    } else {
                        _state.value = _state.value.copy(isLoadingProfile = false)
                    }
                }
                .onFailure { _state.value = _state.value.copy(isLoadingProfile = false) }
            // Carico in seconda battuta gli stats settimanali. Indipendente dal
            // profilo: se fallisce non azzeriamo i dati personali, restano null.
            loadWeeklyStats()
        }
    }

    /** Carica le stats della settimana corrente. Idempotente, può essere chiamato in pull-to-refresh. */
    fun loadWeeklyStats() {
        viewModelScope.launch {
            runCatching { api.getWeeklyStats() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(weeklyStats = resp.body())
                    }
                }
        }
    }

    /**
     * Auto-marca il profilo come completato dopo il PRIMO save di una sezione.
     * Risolve il "banner persistente" se l'utente compila i campi tramite
     * AccountEditScreen senza mai entrare nel flow di onboarding.
     *
     * Idempotente: chiamate successive sono no-op server-side (vedi
     * markProfileCompleted in accountService.js). Fire-and-forget per non
     * bloccare il messaggio di successo della section save.
     */
    private fun maybeAutoCompleteOnboarding() {
        if (_state.value.profileCompletedAt != null) return
        viewModelScope.launch {
            runCatching { api.markProfileComplete() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(profileCompletedAt = resp.body()?.profileCompletedAt)
                    }
                }
        }
    }

    fun savePersonalInfo(data: PersonalInfo) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingSection = true, sectionError = null, sectionSuccess = null)
            runCatching { api.updatePersonalInfo(data) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            personalInfo = resp.body()?.personalInfo ?: data,
                            sectionSuccess = "Dati personali aggiornati.",
                        )
                        maybeAutoCompleteOnboarding()
                    } else {
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            sectionError = "Errore (${resp.code()}).",
                        )
                    }
                }
                .onFailure { _state.value = _state.value.copy(isSavingSection = false, sectionError = it.message) }
        }
    }

    fun saveExperience(data: Experience) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingSection = true, sectionError = null, sectionSuccess = null)
            runCatching { api.updateExperience(data) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            experience = resp.body()?.experience ?: data,
                            sectionSuccess = "Esperienza aggiornata.",
                        )
                        maybeAutoCompleteOnboarding()
                    } else {
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            sectionError = "Errore (${resp.code()}).",
                        )
                    }
                }
                .onFailure { _state.value = _state.value.copy(isSavingSection = false, sectionError = it.message) }
        }
    }

    fun savePreferences(data: Preferences) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingSection = true, sectionError = null, sectionSuccess = null)
            runCatching { api.updatePreferences(data) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val updated = resp.body()?.preferences ?: data
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            preferences = updated,
                            sectionSuccess = "Preferenze aggiornate.",
                        )
                        // F12: aggiornamento immediato del PreferencesHolder al
                        // save success — la UI ricomposta vede subito i nuovi km/mi.
                        PreferencesHolder.update(
                            units = updated.units,
                            language = updated.language,
                        )
                        maybeAutoCompleteOnboarding()
                    } else {
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            sectionError = "Errore (${resp.code()}).",
                        )
                    }
                }
                .onFailure { _state.value = _state.value.copy(isSavingSection = false, sectionError = it.message) }
        }
    }

    /**
     * Aggiorna gli obiettivi settimanali. Riusa l'endpoint `PATCH /users/me/goals`
     * preesistente — invia solo i campi modificati (Joi `.min(1)` rifiuta body vuoti).
     */
    fun saveGoals(km: Int?, elevM: Int?, count: Int?) {
        viewModelScope.launch {
            // Optimization: invia solo i campi modificati rispetto allo state corrente.
            // Evita di "resettare" un campo che l'utente non ha toccato.
            val current = _state.value.weeklyGoals
            val payload = GoalsUpdateRequest(
                km = km?.takeIf { it != current?.km },
                elevM = elevM?.takeIf { it != current?.elevM },
                count = count?.takeIf { it != current?.count },
            )
            // Se nulla è cambiato, finta success senza chiamare il server (Joi rifiuterebbe).
            if (payload.km == null && payload.elevM == null && payload.count == null) {
                _state.value = _state.value.copy(sectionSuccess = "Nessuna modifica da salvare.")
                return@launch
            }

            _state.value = _state.value.copy(isSavingSection = true, sectionError = null, sectionSuccess = null)
            runCatching { api.updateGoals(payload) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            weeklyGoals = resp.body()?.weeklyGoals ?: _state.value.weeklyGoals,
                            sectionSuccess = "Obiettivi aggiornati.",
                        )
                        maybeAutoCompleteOnboarding()
                    } else {
                        _state.value = _state.value.copy(
                            isSavingSection = false,
                            sectionError = "Errore (${resp.code()}).",
                        )
                    }
                }
                .onFailure { _state.value = _state.value.copy(isSavingSection = false, sectionError = it.message) }
        }
    }

    /**
     * Termina l'onboarding (sia "ho compilato tutto" che "salta tutto").
     * Il flag `onboardingCompletedTrigger` viene osservato dall'onboarding flow per
     * navigare alla shell principale al completamento.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            runCatching { api.markProfileComplete() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(
                            profileCompletedAt = resp.body()?.profileCompletedAt,
                            onboardingCompletedTrigger = true,
                        )
                    }
                }
        }
    }

    fun consumeOnboardingTrigger() {
        _state.value = _state.value.copy(onboardingCompletedTrigger = false)
    }

    fun clearSectionMessages() {
        _state.value = _state.value.copy(sectionSuccess = null, sectionError = null)
    }
}
