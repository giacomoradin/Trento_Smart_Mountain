package it.trentosmartmountain.app.ui.screens.refuge

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.ProfileViewModel
import it.trentosmartmountain.app.viewmodel.RefugeDashboardViewModel

private val Bg = TsmColors.DashboardBackground
private val CardBg = TsmColors.DashboardCard
private val Cyan = TsmColors.Info
private val Green = TsmColors.Online
private val TextSecondary = TsmColors.TextSecondary

/**
 * Scheda profilo del rifugista (adattata dal profilo escursionista): identità
 * della struttura (nome, CAI, quota, posti, email/verificato) + accesso alla
 * gestione bacheca + logout. Dati riusati dall'endpoint dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefugeProfileScreen(
  onBack: () -> Unit,
  onNavigateToBoard: () -> Unit,
  onLoggedOut: () -> Unit,
  dashboardViewModel: RefugeDashboardViewModel = viewModel(
    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
      LocalContext.current.applicationContext as Application,
    ),
  ),
  profileViewModel: ProfileViewModel = viewModel(
    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
      LocalContext.current.applicationContext as Application,
    ),
  ),
) {
  val state by dashboardViewModel.state.collectAsStateWithLifecycle()
  val refuge = state.data?.refuge

  Scaffold(
    containerColor = Bg,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.refuge_profile_title), color = Color.White, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(padding)
        .padding(horizontal = 16.dp),
    ) {
      Spacer(Modifier.height(8.dp))

      // ── Scheda identità ──
      Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = CardBg) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier.size(88.dp).clip(CircleShape).background(Cyan),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              initials(refuge?.name ?: "Rifugio"),
              color = Color(0xFF0D0D0F),
              fontWeight = FontWeight.Bold,
              fontSize = 30.sp,
            )
          }
          Spacer(Modifier.height(12.dp))
          Text(
            refuge?.name ?: "Rifugio",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
          )
          val subtitle = stringResource(R.string.refuge_label) + (refuge?.caiCode?.takeIf { it.isNotBlank() }?.let { " · CAI $it" } ?: "")
          Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
          if (refuge?.verified == true) {
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Green.copy(alpha = 0.15f)) {
              Text(
                stringResource(R.string.refuge_verified),
                color = Green,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          refuge?.email?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
          }

          Spacer(Modifier.height(18.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock(stringResource(R.string.refuge_stat_quota), refuge?.altitudeM?.let { "${"%,d".format(it).replace(",", ".")} m" } ?: "—")
            StatBlock(stringResource(R.string.refuge_stat_posti), refuge?.posti?.toString() ?: "—")
          }
        }
      }

      Spacer(Modifier.height(16.dp))

      // ── La mia bacheca ──
      Surface(
        modifier = Modifier.fillMaxWidth().clickable { onNavigateToBoard() },
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
      ) {
        Row(
          modifier = Modifier.padding(16.dp).fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.Campaign, contentDescription = null, tint = Cyan)
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.refuge_board_menu_title), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.refuge_board_menu_subtitle), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
          }
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
      }

      Spacer(Modifier.height(24.dp))
      OutlinedButton(
        onClick = { profileViewModel.logout(onLoggedOut) },
        modifier = Modifier.fillMaxWidth(),
      ) { Text(stringResource(R.string.action_logout)) }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@Composable
private fun StatBlock(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
  }
}

private fun initials(name: String): String =
  name.trim().split(" ").filter { it.isNotBlank() }.take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "R" }
