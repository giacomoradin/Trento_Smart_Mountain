package it.trentosmartmountain.app.repository

import kotlinx.coroutines.flow.Flow

/** Stato osservabile del profilo: cache locale, refresh di rete, errore opzionale.
 *
 * `email` e `isVerified` provengono solo dalla risposta di rete (non cachati su Room)
 * — sono `null` finché il primo refresh non completa. */
data class ProfileObserveState(
  val username: String?,
  val email: String? = null,
  val isVerified: Boolean? = null,
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
