package it.trentosmartmountain.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.trentosmartmountain.app.ui.screens.HomePlaceholderScreen
import it.trentosmartmountain.app.ui.screens.login.LoginScreen

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
