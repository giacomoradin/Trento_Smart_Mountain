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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.screens.home.HomeScreen
import it.trentosmartmountain.app.ui.screens.profile.ProfileScreen
import it.trentosmartmountain.app.ui.screens.registra.RegistraScreen
import it.trentosmartmountain.app.ui.screens.session.SessionHubScreen

private enum class HikerTab { Home, Session, Registra, Profile }

@Composable
fun HikerMainScreen(
  onLoggedOut: () -> Unit,
  onNavigateToSessionDetail: (sessionId: String) -> Unit = {},
) {
  var selectedTab by rememberSaveable { mutableStateOf(HikerTab.Home) }

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
      HikerTab.Home -> HomeScreen(Modifier.padding(innerPadding))
      HikerTab.Session -> SessionHubScreen(
        modifier = Modifier.padding(innerPadding),
        onNavigateToDetail = onNavigateToSessionDetail,
      )
      HikerTab.Registra -> RegistraScreen(Modifier.padding(innerPadding))
      HikerTab.Profile -> ProfileScreen(onLoggedOut = onLoggedOut, modifier = Modifier.padding(innerPadding))
    }
  }
}
