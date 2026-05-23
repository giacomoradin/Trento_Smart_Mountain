package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.repository.ProfileObserveState
import it.trentosmartmountain.app.repository.ProfileRepositoryImpl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Stato UI del tab profilo (username + cache / refresh). */
data class ProfileUiState(
  val username: String? = null,
  /** Caricamento iniziale senza dati in cache. */
  val showBlockingLoading: Boolean = true,
  /** Username già noto mentre la rete aggiorna. */
  val showInlineRefresh: Boolean = false,
  val errorMessage: String? = null,
  /** Errore di rete ma abbiamo ancora uno username da Room. */
  val offlineWithCachedProfile: Boolean = false,
)

/**
 * Carica lo username tramite [ProfileRepositoryImpl]: cache Room poi `GET /users/{id}`.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as TsmApplication
  private val tokenStorage = app.tokenStorage

  private val profileRepository =
    ProfileRepositoryImpl(
      TsmApiClient.service(),
      tokenStorage,
      app.database.profileDao(),
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
  }

  /** Ricarica profilo da rete (e aggiorna Room in caso di successo). */
  fun loadProfile() {
    loadSignal.tryEmit(Unit)
  }

  /**
   * Logout locale: JWT e cache profilo Room.
   * [onDone] viene invocato sul main dopo la pulizia (per navigare via dalla UI).
   */
  fun logout(onDone: () -> Unit) {
    viewModelScope.launch {
      tokenStorage.clearToken()
      profileRepository.clearLocalCache()
      // Wipe delle attività locali: evita che un secondo utente sullo stesso
      // device veda lo storico del precedente. Le attività non sincronizzate
      // andranno perse — è il prezzo per la privacy fra utenti.
      app.database.completedActivityDao().deleteAll()
      _uiState.value = ProfileUiState()
      onDone()
    }
  }

  private fun ProfileObserveState.toUiState(): ProfileUiState =
    ProfileUiState(
      username = username,
      showBlockingLoading = isRefreshing && username == null && errorMessage == null,
      showInlineRefresh = isRefreshing && username != null,
      errorMessage = if (!isRefreshing) errorMessage else null,
      offlineWithCachedProfile =
        !isRefreshing && isStale && username != null && errorMessage != null,
    )
}
