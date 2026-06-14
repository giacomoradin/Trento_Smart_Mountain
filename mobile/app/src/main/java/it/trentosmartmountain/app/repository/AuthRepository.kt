package it.trentosmartmountain.app.repository

/** Esito login verso il backend (successo = JWT salvato in [it.trentosmartmountain.app.data.local.TokenStorage] sul dispositivo). */
sealed interface LoginResult {
  data object Success : LoginResult

  /** Account esistente ma email non ancora verificata (`403` da `POST /auth/login`). */
  data class EmailNotVerified(val message: String) : LoginResult

  /**
   * @param invalidCredentials true per il `401` del server: la UI azzera il
   *  campo password per forzare il re-inserimento manuale (il riempimento
   *  automatico dopo uno switch account può lasciare nel campo mascherato la
   *  password dell'account PRECEDENTE — sembrava un "credenziali non valide"
   *  inspiegabile finché non si ridigitava tutto da zero).
   */
  data class Failure(
    val message: String,
    val invalidCredentials: Boolean = false,
  ) : LoginResult
}

/** Accesso alle API di autenticazione senza esporre Retrofit alla UI. */
fun interface AuthRepository {
  suspend fun login(email: String, password: String): LoginResult
}