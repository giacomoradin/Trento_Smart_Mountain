package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.repository.ProfileRepositoryImpl
import it.trentosmartmountain.app.repository.ProfileResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Stato UI del tab profilo (username visibile in questa fase). */
data class ProfileUiState(
  val username: String? = null,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
)

/**
 * Carica lo username dell’utente loggato tramite [ProfileRepositoryImpl] (`GET /users/{id}`).
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

  private val profileRepository =
    ProfileRepositoryImpl(
      TsmApiClient.service(),
      TokenStorage(application.applicationContext),
    )

  private val _uiState = MutableStateFlow(ProfileUiState())
  val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

  init {
    loadProfile()
  }

  /** Ricarica il profilo (utile se in futuro si aggiunge pull-to-refresh). */
  fun loadProfile() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      when (val result = profileRepository.loadCurrentUsername()) {
        is ProfileResult.Success -> {
          _uiState.update {
            it.copy(isLoading = false, username = result.username, errorMessage = null)
          }
        }
        is ProfileResult.Failure -> {
          _uiState.update {
            it.copy(isLoading = false, username = null, errorMessage = result.message)
          }
        }
      }
    }
  }
}
