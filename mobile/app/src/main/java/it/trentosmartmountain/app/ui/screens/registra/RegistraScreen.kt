package it.trentosmartmountain.app.ui.screens.registra

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.viewmodel.RegistraViewModel

/**
 * Schermata “Registra”: mappa escursionistica, posizione GPS e SOS.
 */
@Composable
fun RegistraScreen(
  modifier: Modifier = Modifier,
  viewModel: RegistraViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var showSosDialog by rememberSaveable { mutableStateOf(false) }

  val hasFineLocation =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED
  val hasCoarseLocation =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED
  val hasPermission = hasFineLocation || hasCoarseLocation

  val permissionLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
      val granted =
        results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      viewModel.onLocationPermissionResult(granted)
    }

  LaunchedEffect(hasPermission) {
    if (hasPermission) {
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

  Box(modifier = modifier.fillMaxSize()) {
    TsmMapView(
      modifier = Modifier.fillMaxSize(),
      userLocation = uiState.userLocation,
      centerOnUserTick = uiState.centerOnUserTick,
      hasLocationPermission = uiState.hasLocationPermission,
    )

    GpsSignalIndicator(
      signalLevel = uiState.gpsSignalLevel,
      accuracyLabel = uiState.gpsAccuracyLabel,
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .padding(top = 12.dp),
    )

    if (uiState.locationPermissionDenied) {
      Surface(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(top = 52.dp, start = 16.dp, end = 16.dp),
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

    val canCenterOnUser =
      uiState.hasLocationPermission && uiState.userLocation != null
    FloatingActionButton(
      onClick = { if (canCenterOnUser) viewModel.centerOnUser() },
      modifier =
        Modifier
          .align(Alignment.BottomStart)
          .padding(24.dp)
          .alpha(if (canCenterOnUser) 1f else 0.45f),
      containerColor = TsmSurface,
      contentColor = TsmAccent,
    ) {
      Icon(
        imageVector = Icons.Filled.MyLocation,
        contentDescription = stringResource(R.string.registra_center_location_cd),
      )
    }

    FloatingActionButton(
      onClick = { showSosDialog = true },
      modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
      containerColor = TsmSos,
      contentColor = MaterialTheme.colorScheme.onError,
    ) {
      Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.registra_sos_cd))
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
}
