package it.trentosmartmountain.app.repository

/** Esito caricamento profilo utente corrente. */
sealed interface ProfileResult {
  data class Success(val username: String) : ProfileResult

  data class Failure(val message: String) : ProfileResult
}

/** Lettura dati profilo senza esporre Retrofit alla UI. */
fun interface ProfileRepository {
  suspend fun loadCurrentUsername(): ProfileResult
}
