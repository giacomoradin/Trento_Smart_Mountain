package it.trentosmartmountain.app.data.local

import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.ui.navigation.Routes

/**
 * Risolve la destinazione iniziale del grafo di navigazione in base al JWT locale.
 *
 * Usato all'avvio dell'app (offline-first): se il token è assente o scaduto si apre il flusso auth,
 * altrimenti si salta direttamente alla shell escursionista o rifugio.
 */
object AuthSession {

  /**
   * Route Compose iniziale: [Routes.AUTH_ENTRY] oppure shell principale dopo validazione locale del JWT.
   * Token non validi vengono rimossi da [TokenStorage] prima del redirect.
   */
  fun startDestinationFor(tokenStorage: TokenStorage): String {
    val token = tokenStorage.getToken() ?: return Routes.AUTH_ENTRY
    if (!JwtDecoder.hasValidLocalSession(token)) {
      tokenStorage.clearToken()
      return Routes.AUTH_ENTRY
    }
    return mainShellRouteForToken(token)
  }

  /** Shell principale (escursionista vs rifugio) per navigazione post-login. */
  fun mainShellRouteForToken(token: String): String =
    if (JwtDecoder.roleFrom(token).equals("rifugio", ignoreCase = true)) {
      Routes.MAIN_RIFUGIO
    } else {
      Routes.MAIN_HIKER
    }
}
