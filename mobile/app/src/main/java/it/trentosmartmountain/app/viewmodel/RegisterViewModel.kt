package it.trentosmartmountain.app.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.repository.RegisterResult
import it.trentosmartmountain.app.repository.RegistrationRepositoryImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Stato UI del form registrazione. */
data class RegisterUiState(
  val username: String = "",
  val email: String = "",
  val password: String = "",
  val confirmPassword: String = "",
  val usernameError: String? = null,
  val emailError: String? = null,
  val passwordError: String? = null,
  val confirmPasswordError: String? = null,
  val generalError: String? = null,
  val isLoading: Boolean = false,
)

/**
 * Presentazione del flusso registrazione (MVVM): validazione locale + [RegistrationRepositoryImpl]
 * verso `POST /users`.
 */
class RegisterViewModel : ViewModel() {

  private val registrationRepository =
    RegistrationRepositoryImpl(TsmApiClient.service(), Gson())

  private val _uiState = MutableStateFlow(RegisterUiState())
  val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

  /** Evento “verifica email”: CONFLATED evita navigazioni duplicate se l’utente tocca più volte Invio. */
  private val navigateVerificationEvents = Channel<RegisterResult.Success>(Channel.CONFLATED)
  val navigateVerificationEventsFlow = navigateVerificationEvents.receiveAsFlow()

  fun onUsernameChange(value: String) {
    _uiState.update {
      it.copy(username = value, usernameError = null, generalError = null)
    }
  }

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

  fun onConfirmPasswordChange(value: String) {
    _uiState.update {
      it.copy(confirmPassword = value, confirmPasswordError = null, generalError = null)
    }
  }

  fun onRegisterClick() {
    val username = _uiState.value.username.trim()
    val email = _uiState.value.email.trim()
    val password = _uiState.value.password
    val confirmPassword = _uiState.value.confirmPassword

    // Validazione locale: nessuna chiamata di rete se i campi non sono accettabili.
    val usernameError =
      when {
        username.isEmpty() -> "Inserisci lo username"
        username.length < 3 -> "Minimo 3 caratteri"
        else -> null
      }

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

    val confirmPasswordError =
      when {
        confirmPassword.isEmpty() -> "Conferma la password"
        confirmPassword != password -> "Le password non coincidono"
        else -> null
      }

    if (
      usernameError != null ||
      emailError != null ||
      passwordError != null ||
      confirmPasswordError != null
    ) {
      _uiState.update {
        it.copy(
          usernameError = usernameError,
          emailError = emailError,
          passwordError = passwordError,
          confirmPasswordError = confirmPasswordError,
          generalError = null,
        )
      }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isLoading = true,
          usernameError = null,
          emailError = null,
          passwordError = null,
          confirmPasswordError = null,
          generalError = null,
        )
      }
      when (val result = registrationRepository.register(username, email, password)) {
        is RegisterResult.Success -> {
          _uiState.update { it.copy(isLoading = false) }
          navigateVerificationEvents.trySend(result)
        }
        is RegisterResult.Failure -> {
          _uiState.update {
            it.copy(isLoading = false, generalError = result.message)
          }
        }
      }
    }
  }
}
