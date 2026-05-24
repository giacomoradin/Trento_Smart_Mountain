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
import androidx.compose.ui.res.stringResource
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
) {
  var selectedTab by rememberSaveable { mutableStateOf(HikerTab.Home) }

  // Quando SessionDetail / SessionHub.AVVIA conferma, il Coordinator emette un sessionId:
  // switchiamo automaticamente alla tab Registra. Il consume() avviene nel VM dopo
  // l'autoStart (vedi RegistraViewModel.init). Usiamo collect su SharedFlow invece
  // di collectAsStateWithLifecycle: i due osservatori (HikerMainScreen + VM) devono
  // ricevere ogni emit in modo indipendente, e StateFlow conflated saltava la transizione.
  LaunchedEffect(Unit) {
    SessionStartCoordinator.pendingSessionStart.collect {
      selectedTab = HikerTab.Registra
    }
  }

  Scaffold(
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = selectedTab == HikerTab.Home,
          onClick = { selectedTab = HikerTab.Home },
          icon = { Icon(Icons.Filled.Home, contentDescription = null) },
          label = { Text(stringResource(R.string.main_tab_home)) },
        )
        NavigationBarItem(
          selected = selectedTab == HikerTab.Session,
          onClick = { selectedTab = HikerTab.Session },
          icon = { Icon(Icons.Filled.Terrain, contentDescription = null) },
          label = { Text(stringResource(R.string.main_tab_session)) },
        )
        NavigationBarItem(
          selected = selectedTab == HikerTab.Registra,
          onClick = { selectedTab = HikerTab.Registra },
          icon = { Icon(Icons.Filled.FiberManualRecord, contentDescription = null) },
          label = { Text(stringResource(R.string.main_tab_registra)) },
        )
        NavigationBarItem(
          selected = selectedTab == HikerTab.Profile,
          onClick = { selectedTab = HikerTab.Profile },
          icon = { Icon(Icons.Filled.Person, contentDescription = null) },
          label = { Text(stringResource(R.string.main_tab_profile)) },
        )
      }
    },
  ) { innerPadding ->
    when (selectedTab) {
      HikerTab.Home -> HomeScreen(
        modifier = Modifier.padding(innerPadding),
        onActivityClick = onNavigateToActivityDetail,
      )
      HikerTab.Session -> SessionHubScreen(
        modifier = Modifier.padding(innerPadding),
        onNavigateToDetail = onNavigateToSessionDetail,
      )
      HikerTab.Registra -> RegistraScreen(Modifier.padding(innerPadding))
      HikerTab.Profile -> ProfileScreen(onLoggedOut = onLoggedOut, modifier = Modifier.padding(innerPadding))
    }
  }
}
