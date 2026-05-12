package it.trentosmartmountain.app.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
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
import it.trentosmartmountain.app.ui.screens.map.MapScreen
import it.trentosmartmountain.app.ui.screens.profile.ProfileScreen
import it.trentosmartmountain.app.ui.screens.session.SessionScreen

private enum class MainTab {
  /** Escursione attiva / avvio nuova sessione. */
  Session,
  /** Mappa di gruppo (placeholder). */
  Map,
  /** Dati account e progressi (fase iniziale: solo username). */
  Profile,
}

/**
 * Area principale post-login con navigazione inferiore tra sessione, mappa e profilo.
 *
 * La tab selezionata resta in [rememberSaveable] anche dopo rotazione schermo.
 */
@Composable
fun MainScreen(
  onLoggedOut: () -> Unit,
) {
  var selectedTab by rememberSaveable { mutableStateOf(MainTab.Session) }

  Scaffold(
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = selectedTab == MainTab.Session,
          onClick = { selectedTab = MainTab.Session },
          icon = { Icon(Icons.Filled.Terrain, contentDescription = null) },
          label = { Text(stringResource(R.string.main_tab_session)) },
        )
        NavigationBarItem(
          selected = selectedTab == MainTab.Map,
          onClick = { selectedTab = MainTab.Map },
          icon = { Icon(Icons.Filled.Map, contentDescription = null) },
          label = { Text(stringResource(R.string.main_tab_map)) },
        )
        NavigationBarItem(
          selected = selectedTab == MainTab.Profile,
          onClick = { selectedTab = MainTab.Profile },
          icon = { Icon(Icons.Filled.Person, contentDescription = null) },
          label = { Text(stringResource(R.string.main_tab_profile)) },
        )
      }
    },
  ) { innerPadding ->
    when (selectedTab) {
      MainTab.Session -> SessionScreen(Modifier.padding(innerPadding))
      MainTab.Map -> MapScreen(Modifier.padding(innerPadding))
      MainTab.Profile -> ProfileScreen(onLoggedOut = onLoggedOut, modifier = Modifier.padding(innerPadding))
    }
  }
}
