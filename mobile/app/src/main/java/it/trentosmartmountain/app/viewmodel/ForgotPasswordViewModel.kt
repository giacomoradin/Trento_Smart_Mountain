package it.trentosmartmountain.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.ForgotPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Recupero password via email. */
class ForgotPasswordViewModel : ViewModel() {

    /** Stato del form forgot password. */
    data class UiState(
        val email: String = "",
        val isLoading: Boolean = false,
        val emailSent: Boolean = false,
        val emailError: String? = null,
        val generalError: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onSendClick() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Inserisci un indirizzo email valido.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            try {
                val response = TsmApiClient.service().forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, emailSent = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, generalError = "Errore. Riprova tra qualche istante.") }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, generalError = "Nessuna connessione. Riprova.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, generalError = e.message) }
            }
        }
    }
}
