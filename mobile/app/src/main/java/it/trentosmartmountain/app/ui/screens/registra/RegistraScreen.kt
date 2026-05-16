package it.trentosmartmountain.app.ui.screens.registra

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
 * Schermata “Registra”: mappa escursionistica, tracking GPS e SOS.
 */
@Composable
fun RegistraScreen(
  modifier: Modifier = Modifier,
  viewModel: RegistraViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var showSosDialog by rememberSaveable { mutableStateOf(false) }

  // Toast di conferma dopo il salvataggio
  LaunchedEffect(uiState.activitySaved) {
    if (uiState.activitySaved) {
      android.widget.Toast.makeText(context, "✓ Attività salvata in Le Mie Attività!", android.widget.Toast.LENGTH_LONG).show()
      viewModel.dismissActivitySaved()
    }
  }

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
  // avvia il tracking automaticamente. Notification permission segue lo stesso flow del bottone REC.
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
    TsmMapView(
      modifier = Modifier.fillMaxSize(),
      userLocation = uiState.userLocation,
      trackGeoPoints = uiState.trackGeoPoints,
      centerOnUserTick = uiState.centerOnUserTick,
      hasLocationPermission = uiState.hasLocationPermission,
    )

    GpsSignalIndicator(
      signalLevel = uiState.gpsSignalLevel,
      accuracyLabel = uiState.gpsAccuracyLabel,
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .padding(top = RegistraLayout.gpsIndicatorTop),
    )

    if (uiState.isAutoPaused) {
      RegistraAutoPauseBanner(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(
              top =
                RegistraLayout.gpsIndicatorTop +
                  RegistraLayout.gpsIndicatorApproxHeight +
                  RegistraLayout.autoPauseBelowGps,
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
      isTrackingActive = isTrackingActive,
      canCenterOnUser = canCenterOnUser,
      onCenterOnUser = viewModel::centerOnUser,
      onSosClick = { showSosDialog = true },
      modifier = Modifier.align(Alignment.BottomEnd),
    )

    if (isTrackingActive) {
      Column(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(
              start = 16.dp,
              end = 16.dp,
              bottom = RegistraLayout.bottomInset,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        RegistraTrackingControls(
          trackingStatus = uiState.trackingStatus,
          onTogglePause = viewModel::togglePause,
          onStop = viewModel::requestStopTracking,
        )
        RegistraMetricStrip(
          trackingStatus = uiState.trackingStatus,
          elapsedSeconds = uiState.elapsedSeconds,
          distanceMeters = uiState.distanceMeters,
          elevationGainMeters = uiState.elevationGainMeters,
          altitudeMeters = uiState.currentAltitudeMeters,
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(top = RegistraLayout.metricsGap),
        )
      }
    } else {
      RegistraRecFab(
        onClick = onStartTracking,
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = RegistraLayout.bottomInset),
      )
    }
  }

  if (showSosDialog) {
    AlertDialog(
      onDismissRequest = { showSosDialog = false },
      title = { Text(stringResource(R.string.registra_sos_dialog_title)) },
      text = { Text(stringResource(R.string.registra_sos_dialog_body)) },
      confirmButton = {
        Button(
          onClick = { showSosDialog = false },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
          Text(stringResource(R.string.registra_sos_dialog_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showSosDialog = false }) {
          Text(stringResource(R.string.registra_sos_dialog_dismiss))
        }
      },
    )
  }

  // ── Dialog "Salva Attività" — sostituisce il vecchio stop confirm ──
  if (uiState.showStopConfirm) {
    val distKm = uiState.distanceMeters / 1000.0
    val movingH = uiState.elapsedSeconds / 3600.0
    val pts = HikeEstimation.finalPoints(distKm, uiState.elevationGainMeters, movingH)
    AlertDialog(
      onDismissRequest = viewModel::dismissStopConfirm,
      containerColor = TsmSurface,
      title = { Text("Salva Attività", color = Color.White, style = MaterialTheme.typography.titleMedium) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // Preview metriche
          Surface(color = TsmSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f km".format(distKm), style = MaterialTheme.typography.titleSmall, color = TsmAccent)
                Text("Distanza", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(HikeEstimation.formatHours(movingH), style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text("Durata", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${uiState.elevationGainMeters}m", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFF9800))
                Text("Dislivello", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$pts pt", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFD700))
                Text("Punti", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
              }
            }
          }
          // Nome attività
          Text("Nome attività", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
          OutlinedTextField(
            value = uiState.activityNameDraft,
            onValueChange = viewModel::onActivityNameDraftChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Escursione – …", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TsmAccent,
              unfocusedBorderColor = Color(0xFF3A3A3A),
              focusedContainerColor = TsmSurfaceVariant,
              unfocusedContainerColor = TsmSurfaceVariant,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
            ),
            shape = RoundedCornerShape(8.dp),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = viewModel::confirmStopTracking,
          colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
          shape = RoundedCornerShape(8.dp),
        ) { Text("Salva") }
      },
      dismissButton = {
        Row {
          TextButton(onClick = viewModel::discardTracking) {
            Text("Scarta", color = MaterialTheme.colorScheme.error)
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(onClick = viewModel::dismissStopConfirm) {
            Text("Annulla", color = Color.Gray)
          }
        }
      },
    )
  }
}
