package it.trentosmartmountain.app.repository

import kotlinx.coroutines.flow.Flow

/** Stato osservabile del profilo: cache locale, refresh di rete, errore opzionale. */
data class ProfileObserveState(
  val username: String?,
  val isRefreshing: Boolean,
  /** `true` se l’username mostrato proviene da cache non ancora confermata dal server. */
  val isStale: Boolean,
  val errorMessage: String?,
)

/** Lettura profilo con cache Room e aggiornamento da rete. */
interface ProfileRepository {
  fun observeCurrentProfile(): Flow<ProfileObserveState>

  /** Logout o reset privacy: svuota la tabella profilo locale. */
  suspend fun clearLocalCache()
}
