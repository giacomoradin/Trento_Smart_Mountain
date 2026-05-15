package it.trentosmartmountain.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.local.AuthSession
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.ui.screens.auth.AuthEntryScreen
import it.trentosmartmountain.app.ui.screens.login.LoginScreen
import it.trentosmartmountain.app.ui.screens.main.HikerMainScreen
import it.trentosmartmountain.app.ui.screens.refuge.RefugeMainScreen
import it.trentosmartmountain.app.ui.screens.register.EmailVerificationPendingScreen
import it.trentosmartmountain.app.ui.screens.register.ForgotPasswordScreen
import it.trentosmartmountain.app.ui.screens.register.RegisterRifugioScreen
import it.trentosmartmountain.app.ui.screens.register.RegisterScreen
import it.trentosmartmountain.app.ui.screens.session.SessionDetailScreen

@Composable
fun TsmNavHost() {
    val application = LocalContext.current.applicationContext as TsmApplication
    val startDestination = remember(application) {
        AuthSession.startDestinationFor(application.tokenStorage)
    }
    val navController = rememberNavController()

    fun navigateToMainAfterLogin() {
        val token = application.tokenStorage.getToken()
        val route = token?.let { AuthSession.mainShellRouteForToken(it) } ?: Routes.MAIN_HIKER
        navController.navigate(route) {
            popUpTo(Routes.AUTH_ENTRY) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToAuthEntry() {
        navController.navigate(Routes.AUTH_ENTRY) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Legge userId dal JWT per il check "isCreator" nella sessione
    val currentUserId = remember(application) {
        application.tokenStorage.getToken()?.let { JwtDecoder.userIdFrom(it) } ?: ""
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.AUTH_ENTRY) {
            AuthEntryScreen(
                onRegisterUserClick = { navController.navigate(Routes.REGISTER) },
                onRegisterRifugioClick = { navController.navigate(Routes.REGISTER_RIFUGIO) },
                onLoginClick = { navController.navigate(Routes.loginRoute()) },
            )
        }

        composable(
            route = Routes.LOGIN,
            arguments = listOf(navArgument("pendingEmail") { type = NavType.StringType; defaultValue = "" }),
        ) { backStackEntry ->
            val pendingEmail = backStackEntry.arguments?.getString("pendingEmail").orEmpty()
            LoginScreen(
                pendingVerificationEmail = pendingEmail,
                onLoggedIn = { navigateToMainAfterLogin() },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
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

        composable(Routes.REGISTER_RIFUGIO) {
            RegisterRifugioScreen(
                onBack = { navController.popBackStack() },
                onRegistrationPendingVerification = { email, serverMessage ->
                    navController.navigate(Routes.emailVerificationPendingRoute(email, serverMessage)) {
                        popUpTo(Routes.REGISTER_RIFUGIO) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.EMAIL_VERIFICATION_PENDING}?serverMessage={serverMessage}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("serverMessage") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            val serverMessage = backStackEntry.arguments?.getString("serverMessage")?.takeIf { it.isNotEmpty() }
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

        composable(Routes.MAIN_HIKER) {
            HikerMainScreen(
                onLoggedOut = { navigateToAuthEntry() },
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate(Routes.sessionDetailRoute(sessionId))
                },
            )
        }

        composable(Routes.MAIN_RIFUGIO) {
            RefugeMainScreen(onLoggedOut = { navigateToAuthEntry() })
        }

        // Dettaglio sessione — navigazione full-screen sopra HikerMainScreen
        composable(
            route = Routes.SESSION_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            SessionDetailScreen(
                sessionId = sessionId,
                currentUserId = currentUserId,
                onBack = { navController.popBackStack() },
                onAvviaConfirmed = { _ ->
                    // Torna a HikerMainScreen (tab Registra sarà avviato dall'utente).
                    // TODO: In futuro, navigare direttamente al tab Registra passando sessionId
                    // quando RegistraScreen sarà completamente implementato con GPS tracking.
                    navController.popBackStack(Routes.MAIN_HIKER, inclusive = false)
                },
            )
        }
    }
}
