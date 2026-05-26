package it.trentosmartmountain.app.ui.screens.registra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.location.TrackingStatus
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.RegistraViewModel

/**
 * Tab **Registra**: mappa OSMdroid, tracking GPS in tempo reale e pulsante SOS.
 *
 * Integra [TsmMapView] (tile OpenTopoMap), permessi posizione/notifiche,
 * [RegistraViewModel] per metriche e traccia live.
 *
 * **SOS**: conferma → countdown 15s → beacon BLE + POST (o coda offline).
 * **Dialog stop**: conferma arresto registrazione e chiusura sessione sul server se collegata.
 */
@Composable
fun RegistraScreen(
  modifier: Modifier = Modifier,
  viewModel: RegistraViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val hasFineLocation =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED
  val hasCoarseLocation =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED
  val hasLocationPermission = hasFineLocation || hasCoarseLocation

  val permissionLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
      val granted =
        results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      viewModel.onLocationPermissionResult(granted)
    }

  val notificationLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

  LaunchedEffect(Unit) {
    viewModel.syncActiveSessionFromServer()
  }

  LaunchedEffect(hasLocationPermission) {
    if (hasLocationPermission) {
      viewModel.onLocationPermissionResult(true)
    } else {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
      )
    }
  }

  // Se c'è una sessione pendente da SessionDetail e ora abbiamo i permessi GPS,
  // avvia il tracking automaticamente. Notification permission segue lo stesso
  // flow del bottone REC. Il controllo GPS hardware è dentro RegistraViewModel.startTracking().
  LaunchedEffect(uiState.activeSessionId, uiState.hasLocationPermission) {
    if (uiState.activeSessionId != null &&
      uiState.hasLocationPermission &&
      uiState.trackingStatus == TrackingStatus.IDLE
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasNotifPermission =
          ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
          ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotifPermission) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
      viewModel.startTracking()
    }
  }

  val onStartTracking = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val hasNotifPermission =
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
      if (!hasNotifPermission) {
        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    viewModel.startTracking()
  }

  val isTrackingActive = uiState.trackingStatus != TrackingStatus.IDLE
  val canCenterOnUser =
    uiState.hasLocationPermission && uiState.userLocation != null

  Box(modifier = modifier.fillMaxSize()) {
    SosAlertBorderOverlay(show = uiState.showSosAlertBorder)

    TsmMapView(
      modifier = Modifier.fillMaxSize(),
      userLocation = uiState.userLocation,
      trackGeoPoints = uiState.trackGeoPoints,
      centerOnUserTick = uiState.centerOnUserTick,
      hasLocationPermission = uiState.hasLocationPermission,
    )

    RegistraTopHud(
      isTrackingActive = isTrackingActive,
      trackingStatus = uiState.trackingStatus,
      gpsSignalLevel = uiState.gpsSignalLevel,
      gpsAccuracyLabel = uiState.gpsAccuracyLabel,
      elapsedSeconds = uiState.elapsedSeconds,
      distanceMeters = uiState.distanceMeters,
      elevationGainMeters = uiState.elevationGainMeters,
      altitudeMeters = uiState.currentAltitudeMeters,
      modifier = Modifier.align(Alignment.TopCenter),
    )

    uiState.incomingSosDebugMessage?.let { debugMsg ->
      if (!uiState.showIncomingEmergencyIcon) {
        Surface(
          modifier =
            Modifier
              .align(Alignment.TopCenter)
              .padding(top = 100.dp, start = 16.dp, end = 16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
          shape = MaterialTheme.shapes.small,
        ) {
          Text(
            text = debugMsg,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    }

    if (uiState.showIncomingEmergencyIcon) {
      IncomingEmergencyIconButton(
        count = uiState.incomingEmergencies.size,
        onClick = viewModel::onIncomingEmergencyIconClick,
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .padding(
              top = RegistraLayout.incomingEmergencyIconTop(isTrackingActive),
              end = 12.dp,
            ),
      )
    }

    if (uiState.isAutoPaused) {
      RegistraAutoPauseBanner(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(
              top = RegistraLayout.autoPauseTop(isTrackingActive),
              start = 24.dp,
              end = 24.dp,
            ),
      )
    }

    if (uiState.locationPermissionDenied) {
      Surface(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(top = 60.dp, start = 16.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
        shape = MaterialTheme.shapes.small,
      ) {
        Text(
          text = stringResource(R.string.registra_location_permission_denied),
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }

    RegistraMapActionFabs(
      canCenterOnUser = canCenterOnUser,
      onCenterOnUser = viewModel::centerOnUser,
      onSosClick = viewModel::onSosFabClicked,
      modifier = Modifier.align(Alignment.BottomEnd),
    )

    if (isTrackingActive) {
      RegistraTrackingControls(
        trackingStatus = uiState.trackingStatus,
        onTogglePause = viewModel::togglePause,
        onStop = viewModel::requestStopTracking,
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = RegistraLayout.bottomInset),
      )
    } else {
      RegistraRecFab(
        onClick = onStartTracking,
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = RegistraLayout.bottomInset),
      )
    }

    val sosBannerMessage = uiState.sosStatusMessage
    if (
      sosBannerMessage != null &&
        (uiState.sosPhase == RegistraViewModel.SosPhase.ACTIVE ||
          uiState.sosPhase == RegistraViewModel.SosPhase.QUEUED_OFFLINE ||
          uiState.sosPhase == RegistraViewModel.SosPhase.SENDING)
    ) {
      Surface(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(top = 120.dp, start = 16.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
        shape = MaterialTheme.shapes.small,
        onClick = viewModel::requestCancelActiveSos,
      ) {
        Text(
          text = sosBannerMessage,
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }
  }

  if (uiState.showSosConfirmDialog) {
    SosConfirmDialog(
      selectedType = uiState.sosSelectedType,
      onTypeChange = viewModel::updateSosEmergencyType,
      onDismiss = viewModel::dismissSosConfirmDialog,
      onProceed = viewModel::confirmSosProceed,
    )
  }

  if (uiState.sosPhase == RegistraViewModel.SosPhase.COUNTDOWN) {
    SosCountdownDialog(
      secondsRemaining = uiState.sosCountdownSeconds,
      onCancel = viewModel::cancelSosCountdown,
    )
  }

  if (uiState.showSosCancelDialog) {
    SosCancelActiveDialog(
      onDismiss = viewModel::dismissSosCancelDialog,
      onMistake = { viewModel.confirmCancelActiveSos("MISTAKE") },
      onResolved = { viewModel.confirmCancelActiveSos("RESOLVED_SELF") },
    )
  }

  if (uiState.showSosListSheet) {
    SosIncomingListDialog(
      emergencies = uiState.incomingEmergencies,
      onDismiss = viewModel::closeSosListSheet,
      onSelect = viewModel::openIncomingEmergencyDetail,
    )
  }

  uiState.selectedIncomingEmergency?.let { emergency ->
    if (uiState.showSosDetailSheet) {
      SosIncomingDetailDialog(
        emergency = emergency,
        isGroupLeader = uiState.isSessionGroupLeader,
        onClose = viewModel::closeSosDetailSheet,
        onDismissEmergency = viewModel::dismissSelectedIncomingEmergency,
        onShareWithGroup = viewModel::shareSelectedIncomingEmergency,
      )
    }
  }

  // ── Dialog "Attività troppo corta" — chiede conferma per attività libere < 50m ──
  // Tre opzioni distinte per evitare che chi vuole solo "chiudere" il dialog cancelli
  // l'intera registrazione cliccando "Scarta":
  //   - Salva comunque (verde):    forza save anche sotto i 50m
  //   - Continua (testo grigio):   chiude il dialog, tracking resta attivo
  //   - Cancella (testo rosso):    discardTracking, sicuro perché esplicitamente "cancella"
  if (uiState.shortActivityConfirm) {
    AlertDialog(
      onDismissRequest = viewModel::dismissShortActivity,
      containerColor = TsmSurface,
      title = { Text("Attività troppo corta", color = Color.White) },
      text = {
        Text(
          "Hai percorso meno di 50 metri. Le attività brevi solitamente sono avvii accidentali. Cosa vuoi fare?",
          color = Color.Gray,
        )
      },
      confirmButton = {
        Button(
          onClick = viewModel::confirmShortActivity,
          colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
          shape = RoundedCornerShape(8.dp),
        ) { Text("Salva comunque") }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          TextButton(onClick = viewModel::discardTracking) {
            Text("Cancella", color = MaterialTheme.colorScheme.error)
          }
          TextButton(onClick = viewModel::dismissShortActivity) {
            Text("Continua", color = Color.Gray)
          }
        }
      },
    )
  }

  if (uiState.gpsDisabledWarning) {
    AlertDialog(
      onDismissRequest = viewModel::dismissGpsWarning,
      title = { Text("GPS spento") },
      text = { Text("Per registrare l'escursione devi attivare il GPS dalle impostazioni del dispositivo.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.dismissGpsWarning()
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
          },
        ) { Text("Apri impostazioni") }
      },
      dismissButton = {
        TextButton(onClick = viewModel::dismissGpsWarning) { Text("Annulla") }
      },
    )
  }

  if (uiState.showStopConfirm) {
    val distKm = uiState.distanceMeters / 1000.0
    val movingH = uiState.elapsedSeconds / 3600.0
    val pts = HikeEstimation.finalPoints(distKm, uiState.elevationGainMeters, movingH)
    val durationLabel = if (uiState.elapsedSeconds > 0) HikeEstimation.formatHours(movingH) else "0m"
    AlertDialog(
      onDismissRequest = viewModel::dismissStopConfirm,
      containerColor = TsmSurface,
      title = { Text("Salva Attività", color = Color.White) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          // Riga KPI riepilogo metriche tracking
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = TsmSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SaveKpiCell("Distanza", "%.1f km".format(distKm), TsmAccent, Modifier.weight(1f))
              SaveKpiCell("Durata", durationLabel, Color.White, Modifier.weight(1f))
              SaveKpiCell("Dislivello", "+${uiState.elevationGainMeters}m", Color(0xFFFF9800), Modifier.weight(1f))
              SaveKpiCell("Punti", "$pts pt", Color(0xFFFFC107), Modifier.weight(1f))
            }
          }
          // Campo nome editabile con default Escursione – <data>
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              "Nome attività",
              style = MaterialTheme.typography.labelSmall,
              color = Color.Gray,
            )
            OutlinedTextField(
              value = uiState.activityNameDraft,
              onValueChange = viewModel::updateActivityNameDraft,
              placeholder = { Text("Es. Cima Tosa", color = Color.Gray) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { viewModel.confirmStopTracking() },
          colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text("Salva", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          TextButton(onClick = viewModel::discardTracking) {
            Text("Scarta", color = MaterialTheme.colorScheme.error)
          }
          TextButton(onClick = viewModel::dismissStopConfirm) {
            Text("Annulla", color = Color.Gray)
          }
        }
      },
    )
  }
}

@Composable
private fun SaveKpiCell(
  label: String,
  value: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      value,
      color = valueColor,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(2.dp))
    Text(
      label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.Gray,
    )
  }
}
