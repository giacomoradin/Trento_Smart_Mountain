package it.trentosmartmountain.app.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
  val email: String = "",
  val password: String = "",
  val emailError: String? = null,
  val passwordError: String? = null,
  val generalError: String? = null,
  val isLoading: Boolean = false,
)

/**
 * Login email/password: validazione locale e passaggio alla home dopo “successo”.
 * Chiamata REST reale: da collegare quando il backend del team sarà disponibile (POST /auth/login o simile).
 */
class LoginViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
      // TODO: sostituire con AuthRepository / TsmApiService.login(email, password)
      delay(450)
      _uiState.update { it.copy(isLoading = false) }
      navigateHomeEvents.trySend(Unit)
    }
  }
}
