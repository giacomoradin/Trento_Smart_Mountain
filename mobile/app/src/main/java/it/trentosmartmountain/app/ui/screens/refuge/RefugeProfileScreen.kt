package it.trentosmartmountain.app.ui.screens.refuge

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.RefugeProfileUpdateRequest
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmGlow
import it.trentosmartmountain.app.ui.util.AvatarUtils
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.ProfileViewModel
import it.trentosmartmountain.app.viewmodel.RefugeDashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = TsmColors.DashboardBackground
private val CardBg = TsmColors.DashboardCard
private val Cyan = TsmColors.Info
private val Green = TsmColors.Online
private val TextSecondary = TsmColors.TextSecondary

/**
 * Scheda profilo del rifugista: identità della struttura (FOTO personalizzabile,
 * nome, CAI, quota, posti, email/verificato), gestione bacheca, impostazioni
 * (cambio password) e logout. Dati riusati dall'endpoint dashboard; la foto
 * viene caricata via `PATCH /api/v1/refuge/profile` (data URI Base64, stessa
 * pipeline di compressione dell'avatar escursionista).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefugeProfileScreen(
  onBack: () -> Unit,
  onNavigateToBoard: () -> Unit,
  onLoggedOut: () -> Unit,
  onNavigateToChangePassword: () -> Unit = {},
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
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Override locale post-upload: mostra subito la nuova foto senza attendere
  // il re-fetch della dashboard. null = usa il valore della dashboard.
  var localAvatarOverride by remember { mutableStateOf<String?>(null) }
  var avatarUploading by remember { mutableStateOf(false) }
  val shownAvatar = localAvatarOverride ?: refuge?.avatarUrl

  fun uploadAvatar(dataUri: String?) {
    scope.launch {
      avatarUploading = true
      val resp = runCatching {
        TsmApiClient.service().updateRefugeProfile(RefugeProfileUpdateRequest(avatarUrl = dataUri ?: ""))
      }.getOrNull()
      avatarUploading = false
      if (resp?.isSuccessful == true) {
        localAvatarOverride = resp.body()?.avatarUrl ?: ""
        dashboardViewModel.refresh()
        Toast.makeText(context, if (dataUri == null) "Foto rimossa." else "Foto aggiornata!", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Aggiornamento foto non riuscito. Riprova.", Toast.LENGTH_LONG).show()
      }
    }
  }

  // Photo picker: conversione URI → data URI Base64 in IO (stessa pipeline
  // dell'avatar hiker: EXIF + downscale + JPEG + Base64).
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent(),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      val dataUri = withContext(Dispatchers.IO) {
        AvatarUtils.prepareAvatarForUpload(context.contentResolver, uri)
      }
      if (dataUri == null) {
        Toast.makeText(context, "Impossibile leggere la foto selezionata. Riprova.", Toast.LENGTH_LONG).show()
      } else {
        uploadAvatar(dataUri)
      }
    }
  }

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
    Box(Modifier.fillMaxSize()) {
    // Profondità "telemetria" coerente con dashboard e simulatore rifiuti.
    TsmAuroraBackground(modifier = Modifier.fillMaxSize(), baseColor = Bg, particleCount = 10)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(padding)
        .padding(horizontal = 16.dp),
    ) {
      Spacer(Modifier.height(8.dp))

      // ── Scheda identità (glass) ──
      TsmGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(contentAlignment = Alignment.BottomEnd) {
            // Glow ciano dietro la foto: profondità "athletic" sul punto focale.
            TsmGlow(color = Cyan, modifier = Modifier.size(128.dp).align(Alignment.Center), alpha = 0.35f)
            AvatarImage(
              avatarUrl = shownAvatar?.takeIf { it.isNotBlank() },
              fallbackName = refuge?.name ?: "Rifugio",
              size = 96.dp,
              isLoading = avatarUploading,
              modifier = Modifier.clickable(enabled = !avatarUploading) {
                photoPickerLauncher.launch("image/*")
              },
            )
            // Badge fotocamera: rende scopribile il tap per cambiare foto.
            Surface(
              shape = CircleShape,
              color = Cyan,
              modifier = Modifier
                .size(30.dp)
                .clickable(enabled = !avatarUploading) { photoPickerLauncher.launch("image/*") },
            ) {
              Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = "Cambia foto",
                tint = Color(0xFF0D0D0F),
                modifier = Modifier.padding(6.dp),
              )
            }
          }
          if (!shownAvatar.isNullOrBlank()) {
            TextButton(onClick = { uploadAvatar(null) }, enabled = !avatarUploading) {
              Text("Rimuovi foto", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
          }
          Spacer(Modifier.height(6.dp))
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
      SettingsRow(
        icon = Icons.Filled.Campaign,
        title = stringResource(R.string.refuge_board_menu_title),
        subtitle = stringResource(R.string.refuge_board_menu_subtitle),
        onClick = onNavigateToBoard,
      )

      Spacer(Modifier.height(10.dp))

      // ── Impostazioni e sicurezza ──
      SettingsRow(
        icon = Icons.Outlined.Lock,
        title = "Cambia password",
        subtitle = "Aggiorna la password di accesso dell'account",
        onClick = onNavigateToChangePassword,
      )

      Spacer(Modifier.height(24.dp))
      OutlinedButton(
        onClick = { profileViewModel.logout(onLoggedOut) },
        modifier = Modifier.fillMaxWidth(),
      ) { Text(stringResource(R.string.action_logout)) }
      Spacer(Modifier.height(24.dp))
    }
    }
  }
}

@Composable
private fun SettingsRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
) {
  TsmGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp, onClick = onClick) {
    Row(
      modifier = Modifier.padding(16.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = null, tint = Cyan)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
      }
      Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
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
