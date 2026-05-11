package it.trentosmartmountain.app.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.repository.AuthRepositoryImpl
import it.trentosmartmountain.app.repository.LoginResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Stato UI del form login: campi, messaggi di errore per campo, errore generico, caricamento. */
data class LoginUiState(
  val email: String = "",
  val password: String = "",
  val emailError: String? = null,
  val passwordError: String? = null,
  val generalError: String? = null,
  val isLoading: Boolean = false,
)

/**
 * Presentazione del flusso login (MVVM): validazione locale + [AuthRepositoryImpl] verso `POST /auth/login`.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

  private val authRepository =
    AuthRepositoryImpl(
      TsmApiClient.service(),
      Gson(),
      TokenStorage(application.applicationContext),
    )

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  /** Evento “vai alla home”: CONFLATED evita code di eventi se l’utente tocca più volte il pulsante. */
  private val navigateHomeEvents = Channel<Unit>(Channel.CONFLATED)
  val navigateHomeEventsFlow = navigateHomeEvents.receiveAsFlow()

  fun onEmailChange(value: String) {
    _uiState.update {
      it.copy(email = value, emailError = null, generalError = null)
    }
  }

  fun onPasswordChange(value: String) {
    _uiState.update {
      it.copy(password = value, passwordError = null, generalError = null)
    }
  }

  fun onLoginClick() {
    val email = _uiState.value.email.trim()
    val password = _uiState.value.password

    // Errori di validazione locale: nessuna chiamata di rete se i campi non sono accettabili.
    val emailError =
      when {
        email.isEmpty() -> "Inserisci l’email"
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email non valida"
        else -> null
      }

    val passwordError =
      when {
        password.isEmpty() -> "Inserisci la password"
        password.length < 6 -> "Minimo 6 caratteri"
        else -> null
      }

    if (emailError != null || passwordError != null) {
      _uiState.update {
        it.copy(
          emailError = emailError,
          passwordError = passwordError,
          generalError = null,
        )
      }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isLoading = true,
          emailError = null,
          passwordError = null,
          generalError = null,
        )
      }
      when (val result = authRepository.login(email, password)) {
        is LoginResult.Success -> {
          _uiState.update { it.copy(isLoading = false) }
          navigateHomeEvents.trySend(Unit)
        }
        is LoginResult.Failure -> {
          _uiState.update {
            it.copy(isLoading = false, generalError = result.message)
          }
        }
      }
    }
  }
}
