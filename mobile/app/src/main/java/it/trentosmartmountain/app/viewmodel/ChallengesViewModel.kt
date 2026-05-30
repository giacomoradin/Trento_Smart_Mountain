package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.Challenge
import it.trentosmartmountain.app.data.remote.dto.ChallengeDetailResponse
import it.trentosmartmountain.app.data.remote.dto.ChallengeRespondRequest
import it.trentosmartmountain.app.data.remote.dto.CreateChallengeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChallengesUiState(
    val isLoading: Boolean = true,
    val items: List<Challenge> = emptyList(),
    val error: String? = null,
    val operationMessage: String? = null,
)

data class ChallengeDetailUiState(
    val isLoading: Boolean = true,
    val detail: ChallengeDetailResponse? = null,
    val error: String? = null,
    val operationMessage: String? = null,
)

/**
 * ViewModel per la lista delle sfide. Carica in init e supporta refresh
 * + create + cancel. Per la detail screen usiamo un VM separato che vive
 * scoped allo step (vedi `ChallengeDetailViewModel` qui sotto).
 */
class ChallengesViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow(ChallengesUiState())
    val state: StateFlow<ChallengesUiState> = _state.asStateFlow()

    init { loadChallenges() }

    fun loadChallenges() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { api.listChallenges() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(isLoading = false, items = resp.body().orEmpty())
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = "Errore (${resp.code()}).")
                    }
                }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun createChallenge(req: CreateChallengeRequest, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { api.createChallenge(req) }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body != null) {
                        _state.value = _state.value.copy(operationMessage = "Sfida creata.")
                        loadChallenges()
                        onCreated(body.id)
                    } else {
                        _state.value = _state.value.copy(error = "Creazione fallita (${resp.code()}).")
                    }
                }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(operationMessage = null, error = null)
    }
}

/**
 * ViewModel della schermata di dettaglio. `load(id)` deve essere chiamato
 * dalla composable in `LaunchedEffect(id)` perché il VM è scoped al graph
 * e non conosce l'id a priori.
 */
class ChallengeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow(ChallengeDetailUiState())
    val state: StateFlow<ChallengeDetailUiState> = _state.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { api.getChallengeDetail(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(isLoading = false, detail = resp.body())
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = "Errore (${resp.code()}).")
                    }
                }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun respond(accept: Boolean) {
        val id = _state.value.detail?.challenge?.id ?: return
        viewModelScope.launch {
            runCatching { api.respondToChallenge(id, ChallengeRespondRequest(accept)) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = _state.value.copy(operationMessage = if (accept) "Invito accettato." else "Invito rifiutato.")
                        load(id)
                    } else {
                        _state.value = _state.value.copy(error = "Errore (${resp.code()}).")
                    }
                }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(operationMessage = null, error = null)
    }
}
