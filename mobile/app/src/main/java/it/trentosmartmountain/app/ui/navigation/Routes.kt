package it.trentosmartmountain.app.ui.navigation

import android.net.Uri

/** Route string usate da Jetpack Navigation ([androidx.navigation.compose.NavHost]). */
object Routes {
  /** Scelta tra login e registrazione. */
  const val AUTH_ENTRY = "auth_entry"
  /** Form di accesso; `pendingEmail` opzionale precompila il campo e mostra un promemoria. */
  const val LOGIN = "login?pendingEmail={pendingEmail}"
  /** Creazione account. */
  const val REGISTER = "register"
  /** Istruzioni verifica email dopo `POST /users`. */
  const val EMAIL_VERIFICATION_PENDING = "email_verification_pending/{email}"
  /** Shell con tab Sessione / Mappa / Profilo. */
  const val MAIN = "main"

  fun loginRoute(pendingEmail: String = ""): String =
    "login?pendingEmail=${Uri.encode(pendingEmail)}"

  fun emailVerificationPendingRoute(
    email: String,
    serverMessage: String?,
  ): String {
    val route = "email_verification_pending/${Uri.encode(email)}"
    val message = serverMessage?.trim().orEmpty()
    return if (message.isEmpty()) route else "$route?serverMessage=${Uri.encode(message)}"
  }
}
