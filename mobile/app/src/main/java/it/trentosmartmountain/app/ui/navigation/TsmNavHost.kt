package it.trentosmartmountain.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.trentosmartmountain.app.ui.screens.auth.AuthEntryScreen
import it.trentosmartmountain.app.ui.screens.login.LoginScreen
import it.trentosmartmountain.app.ui.screens.main.MainScreen
import it.trentosmartmountain.app.ui.screens.register.EmailVerificationPendingScreen
import it.trentosmartmountain.app.ui.screens.register.RegisterScreen

/**
 * Grafo di navigazione Compose: scelta accesso/registrazione → login o registrazione → area principale.
 * Dopo il login si usa [popUpTo] sulla route auth così l’utente non torna alle schermate credenziali col tasto sistema.
 */
@Composable
fun TsmNavHost() {
  val navController = rememberNavController()

  fun navigateToMain() {
    navController.navigate(Routes.MAIN) {
      popUpTo(Routes.AUTH_ENTRY) { inclusive = true }
    }
  }

  fun navigateToAuthEntry() {
    navController.navigate(Routes.AUTH_ENTRY) {
      popUpTo(Routes.MAIN) { inclusive = true }
    }
  }

  NavHost(navController = navController, startDestination = Routes.AUTH_ENTRY) {
    composable(Routes.AUTH_ENTRY) {
      AuthEntryScreen(
        onLoginClick = { navController.navigate(Routes.loginRoute()) },
        onRegisterClick = { navController.navigate(Routes.REGISTER) },
      )
    }
    composable(
      route = Routes.LOGIN,
      arguments =
        listOf(
          navArgument("pendingEmail") {
            type = NavType.StringType
            defaultValue = ""
          },
        ),
    ) { backStackEntry ->
      val pendingEmail = backStackEntry.arguments?.getString("pendingEmail").orEmpty()
      LoginScreen(
        pendingVerificationEmail = pendingEmail,
        onLoggedIn = {
          navigateToMain()
        },
      )
    }
    composable(Routes.REGISTER) {
      RegisterScreen(
        onRegistrationPendingVerification = { email, serverMessage ->
          navController.navigate(Routes.emailVerificationPendingRoute(email, serverMessage)) {
            popUpTo(Routes.REGISTER) { inclusive = true }
          }
        },
        onBack = { navController.popBackStack() },
      )
    }
    composable(
      route = "${Routes.EMAIL_VERIFICATION_PENDING}?serverMessage={serverMessage}",
      arguments =
        listOf(
          navArgument("email") { type = NavType.StringType },
          navArgument("serverMessage") {
            type = NavType.StringType
            defaultValue = ""
          },
        ),
    ) { backStackEntry ->
      val email = backStackEntry.arguments?.getString("email").orEmpty()
      val serverMessage =
        backStackEntry.arguments
          ?.getString("serverMessage")
          ?.takeIf { it.isNotEmpty() }
      EmailVerificationPendingScreen(
        email = email,
        serverMessage = serverMessage,
        onContinueToLogin = {
          navController.navigate(Routes.loginRoute(email)) {
            popUpTo(Routes.AUTH_ENTRY) { inclusive = false }
          }
        },
        onBack = { navController.popBackStack() },
      )
    }
    composable(Routes.MAIN) {
      MainScreen(onLoggedOut = { navigateToAuthEntry() })
    }
  }
}
