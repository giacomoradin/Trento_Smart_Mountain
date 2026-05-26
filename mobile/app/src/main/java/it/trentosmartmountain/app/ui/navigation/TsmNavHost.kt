package it.trentosmartmountain.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.local.AuthSession
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.dto.NfcScanResponse
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionResponse
import it.trentosmartmountain.app.ui.screens.account.AccountEditScreen
import it.trentosmartmountain.app.ui.screens.account.ChangePasswordScreen
import it.trentosmartmountain.app.ui.screens.account.DeleteAccountScreen
import it.trentosmartmountain.app.ui.screens.auth.AuthEntryScreen
import it.trentosmartmountain.app.ui.screens.badges.BadgesScreen
import it.trentosmartmountain.app.ui.screens.challenges.ChallengeDetailScreen
import it.trentosmartmountain.app.ui.screens.challenges.ChallengesScreen
import it.trentosmartmountain.app.ui.screens.challenges.CreateChallengeScreen
import it.trentosmartmountain.app.ui.screens.formazione.FormazioneScreen
import it.trentosmartmountain.app.ui.screens.home.ActivityDetailScreen
import it.trentosmartmountain.app.ui.screens.login.LoginScreen
import it.trentosmartmountain.app.ui.screens.main.HikerMainScreen
import it.trentosmartmountain.app.ui.screens.nfc.NfcResultScreen
import it.trentosmartmountain.app.ui.screens.nfc.NfcScanScreen
import it.trentosmartmountain.app.ui.screens.quiz.QuizResultScreen
import it.trentosmartmountain.app.ui.screens.quiz.QuizScreen
import it.trentosmartmountain.app.ui.screens.refuge.RefugeMainScreen
import it.trentosmartmountain.app.ui.screens.register.EmailVerificationPendingScreen
import it.trentosmartmountain.app.ui.screens.register.ForgotPasswordScreen
import it.trentosmartmountain.app.ui.screens.register.RegisterRifugioScreen
import it.trentosmartmountain.app.ui.screens.register.RegisterScreen
import it.trentosmartmountain.app.ui.screens.onboarding.OnboardingExperienceScreen
import it.trentosmartmountain.app.ui.screens.onboarding.OnboardingPersonalInfoScreen
import it.trentosmartmountain.app.ui.screens.onboarding.OnboardingPreferencesScreen
import it.trentosmartmountain.app.ui.screens.profile.ProfileViewScreen
import it.trentosmartmountain.app.ui.screens.profilev2.ExperienceEditScreen
import it.trentosmartmountain.app.ui.screens.profilev2.GoalsEditScreen
import it.trentosmartmountain.app.ui.screens.profilev2.PersonalInfoEditScreen
import it.trentosmartmountain.app.ui.screens.profilev2.PreferencesEditScreen
import it.trentosmartmountain.app.ui.screens.session.SessionDetailScreen
import it.trentosmartmountain.app.ui.util.gsonSaver

/**
 * Grafo di navigazione principale (Jetpack Navigation Compose).
 *
 * Flussi:
 * - **Auth** (non autenticato): [Routes.AUTH_ENTRY] → login / registrazione escursionista o rifugio
 * - **Shell escursionista** ([Routes.MAIN_HIKER]): bottom bar con tab Home, Sessione, Registra, Profilo — vedi [it.trentosmartmountain.app.ui.screens.main.HikerMainScreen]
 * - **Shell rifugio** ([Routes.MAIN_RIFUGIO]): area dedicata account rifugio
 * - **Dettaglio sessione** ([Routes.SESSION_DETAIL]): schermata full-screen sopra la shell; al conferma AVVIA torna alla shell e la tab Registra può avviare il GPS via [it.trentosmartmountain.app.data.session.SessionStartCoordinator]
 *
 * La destinazione iniziale dipende dal JWT salvato ([it.trentosmartmountain.app.data.local.AuthSession]).
 */
