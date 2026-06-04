package it.trentosmartmountain.app.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.map
import it.trentosmartmountain.app.viewmodel.RegistraViewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.session.SessionStartCoordinator
import it.trentosmartmountain.app.ui.screens.home.HomeScreen
import it.trentosmartmountain.app.ui.screens.profile.ProfileScreen
import it.trentosmartmountain.app.ui.screens.registra.RegistraScreen
import it.trentosmartmountain.app.ui.screens.session.SessionHubScreen

/** Tab della bottom bar dell'area escursionista. */
private enum class HikerTab { Home, Session, Registra, Profile }

/**
 * Shell principale utente escursionista: navigazione a **tab** (non nested graph).
 *
 * Tab:
 * - Home — feed e attività personali
 * - Sessione — pianifica / unisciti ([it.trentosmartmountain.app.ui.screens.session.SessionHubScreen])
 * - Registra — mappa OSMdroid e tracking GPS
 * - Profilo — dati utente e logout
 *
 * @param onLoggedOut callback dopo logout (navigazione verso auth)
 * @param onNavigateToSessionDetail apre il dettaglio sessione sul grafo root ([it.trentosmartmountain.app.ui.navigation.Routes.SESSION_DETAIL])
 * @param onNavigateToActivityDetail apre il dettaglio attività completata sul grafo root ([it.trentosmartmountain.app.ui.navigation.Routes.ACTIVITY_DETAIL])
 */
