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

  /** Dettaglio attività completata (modale sul nav principale). `sessionId` opzionale come query. */
  const val ACTIVITY_DETAIL = "activity_detail/{activityId}?sessionId={sessionId}"

  // ── Sprint 2: Formazione + Quiz ──────────────────────────────────────────────
  const val FORMAZIONE = "formazione"
  const val QUIZ = "quiz/{quizId}"
  /** Entry point "Continua →" dalla FormazioneScreen: il backend risolve il
   *  primo quiz non superato per la categoria indicata. */
  const val QUIZ_FROM_CATEGORY = "quiz_cat/{slug}"
  const val QUIZ_RESULT = "quiz_result"

  // ── Sprint 2: NFC ────────────────────────────────────────────────────────────
  const val NFC_SCAN = "nfc_scan"
  const val NFC_RESULT = "nfc_result"

  // ── Sprint 2: Account ────────────────────────────────────────────────────────
  const val ACCOUNT_EDIT = "account_edit"
  const val CHANGE_PASSWORD = "change_password"
  const val DELETE_ACCOUNT = "delete_account"

  // ── Profilo v2: edit per-sezione ─────────────────────────────────────────────
  const val PERSONAL_INFO_EDIT = "personal_info_edit"
  const val EXPERIENCE_EDIT = "experience_edit"
  const val PREFERENCES_EDIT = "preferences_edit"
  const val GOALS_EDIT = "goals_edit"

  // ── Onboarding v2 (post-registrazione, skippable) ────────────────────────────
  const val ONBOARDING_PERSONAL_INFO = "onboarding/personal_info"
  const val ONBOARDING_EXPERIENCE = "onboarding/experience"
  const val ONBOARDING_PREFERENCES = "onboarding/preferences"

  // ── Social: Challenges ──────────────────────────────────────────────────────
  const val CHALLENGES = "challenges"
  const val CHALLENGE_CREATE = "challenges/create"
  const val CHALLENGE_DETAIL = "challenges/{id}"
  fun challengeDetailRoute(id: String) = "challenges/${Uri.encode(id)}"

  // ── Bacheca: Badge + Certificati ────────────────────────────────────────────
  const val BADGES = "badges"

  fun quizRoute(quizId: String) = "quiz/$quizId"
  fun quizFromCategoryRoute(slug: String) = "quiz_cat/${Uri.encode(slug)}"

  /** Route concreta per il dettaglio sessione (argomento `sessionId`). */
  fun sessionDetailRoute(sessionId: String) = "session_detail/$sessionId"

  /** Route concreta per il dettaglio attività; `sessionId` è opzionale (attività libere = null). */
  fun activityDetailRoute(activityId: String, sessionId: String?): String {
    val base = "activity_detail/${Uri.encode(activityId)}"
    val sid = sessionId?.takeIf { it.isNotBlank() } ?: return base
    return "$base?sessionId=${Uri.encode(sid)}"
  }

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
