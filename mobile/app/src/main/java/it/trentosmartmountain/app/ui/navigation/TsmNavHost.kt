package it.trentosmartmountain.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.local.AuthSession
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.data.remote.dto.StoryViewerLaunchContext
import it.trentosmartmountain.app.data.remote.dto.NfcScanResponse
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionResponse
import it.trentosmartmountain.app.ui.screens.account.AccountEditScreen
import it.trentosmartmountain.app.ui.screens.account.AccountPasswordGateScreen
import it.trentosmartmountain.app.ui.screens.account.ChangePasswordScreen
import it.trentosmartmountain.app.ui.screens.account.DeleteAccountScreen
import it.trentosmartmountain.app.ui.screens.auth.AuthEntryScreen
import it.trentosmartmountain.app.ui.screens.badges.BadgesScreen
import it.trentosmartmountain.app.ui.screens.board.BoardScreen
import it.trentosmartmountain.app.ui.screens.challenges.ChallengeDetailScreen
import it.trentosmartmountain.app.ui.screens.challenges.ChallengesScreen
import it.trentosmartmountain.app.ui.screens.challenges.CreateChallengeScreen
import it.trentosmartmountain.app.ui.screens.formazione.FormazioneScreen
import it.trentosmartmountain.app.ui.screens.home.ActivityDetailScreen
import it.trentosmartmountain.app.ui.screens.home.FollowListScreen
import it.trentosmartmountain.app.ui.screens.home.LeaderboardScreen
import it.trentosmartmountain.app.ui.screens.home.NotificationsScreen
import it.trentosmartmountain.app.ui.screens.home.PostDetailScreen
import it.trentosmartmountain.app.ui.screens.home.UserSearchScreen
import it.trentosmartmountain.app.viewmodel.FollowListType
import it.trentosmartmountain.app.ui.screens.login.LoginScreen
import it.trentosmartmountain.app.ui.screens.main.HikerMainScreen
import it.trentosmartmountain.app.ui.screens.nfc.NfcResultScreen
import it.trentosmartmountain.app.ui.screens.nfc.NfcScanScreen
import it.trentosmartmountain.app.ui.screens.quiz.QuizResultScreen
import it.trentosmartmountain.app.ui.screens.quiz.QuizScreen
import it.trentosmartmountain.app.ui.screens.refuge.RefugeMainScreen
import it.trentosmartmountain.app.ui.screens.refuge.RefugeProfileScreen
import it.trentosmartmountain.app.ui.screens.refuge.WasteSimulatorScreen
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
private val DarkSurface = Color(0xFF1C1C1E)
private val AccentCyan = Color(0xFF4DD0E1)

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
    var pendingPostDetail by rememberSaveable(stateSaver = gsonSaver<FeedItem>()) {
        mutableStateOf<FeedItem?>(null)
    }
    // Holder per il composer storie (come pendingPostDetail): l'origine setta gli
    // args (tipo + ref + overlay) e naviga a STORY_COMPOSER.
    var pendingStoryDraft by rememberSaveable(
        stateSaver = gsonSaver<it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs>(),
    ) {
        mutableStateOf<it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs?>(null)
    }
    var pendingStoryViewerContext by rememberSaveable(
        stateSaver = gsonSaver<StoryViewerLaunchContext>(),
    ) {
        mutableStateOf<StoryViewerLaunchContext?>(null)
    }

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
                onNavigateToAccount = { navController.navigate(Routes.ACCOUNT_PASSWORD_GATE) },
                onNavigateToOnboarding = { navController.navigate(Routes.ONBOARDING_PERSONAL_INFO) },
                onNavigateToGoals = { navController.navigate(Routes.GOALS_EDIT) },
                onNavigateToChallenges = { navController.navigate(Routes.CHALLENGES) },
                onNavigateToBadges = { navController.navigate(Routes.BADGES) },
                onNavigateToProfileView = { navController.navigate(Routes.PROFILE_VIEW) },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Routes.userProfileRoute(userId))
                },
                onNavigateToStoryViewer = { launch ->
                    launch.startUserId?.let { startId ->
                        pendingStoryViewerContext = launch
                        navController.navigate(Routes.storyViewerRoute(startId))
                    }
                },
                onNavigateToUserSearch = { navController.navigate(Routes.USER_SEARCH) },
                onNavigateToLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToBoard = { navController.navigate(Routes.boardRoute(false)) },
                onNavigateToPostDetail = { item ->
                    pendingPostDetail = item
                    navController.navigate(Routes.POST_DETAIL)
                },
            )
        }

        composable(Routes.POST_DETAIL) {
            val item = pendingPostDetail
            if (item != null) {
                PostDetailScreen(
                    item = item,
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.navigate(Routes.userProfileRoute(userId))
                    }
                )
            } else {
                // Fallback se per qualche motivo lo stato viene perso
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Routes.MAIN_RIFUGIO) {
            RefugeMainScreen(
                onNavigateToProfile = { navController.navigate(Routes.REFUGE_PROFILE) },
                onNavigateToWaste = { navController.navigate(Routes.REFUGE_WASTE) },
            )
        }

        composable(Routes.REFUGE_WASTE) {
            WasteSimulatorScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.REFUGE_PROFILE) {
            RefugeProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBoard = { navController.navigate(Routes.boardRoute(true)) },
                onLoggedOut = { navigateToAuthEntry() },
            )
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
                onUserClick = { uid -> navController.navigate(Routes.userProfileRoute(uid)) },
                onShareStory = { args ->
                    pendingStoryDraft = args
                    navController.navigate(Routes.STORY_COMPOSER)
                },
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
                onUserClick = { uid -> navController.navigate(Routes.userProfileRoute(uid)) },
                onShareStory = { args ->
                    pendingStoryDraft = args
                    navController.navigate(Routes.STORY_COMPOSER)
                },
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
                        navController.navigate(Routes.FORMAZIONE) {
                            popUpTo(Routes.MAIN_HIKER) { inclusive = false }
                        }
                        // Cleanup differito per evitare ricolorazioni bianche durante la transizione
                        pendingQuizResult = null
                    },
                    onRetry = {
                        val qId = pendingQuizId
                        navController.navigate(Routes.quizRoute(qId)) {
                            popUpTo(Routes.QUIZ_RESULT) { inclusive = true }
                        }
                        pendingQuizResult = null
                    },
                )
            } else {
                Box(Modifier.fillMaxSize().background(DarkSurface), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.FORMAZIONE) {
                        popUpTo(Routes.MAIN_HIKER) { inclusive = false }
                    }
                }
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

        composable(Routes.ACCOUNT_PASSWORD_GATE) {
            AccountPasswordGateScreen(
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(Routes.ACCOUNT_EDIT) {
                        popUpTo(Routes.ACCOUNT_PASSWORD_GATE) { inclusive = true }
                    }
                },
            )
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
                onNavigateToEdit = { navController.navigate(Routes.ACCOUNT_PASSWORD_GATE) },
            )
        }

        // ── Profilo pubblico altri utenti (Social, Sprint 2) ──────────────
        // Path arg `userId`. Aperto da FeedCard.tap su avatar e da future
        // entry-point (es. CommentsBottomSheet → autore del commento).
        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()
            it.trentosmartmountain.app.ui.screens.home.UserProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onUserClick = { uid -> navController.navigate(Routes.userProfileRoute(uid)) },
                onOpenFollowList = { uid, type ->
                    val typeArg =
                        if (type == FollowListType.FOLLOWING) "following" else "followers"
                    navController.navigate(Routes.followListRoute(uid, typeArg))
                },
                onOpenDetail = { item ->
                    pendingPostDetail = item
                    navController.navigate(Routes.POST_DETAIL)
                },
            )
        }

        // ── Social: ricerca/scoperta utenti ("aggiungi amici") ───────────
        composable(Routes.USER_SEARCH) {
            UserSearchScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { uid -> navController.navigate(Routes.userProfileRoute(uid)) },
            )
        }

        // ── Social: classifica settimanale ───────────────────────────────
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { uid -> navController.navigate(Routes.userProfileRoute(uid)) },
            )
        }

        // ── Social: centro notifiche ──────────────────────────────────────
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { uid -> navController.navigate(Routes.userProfileRoute(uid)) },
                onOpenActivity = { activityId, sessionId ->
                    navController.navigate(Routes.activityDetailRoute(activityId, sessionId))
                },
                onOpenSession = { sessionId ->
                    navController.navigate(Routes.sessionDetailRoute(sessionId))
                },
            )
        }

        // ── Bacheca rifugi (consultazione utente / gestione rifugista) ────
        composable(
            route = Routes.BOARD,
            arguments = listOf(navArgument("manage") { type = NavType.BoolType; defaultValue = false }),
        ) { backStackEntry ->
            val manage = backStackEntry.arguments?.getBoolean("manage") ?: false
            BoardScreen(manage = manage, onBack = { navController.popBackStack() })
        }

        // ── Social: lista follower/seguiti (navigazione del grafo) ───────
        composable(
            route = Routes.FOLLOW_LIST,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("userId").orEmpty()
            val typeArg = backStackEntry.arguments?.getString("type") ?: "followers"
            val listType =
                if (typeArg == "following") FollowListType.FOLLOWING else FollowListType.FOLLOWERS
            FollowListScreen(
                userId = uid,
                type = listType,
                onBack = { navController.popBackStack() },
                onUserClick = { targetId -> navController.navigate(Routes.userProfileRoute(targetId)) },
            )
        }

        // ── Story viewer full-screen (Instagram-like, storie reali per autore) ──
        composable(
            route = Routes.STORY_VIEWER,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val storyUserId = backStackEntry.arguments?.getString("userId").orEmpty()
            it.trentosmartmountain.app.ui.screens.home.StoryViewerScreen(
                userId = storyUserId,
                launchContext = pendingStoryViewerContext,
                onClose = {
                    pendingStoryViewerContext = null
                    navController.popBackStack()
                },
                onOpenSession = { sid -> navController.navigate(Routes.sessionDetailRoute(sid)) },
            )
        }

        // ── Composer storie (foto/video + overlay) ───────────────────────
        composable(Routes.STORY_COMPOSER) { _ ->
            val draft = pendingStoryDraft
            if (draft != null) {
                it.trentosmartmountain.app.ui.screens.home.StoryComposerScreen(
                    args = draft,
                    onClose = { navController.popBackStack() },
                    onPublished = {
                        pendingStoryDraft = null
                        navController.popBackStack()
                    },
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
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
