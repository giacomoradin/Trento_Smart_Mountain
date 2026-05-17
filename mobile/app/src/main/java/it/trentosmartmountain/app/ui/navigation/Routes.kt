package it.trentosmartmountain.app.ui.navigation

import android.net.Uri

/** Route string usate da Jetpack Navigation ([androidx.navigation.compose.NavHost]). */
object Routes {
  /** Scelta tra login e registrazione. */
  const val AUTH_ENTRY = "auth_entry"
  /** Form di accesso; `pendingEmail` opzionale precompila il campo e mostra un promemoria. */
  const val LOGIN = "login?pendingEmail={pendingEmail}"
  /** Creazione account escursionista / generico. */
  const val REGISTER = "register"
  /** Creazione account rifugio (flusso dedicato, UI in evoluzione). */
  const val REGISTER_RIFUGIO = "register_rifugio"
  /** Istruzioni verifica email dopo `POST /users`. */
  const val EMAIL_VERIFICATION_PENDING = "email_verification_pending/{email}"
  /** Area principale utente escursionista (bottom bar: Home, Sessione, Registra, Profilo). */
  const val MAIN_HIKER = "main_hiker"
  /** Area principale account rifugio (metriche IoT, crediti ospiti). */
  const val MAIN_RIFUGIO = "main_rifugio"
  /** Recupero password via email. */
  const val FORGOT_PASSWORD = "forgot_password"
  /** Dettaglio sessione escursione (modale sul nav principale). */
  const val SESSION_DETAIL = "session_detail/{sessionId}"
  /** Dettaglio attività completata (da "Le Mie Attività"). */
  const val ACTIVITY_DETAIL = "activity_detail/{activityId}?sessionId={sessionId}"

  /** Route concreta per il dettaglio sessione (argomento `sessionId`). */
  fun sessionDetailRoute(sessionId: String) = "session_detail/$sessionId"
  fun activityDetailRoute(activityId: String, sessionId: String? = null): String =
    if (!sessionId.isNullOrBlank()) "activity_detail/$activityId?sessionId=$sessionId"
    else "activity_detail/$activityId"

  /** Route login con email opzionale in query (post verifica account). */
  fun loginRoute(pendingEmail: String = ""): String =
    "login?pendingEmail=${Uri.encode(pendingEmail)}"

  /** Route schermata attesa verifica email, con messaggio server opzionale in query. */
  fun emailVerificationPendingRoute(
    email: String,
    serverMessage: String?,
  ): String {
    val route = "email_verification_pending/${Uri.encode(email)}"
    val message = serverMessage?.trim().orEmpty()
    return if (message.isEmpty()) route else "$route?serverMessage=${Uri.encode(message)}"
  }
}
