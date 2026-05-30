package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.AccountUpdateRequest
import it.trentosmartmountain.app.data.remote.dto.ChangePasswordRequest
import it.trentosmartmountain.app.data.remote.dto.DeleteAccountRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountEditUiState(
    val isLoading: Boolean = false,
    val isLoadingProfile: Boolean = true,
    val currentUsername: String = "",
    val currentEmail: String = "",
    val success: String? = null,
    val error: String? = null,
    val requiresEmailVerification: Boolean = false,
    val accountDeleted: Boolean = false,
)

class AccountEditViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TsmApplication
    private val tokenStorage = app.tokenStorage
    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow(AccountEditUiState())
    val state: StateFlow<AccountEditUiState> = _state.asStateFlow()

    init {
        loadCurrentUser()
    }

    /**
     * Carica i dati attuali da `GET /hikers/:id` per pre-popolare i campi nel form.
     * Se la chiamata fallisce restiamo con stringhe vuote — l'utente può comunque
     * digitare i valori manualmente (il backend rifiuterà richieste vuote tramite Joi).
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val token = tokenStorage.getToken()
            val userId = token?.let { JwtDecoder.userIdFrom(it) }
            if (userId.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoadingProfile = false)
                return@launch
            }
            runCatching { api.getUserById(userId) }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body != null) {
                        _state.value = _state.value.copy(
                            isLoadingProfile = false,
                            currentUsername = body.username.orEmpty(),
                            currentEmail = body.email.orEmpty(),
                        )
                    } else {
                        _state.value = _state.value.copy(isLoadingProfile = false)
                    }
                }
                .onFailure { _state.value = _state.value.copy(isLoadingProfile = false) }
        }
    }

    /**
     * Invia un PATCH /users/me con SOLO i campi effettivamente modificati.
     * Se l'utente non ha cambiato nulla restituisce subito un messaggio amichevole
     * senza chiamare il server (sarebbe rifiutato dal Joi `.min(1)`).
     */
    fun updateAccount(username: String, email: String) {
        viewModelScope.launch {
            val trimmedUsername = username.trim()
            val trimmedEmail = email.trim()
            val changedUsername = trimmedUsername.takeIf { it.isNotBlank() && it != _state.value.currentUsername }
            val changedEmail = trimmedEmail.takeIf { it.isNotBlank() && it != _state.value.currentEmail }

            if (changedUsername == null && changedEmail == null) {
                _state.value = _state.value.copy(success = "Nessuna modifica da salvare.")
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, success = null, error = null)
            runCatching {
                api.updateAccount(AccountUpdateRequest(username = changedUsername, email = changedEmail))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        success = "Profilo aggiornato.",
                        requiresEmailVerification = body?.requiresEmailVerification == true,
                        // Aggiorniamo subito i "valori correnti" così un secondo salvataggio non rispedisce gli stessi campi.
                        currentUsername = changedUsername ?: _state.value.currentUsername,
                        currentEmail = changedEmail ?: _state.value.currentEmail,
                    )
                } else {
                    val msg = when (resp.code()) {
                        409 -> "Email già in uso."
                        422 -> "Dati non validi."
                        else -> "Errore aggiornamento (${resp.code()})."
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message ?: "Errore di rete")
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, success = null, error = null)
            runCatching {
                api.changePassword(ChangePasswordRequest(oldPassword, newPassword))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, success = "Password aggiornata.")
                } else {
                    val msg = if (resp.code() == 401) "Password attuale errata." else "Errore (${resp.code()})."
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message ?: "Errore di rete")
            }
        }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, success = null, error = null)
            runCatching {
                api.deleteAccount(DeleteAccountRequest(password))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, accountDeleted = true)
                } else {
                    val msg = if (resp.code() == 401) "Password errata." else "Errore (${resp.code()})."
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message ?: "Errore di rete")
            }
        }
    }

    fun clearMessages() { _state.value = _state.value.copy(success = null, error = null) }
}
