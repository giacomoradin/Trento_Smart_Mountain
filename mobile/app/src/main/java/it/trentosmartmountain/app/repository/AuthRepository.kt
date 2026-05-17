package it.trentosmartmountain.app.repository

/** Esito login verso il backend (successo = JWT salvato in [it.trentosmartmountain.app.data.local.TokenStorage] sul dispositivo). */
sealed interface LoginResult {
  data object Success : LoginResult

  /** Account esistente ma email non ancora verificata (`403` da `POST /auth/login`). */
  data class EmailNotVerified(val message: String) : LoginResult

  data class Failure(val message: String) : LoginResult
}

/** Accesso alle API di autenticazione senza esporre Retrofit alla UI. */
fun interface AuthRepository {
  suspend fun login(email: String, password: String): LoginResult
}