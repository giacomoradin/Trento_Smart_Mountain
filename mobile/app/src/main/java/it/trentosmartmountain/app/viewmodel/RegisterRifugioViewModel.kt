package it.trentosmartmountain.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.RegisterRifugioRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Registrazione account rifugio verso API dedicata. */
class RegisterRifugioViewModel : ViewModel() {

    /** Campi del form rifugio e messaggi di errore. */
    data class UiState(
        val rifugioName: String = "",
        val caiCode: String = "",
        val quota: String = "",
        val posti: String = "",
        val coordinates: String = "",
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val rifugioNameError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null,
        val generalError: String? = null,
    )

    data class RegistrationResult(val email: String, val serverMessage: String?)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<RegistrationResult>(Channel.BUFFERED)
    val navigateVerificationEventsFlow = _navigationEvents.receiveAsFlow()

    fun onRifugioNameChange(v: String) = _uiState.update { it.copy(rifugioName = v, rifugioNameError = null) }
    fun onCaiCodeChange(v: String) = _uiState.update { it.copy(caiCode = v) }
    fun onQuotaChange(v: String) = _uiState.update { it.copy(quota = v) }
    fun onPostiChange(v: String) = _uiState.update { it.copy(posti = v) }
    fun onCoordinatesChange(v: String) = _uiState.update { it.copy(coordinates = v) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null) }

    fun onSubmitClick() {
        val state = _uiState.value
        var hasError = false

        if (state.rifugioName.isBlank()) {
            _uiState.update { it.copy(rifugioNameError = "Nome rifugio obbligatorio.") }
            hasError = true
        }
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Email non valida.") }
            hasError = true
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(passwordError = "Minimo 8 caratteri.") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            try {
                // Post-refactor 2026-05: il backend usa discriminator Mongoose,
                // niente più sub-document `rifugioDetails`. Campi flat sul body.
                val body = RegisterRifugioRequest(
                    username = state.rifugioName,
                    email = state.email,
                    password = state.password,
                    rifugioName = state.rifugioName,
                    caiCode = state.caiCode.ifBlank { null },
                    quota = state.quota.toIntOrNull(),
                    posti = state.posti.toIntOrNull(),
                    coordinates = state.coordinates.ifBlank { null },
                )
                val response = TsmApiClient.service().registerRifugio(body)
                if (response.isSuccessful) {
                    val serverMessage = response.body()?.message
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvents.send(RegistrationResult(state.email, serverMessage))
                } else {
                    val errorMsg = when (response.code()) {
                        409 -> "Email o nome già registrati."
                        else -> "Errore server (${response.code()})."
                    }
                    _uiState.update { it.copy(isLoading = false, generalError = errorMsg) }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, generalError = "Nessuna connessione al server.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, generalError = e.message) }
            }
        }
    }
}