@Composable
fun TsmNavHost() {
    val application = LocalContext.current.applicationContext as TsmApplication
    val startDestination = remember(application) {
        AuthSession.startDestinationFor(application.tokenStorage)
    }
    val navController = rememberNavController()

    // Temporary holders for result objects passed between screens via closure.
    // rememberSaveable + Gson Saver: i risultati sopravvivono a process-death (l'OS può
    // killare l'app in background, l'utente al rientro deve trovare il risultato).
    var pendingNfcResult by rememberSaveable(stateSaver = gsonSaver<NfcScanResponse>()) {
        mutableStateOf<NfcScanResponse?>(null)
    }
    var pendingQuizResult by rememberSaveable(stateSaver = gsonSaver<QuizSubmissionResponse>()) {
        mutableStateOf<QuizSubmissionResponse?>(null)
    }
    var pendingQuizTitle by rememberSaveable { mutableStateOf("") }
    var pendingQuizId by rememberSaveable { mutableStateOf("") }

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
                onNavigateToActivityDetail = { activityId, sessionId ->
                    navController.navigate(Routes.activityDetailRoute(activityId, sessionId))
                },
                onNavigateToFormazione = { navController.navigate(Routes.FORMAZIONE) },
                onNavigateToNfcScan = { navController.navigate(Routes.NFC_SCAN) },
                onNavigateToAccount = { navController.navigate(Routes.ACCOUNT_EDIT) },
                onNavigateToOnboarding = { navController.navigate(Routes.ONBOARDING_PERSONAL_INFO) },
                onNavigateToGoals = { navController.navigate(Routes.GOALS_EDIT) },
                onNavigateToChallenges = { navController.navigate(Routes.CHALLENGES) },
                onNavigateToBadges = { navController.navigate(Routes.BADGES) },
                onNavigateToProfileView = { navController.navigate(Routes.PROFILE_VIEW) },
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
                    // Pop a HikerMainScreen: il VM ha già chiamato SessionStartCoordinator.requestStart,
                    // HikerMainScreen.collect → switch tab Registra, RegistraVM.autoStartFromSession.
                    navController.popBackStack(Routes.MAIN_HIKER, inclusive = false)
                },
            )
        }

        // Dettaglio attività completata — full-screen sopra HikerMainScreen
        composable(
            route = Routes.ACTIVITY_DETAIL,
            arguments = listOf(
                navArgument("activityId") { type = NavType.StringType },
                navArgument("sessionId") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.takeIf { it.isNotBlank() }
            ActivityDetailScreen(
                activityId = activityId,
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
            )
        }

        // ── Sprint 2 routes ──────────────────────────────────────────────────────

        composable(Routes.FORMAZIONE) {
            FormazioneScreen(
                onBack = { navController.popBackStack() },
                onNavigateToQuiz = { slug, _ ->
                    // Il backend risolve il primo quiz non superato per la categoria
                    // (endpoint /quiz/categories/:slug/next). UI behavior del mockup "Continua →".
                    navController.navigate(Routes.quizFromCategoryRoute(slug))
                },
            )
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId").orEmpty()
            QuizScreen(
                quizId = quizId,
                onClose = { navController.popBackStack() },
                onResult = { submission, qId, title ->
                    pendingQuizResult = submission
                    pendingQuizTitle = title
                    pendingQuizId = qId
                    navController.navigate(Routes.QUIZ_RESULT) {
                        popUpTo(Routes.QUIZ) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.QUIZ_FROM_CATEGORY,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug").orEmpty()
            QuizScreen(
                categorySlug = slug,
                onClose = { navController.popBackStack() },
                onResult = { submission, qId, title ->
                    pendingQuizResult = submission
                    pendingQuizTitle = title
                    pendingQuizId = qId
                    navController.navigate(Routes.QUIZ_RESULT) {
                        popUpTo(Routes.QUIZ_FROM_CATEGORY) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.QUIZ_RESULT) {
            val result = pendingQuizResult
            if (result != null) {
                QuizResultScreen(
                    submission = result,
                    quizTitle = pendingQuizTitle,
                    onBackToFormazione = {
                        pendingQuizResult = null
                        navController.navigate(Routes.FORMAZIONE) {
                            popUpTo(Routes.MAIN_HIKER) { inclusive = false }
                        }
                    },
                    onRetry = {
                        pendingQuizResult = null
                        navController.navigate(Routes.quizRoute(pendingQuizId)) {
                            popUpTo(Routes.QUIZ_RESULT) { inclusive = true }
                        }
                    },
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(Routes.NFC_SCAN) {
            NfcScanScreen(
                onBack = { navController.popBackStack() },
                onResult = { response ->
                    pendingNfcResult = response
                    navController.navigate(Routes.NFC_RESULT) {
                        popUpTo(Routes.NFC_SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.NFC_RESULT) {
            val result = pendingNfcResult
            if (result != null) {
                NfcResultScreen(
                    response = result,
                    onBack = {
                        pendingNfcResult = null
                        navController.popBackStack(Routes.MAIN_HIKER, inclusive = false)
                    },
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(Routes.ACCOUNT_EDIT) {
            AccountEditScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onNavigateToDeleteAccount = { navController.navigate(Routes.DELETE_ACCOUNT) },
                onNavigateToPersonalInfo = { navController.navigate(Routes.PERSONAL_INFO_EDIT) },
                onNavigateToExperience = { navController.navigate(Routes.EXPERIENCE_EDIT) },
                onNavigateToPreferences = { navController.navigate(Routes.PREFERENCES_EDIT) },
                onNavigateToGoals = { navController.navigate(Routes.GOALS_EDIT) },
                onAccountDeleted = {
                    application.tokenStorage.clearToken()
                    navigateToAuthEntry()
                },
            )
        }

        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DELETE_ACCOUNT) {
            DeleteAccountScreen(
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    application.tokenStorage.clearToken()
                    navigateToAuthEntry()
                },
            )
        }

        // ── Profilo v2: edit per-sezione ──────────────────────────────────────
        composable(Routes.PERSONAL_INFO_EDIT) {
            PersonalInfoEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPERIENCE_EDIT) {
            ExperienceEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PREFERENCES_EDIT) {
            PreferencesEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GOALS_EDIT) {
            GoalsEditScreen(onBack = { navController.popBackStack() })
        }

        // ── Social: Challenges ─────────────────────────────────────────────
        composable(Routes.CHALLENGES) {
            ChallengesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Routes.CHALLENGE_CREATE) },
                onNavigateToDetail = { id -> navController.navigate(Routes.challengeDetailRoute(id)) },
            )
        }
        composable(Routes.CHALLENGE_CREATE) {
            CreateChallengeScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    // Sostituisce la schermata create con la detail della sfida appena nata,
                    // così il back-button torna alla lista, non al form.
                    navController.navigate(Routes.challengeDetailRoute(id)) {
                        popUpTo(Routes.CHALLENGE_CREATE) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.CHALLENGE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            ChallengeDetailScreen(challengeId = id, onBack = { navController.popBackStack() })
        }

        // ── Bacheca Badges + Certificati ──────────────────────────────────
        composable(Routes.BADGES) {
            BadgesScreen(onBack = { navController.popBackStack() })
        }

        // ── Vista profilo completo (read-only) ────────────────────────────
        composable(Routes.PROFILE_VIEW) {
            ProfileViewScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Routes.ACCOUNT_EDIT) },
            )
        }

        // ── Onboarding v2 (3 step skippable) ─────────────────────────────────
        // Tutti i 3 step condividono lo stesso ProfileV2ViewModel (factory default,
        // scope al graph) → quando l'ultimo step chiama completeOnboarding(), il
        // banner in ProfileScreen sparisce immediatamente (reactive state flow).
        //
        // "Salta tutto" da QUALSIASI step marca onboarding completato (chiamata fatta
        // dallo step stesso prima dell'invocazione di onSkipAll) e torna alla shell —
        // non perdiamo l'utente in un loop. Vedi OnboardingXxxScreen.onSkipAll wiring.
        val popOnboarding: () -> Unit = {
            navController.popBackStack(Routes.ONBOARDING_PERSONAL_INFO, inclusive = true)
        }

        composable(Routes.ONBOARDING_PERSONAL_INFO) {
            OnboardingPersonalInfoScreen(
                onSkipStep = { navController.navigate(Routes.ONBOARDING_EXPERIENCE) },
                onSkipAll = popOnboarding,
                onNext = { navController.navigate(Routes.ONBOARDING_EXPERIENCE) },
            )
        }
        composable(Routes.ONBOARDING_EXPERIENCE) {
            OnboardingExperienceScreen(
                onSkipStep = { navController.navigate(Routes.ONBOARDING_PREFERENCES) },
                onSkipAll = popOnboarding,
                onNext = { navController.navigate(Routes.ONBOARDING_PREFERENCES) },
            )
        }
        composable(Routes.ONBOARDING_PREFERENCES) {
            // Lo step finale: lo screen chiama viewModel.savePreferences + completeOnboarding
            // poi onFinish. Qui ci limitiamo a tornare alla shell.
            OnboardingPreferencesScreen(
                onSkipStep = popOnboarding,
                onSkipAll = popOnboarding,
                onFinish = popOnboarding,
            )
        }
    }
}
