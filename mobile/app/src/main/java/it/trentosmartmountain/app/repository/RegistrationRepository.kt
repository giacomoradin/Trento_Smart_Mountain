package it.trentosmartmountain.app.repository

/** Esito registrazione verso `POST /users`. */
sealed interface RegisterResult {
  data object Success : RegisterResult

  data class Failure(val message: String) : RegisterResult
}

/** Creazione account senza esporre Retrofit alla UI. */
fun interface RegistrationRepository {
  suspend fun register(username: String, email: String, password: String): RegisterResult
}
