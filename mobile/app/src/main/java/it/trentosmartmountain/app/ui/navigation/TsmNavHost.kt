package it.trentosmartmountain.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.trentosmartmountain.app.ui.screens.HomePlaceholderScreen
import it.trentosmartmountain.app.ui.screens.auth.AuthEntryScreen
import it.trentosmartmountain.app.ui.screens.login.LoginScreen
import it.trentosmartmountain.app.ui.screens.register.RegisterScreen

/**
 * Grafo di navigazione Compose: scelta accesso/registrazione → login o registrazione → home dopo autenticazione.
 * Dopo il login si usa [popUpTo] sulla route auth così l’utente non torna alle schermate credenziali col tasto sistema.
 */
@Composable
fun TsmNavHost() {
  val navController = rememberNavController()
  NavHost(navController = navController, startDestination = Routes.AUTH_ENTRY) {
    composable(Routes.AUTH_ENTRY) {
      AuthEntryScreen(
        onLoginClick = { navController.navigate(Routes.LOGIN) },
        onRegisterClick = { navController.navigate(Routes.REGISTER) },
      )
    }
    composable(Routes.LOGIN) {
      LoginScreen(
        onLoggedIn = {
          navController.navigate(Routes.HOME) {
            popUpTo(Routes.AUTH_ENTRY) { inclusive = true }
          }
        },
      )
    }
    composable(Routes.REGISTER) {
      RegisterScreen(
        onRegistered = {
          navController.navigate(Routes.LOGIN) {
            popUpTo(Routes.REGISTER) { inclusive = true }
          }
        },
        onBack = { navController.popBackStack() },
      )
    }
    composable(Routes.HOME) {
      HomePlaceholderScreen()
    }
  }
}
