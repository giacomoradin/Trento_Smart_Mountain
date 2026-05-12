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
    return Routes.MAIN
  }
}
