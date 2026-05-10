package it.trentosmartmountain.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.trentosmartmountain.app.ui.screens.HomePlaceholderScreen
import it.trentosmartmountain.app.ui.screens.login.LoginScreen

/**
 * Grafo di navigazione Compose: login → home dopo autenticazione.
 * Dopo il login si usa [popUpTo] sulla route login così l’utente non torna indietro alla schermata credenziali col tasto sistema.
 */
@Composable
fun TsmNavHost() {
  val navController = rememberNavController()
  NavHost(navController = navController, startDestination = Routes.LOGIN) {
    composable(Routes.LOGIN) {
      LoginScreen(
        onLoggedIn = {
          navController.navigate(Routes.HOME) {
            popUpTo(Routes.LOGIN) { inclusive = true }
          }
        },
      )
    }
    composable(Routes.HOME) {
      HomePlaceholderScreen()
    }
  }
}
