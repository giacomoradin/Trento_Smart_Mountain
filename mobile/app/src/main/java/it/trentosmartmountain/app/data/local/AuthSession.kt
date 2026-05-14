package it.trentosmartmountain.app.data.local

import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.ui.navigation.Routes

/** Risolve la destinazione iniziale del grafo auth in base al JWT salvato sul dispositivo. */
object AuthSession {

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