@Composable
fun HikerMainScreen(
  onLoggedOut: () -> Unit,
  onNavigateToSessionDetail: (sessionId: String) -> Unit = {},
  onNavigateToActivityDetail: (activityId: String, sessionId: String?) -> Unit = { _, _ -> },
  onNavigateToFormazione: () -> Unit = {},
  onNavigateToNfcScan: () -> Unit = {},
  onNavigateToAccount: () -> Unit = {},
  onNavigateToOnboarding: () -> Unit = {},
  onNavigateToGoals: () -> Unit = {},
  onNavigateToChallenges: () -> Unit = {},
  onNavigateToBadges: () -> Unit = {},
  onNavigateToProfileView: () -> Unit = {},
  // Apertura del profilo pubblico di un altro utente: chiamato dal tap su
  // avatar nel feed Social. Default no-op per backward-compat con i preview.
  onNavigateToUserProfile: (userId: String) -> Unit = {},
  // Tap su anello story della AvatarRow → apre StoryViewerScreen full-screen (per autore).
  onNavigateToStoryViewer: (it.trentosmartmountain.app.data.remote.dto.StoryViewerLaunchContext) -> Unit = {},
  // Tap sulla barra "Trova persone" del feed → apre la ricerca utenti.
  onNavigateToUserSearch: () -> Unit = {},
  // Tap sull'icona trofeo del feed → apre la classifica settimanale.
  onNavigateToLeaderboard: () -> Unit = {},
  // Tap sulla campanella del feed → apre il centro notifiche.
  onNavigateToNotifications: () -> Unit = {},
  // Voce "Bacheca rifugi" nel Profilo → consultazione avvisi/pericoli.
  onNavigateToBoard: () -> Unit = {},
  // Tap su una card del feed → dettaglio social del post.
  onNavigateToPostDetail: (item: it.trentosmartmountain.app.data.remote.dto.FeedItem) -> Unit = {},
) {
  var selectedTab by rememberSaveable { mutableStateOf(HikerTab.Home) }
  // Scope Activity: il polling live/SOS continua anche se l'utente è su un'altra tab.
  val registraViewModel: RegistraViewModel = viewModel()

  LaunchedEffect(Unit) {
    registraViewModel.syncActiveSessionFromServer()
  }

  // Il dialog "Salva attività" vive in RegistraScreen: se una richiesta di stop
  // (anche dal tasto "Arresta" in Unisciti) accende showStopConfirm mentre l'utente
  // è su un'altra tab, portiamolo sulla tab Registra così il dialog è visibile e
  // l'attività non viene persa. Osserviamo solo il booleano derivato per non
  // ricomporre la shell ad ogni tick GPS.
  val stopDialogPending by registraViewModel.uiState
    .map { it.showStopConfirm || it.shortActivityConfirm }
    .collectAsStateWithLifecycle(initialValue = false)
  LaunchedEffect(stopDialogPending) {
    if (stopDialogPending) selectedTab = HikerTab.Registra
  }

  // Quando SessionDetail / SessionHub.AVVIA conferma, il Coordinator emette un sessionId:
  // switchiamo automaticamente alla tab Registra. Il consume() avviene nel VM dopo
  // l'autoStart (vedi RegistraViewModel.init). Usiamo collect su SharedFlow invece
  // di collectAsStateWithLifecycle: i due osservatori (HikerMainScreen + VM) devono
  // ricevere ogni emit in modo indipendente, e StateFlow conflated saltava la transizione.
  LaunchedEffect(Unit) {
    SessionStartCoordinator.pendingSessionStart.collect {
      selectedTab = HikerTab.Registra
      SessionStartCoordinator.consume()
    }
  }

  Scaffold(
    bottomBar = {
      NavigationBar(
        containerColor = it.trentosmartmountain.app.ui.theme.TsmColors.Card,
      ) {
        val navColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
          selectedIconColor = it.trentosmartmountain.app.ui.theme.TsmColors.Primary,
          selectedTextColor = it.trentosmartmountain.app.ui.theme.TsmColors.Primary,
          indicatorColor = it.trentosmartmountain.app.ui.theme.TsmColors.Primary.copy(alpha = 0.16f),
          unselectedIconColor = it.trentosmartmountain.app.ui.theme.TsmColors.TextTertiary,
          unselectedTextColor = it.trentosmartmountain.app.ui.theme.TsmColors.TextTertiary,
        )
        NavigationBarItem(
          selected = selectedTab == HikerTab.Home,
          onClick = { selectedTab = HikerTab.Home },
          icon = { BounceTabIcon(selectedTab == HikerTab.Home, Icons.Filled.Home) },
          label = { Text(stringResource(R.string.main_tab_home)) },
          colors = navColors,
        )
        NavigationBarItem(
          selected = selectedTab == HikerTab.Session,
          onClick = { selectedTab = HikerTab.Session },
          icon = { BounceTabIcon(selectedTab == HikerTab.Session, Icons.Filled.Terrain) },
          label = { Text(stringResource(R.string.main_tab_session)) },
          colors = navColors,
        )
        NavigationBarItem(
          selected = selectedTab == HikerTab.Registra,
          onClick = { selectedTab = HikerTab.Registra },
          icon = { BounceTabIcon(selectedTab == HikerTab.Registra, Icons.Filled.FiberManualRecord) },
          label = { Text(stringResource(R.string.main_tab_registra)) },
          colors = navColors,
        )
        NavigationBarItem(
          selected = selectedTab == HikerTab.Profile,
          onClick = { selectedTab = HikerTab.Profile },
          icon = { BounceTabIcon(selectedTab == HikerTab.Profile, Icons.Filled.Person) },
          label = { Text(stringResource(R.string.main_tab_profile)) },
          colors = navColors,
        )
      }
    },
  ) { innerPadding ->
    when (selectedTab) {
      HikerTab.Home -> HomeScreen(
        modifier = Modifier.padding(innerPadding),
        onActivityClick = onNavigateToActivityDetail,
        onNavigateToUserProfile = onNavigateToUserProfile,
        onNavigateToSessionDetail = onNavigateToSessionDetail,
        onNavigateToStoryViewer = onNavigateToStoryViewer,
        onNavigateToUserSearch = onNavigateToUserSearch,
        onNavigateToLeaderboard = onNavigateToLeaderboard,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToPostDetail = onNavigateToPostDetail,
      )
      HikerTab.Session -> SessionHubScreen(
        modifier = Modifier.padding(innerPadding),
        onNavigateToDetail = onNavigateToSessionDetail,
        onNavigateToBoard = onNavigateToBoard,
        onNavigateToUserProfile = onNavigateToUserProfile,
        onRequestStopTracking = {
          selectedTab = HikerTab.Registra
          registraViewModel.requestStopTracking()
        }
      )
      HikerTab.Registra -> RegistraScreen(
        modifier = Modifier.padding(innerPadding),
        onNavigateToBoard = onNavigateToBoard,
        viewModel = registraViewModel,
      )
      HikerTab.Profile -> ProfileScreen(
        onLoggedOut = onLoggedOut,
        onNavigateToFormazione = onNavigateToFormazione,
        onNavigateToNfcScan = onNavigateToNfcScan,
        onNavigateToAccount = onNavigateToAccount,
        onNavigateToOnboarding = onNavigateToOnboarding,
        onNavigateToGoals = onNavigateToGoals,
        onNavigateToChallenges = onNavigateToChallenges,
        onNavigateToBadges = onNavigateToBadges,
        onNavigateToProfileView = onNavigateToProfileView,
        onNavigateToBoard = onNavigateToBoard,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}


/**
 * Icona della bottom-bar con micro-bounce alla selezione (Fase polish 5): la
 * tab attiva ingrandisce leggermente con uno spring, dando feedback tattile.
 */
@androidx.compose.runtime.Composable
private fun BounceTabIcon(
  selected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
  val scale by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (selected) 1.18f else 1f,
    animationSpec = androidx.compose.animation.core.spring(
      dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
      stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
    ),
    label = "tab-bounce",
  )
  androidx.compose.material3.Icon(
    imageVector = icon,
    contentDescription = null,
    modifier = androidx.compose.ui.Modifier.scale(scale),
  )
}
