package it.trentosmartmountain.app.ui.screens.registra

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.ble.BluetoothHelper
import it.trentosmartmountain.app.data.ble.SosBeaconParser
import it.trentosmartmountain.app.viewmodel.SosBeaconScannerViewModel

@Composable
fun SosBeaconScannerDialog(
  beaconInstanceId: String,
  onDismiss: () -> Unit,
  viewModel: SosBeaconScannerViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = androidx.compose.ui.platform.LocalContext.current

  val requiredPermissions = BluetoothHelper.requiredScanPermissions()

  fun hasAllPermissions(): Boolean = BluetoothHelper.hasScanPermissions(context)

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
      val granted = results.values.all { it }
      viewModel.onPermissionsResult(granted)
    }

  val bluetoothEnableLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        // BT appena abilitato: proviamo ad avviare la scan
        if (hasAllPermissions()) {
          viewModel.startScan(beaconInstanceId)
        } else {
          permissionLauncher.launch(requiredPermissions)
        }
      }
      // se l'utente ha rifiutato, il viewModel rimane in idle e la UI mostra il bottone
    }

  LaunchedEffect(beaconInstanceId) {
    if (!hasAllPermissions()) {
      permissionLauncher.launch(requiredPermissions)
      return@LaunchedEffect
    }
    if (!BluetoothHelper.isBluetoothEnabled(context)) {
      // non avviamo la scan; la UI mostra il bottone per attivare BT
      return@LaunchedEffect
    }
    viewModel.startScan(beaconInstanceId)
  }

  DisposableEffect(Unit) {
    onDispose { viewModel.stopScan() }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sos_beacon_scanner_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          stringResource(R.string.sos_beacon_scanner_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val btEnabled = BluetoothHelper.isBluetoothEnabled(context)
        when {
          !btEnabled -> {
            Text(
              stringResource(R.string.sos_beacon_bt_off_scanner),
              color = MaterialTheme.colorScheme.error,
            )
            Button(
              onClick = {
                if (BluetoothHelper.canRequestEnableBluetooth(context)) {
                  runCatching {
                    bluetoothEnableLauncher.launch(BluetoothHelper.createEnableIntent())
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(stringResource(R.string.sos_bluetooth_enable_activate))
            }
          }
          state.permissionDenied ->
            Text(
              stringResource(R.string.sos_beacon_permission_denied),
              color = MaterialTheme.colorScheme.error,
            )
          state.errorMessage != null ->
            Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
          state.smoothedRssi == null && state.isScanning -> {
            Text(stringResource(R.string.sos_beacon_scanning))
            if (state.targetMajorMinorLabel.isNotBlank()) {
              Text(
                state.targetMajorMinorLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (!state.blePacketsSeen) {
              Text(
                stringResource(R.string.sos_beacon_scanning_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          state.smoothedRssi != null -> {
            val band = state.signalBand ?: SosBeaconParser.SignalBand.LOST
            Text(
              stringResource(R.string.sos_beacon_rssi_label, state.smoothedRssi!!),
              fontWeight = FontWeight.Bold,
            )
            Text(signalBandLabel(band))
            LinearProgressIndicator(
              progress = { signalProgress(band) },
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(8.dp),
              color = MaterialTheme.colorScheme.error,
            )
            state.estimatedDistanceM?.let { d ->
              Text(
                stringResource(R.string.sos_beacon_distance_estimate, d),
                style = MaterialTheme.typography.bodySmall,
              )
            }
            Text(
              stringResource(R.string.sos_beacon_distance_disclaimer),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.sos_close))
      }
    },
  )
}

@Composable
private fun signalBandLabel(band: SosBeaconParser.SignalBand): String =
  when (band) {
    SosBeaconParser.SignalBand.EXCELLENT -> stringResource(R.string.sos_beacon_signal_excellent)
    SosBeaconParser.SignalBand.GOOD -> stringResource(R.string.sos_beacon_signal_good)
    SosBeaconParser.SignalBand.WEAK -> stringResource(R.string.sos_beacon_signal_weak)
    SosBeaconParser.SignalBand.LOST -> stringResource(R.string.sos_beacon_signal_lost)
  }

private fun signalProgress(band: SosBeaconParser.SignalBand): Float =
  when (band) {
    SosBeaconParser.SignalBand.EXCELLENT -> 1f
    SosBeaconParser.SignalBand.GOOD -> 0.66f
    SosBeaconParser.SignalBand.WEAK -> 0.33f
    SosBeaconParser.SignalBand.LOST -> 0.1f
  }
