package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.R

val SOS_TYPES =
  listOf(
    "INJURY" to R.string.sos_type_injury,
    "LOST" to R.string.sos_type_lost,
    "AVALANCHE" to R.string.sos_type_avalanche,
    "WEATHER" to R.string.sos_type_weather,
    "EQUIPMENT" to R.string.sos_type_equipment,
    "OTHER" to R.string.sos_type_other,
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosConfirmDialog(
  selectedType: String,
  onTypeChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onProceed: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val labelRes = SOS_TYPES.firstOrNull { it.first == selectedType }?.second ?: R.string.sos_type_injury

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.registra_sos_dialog_title)) },
    text = {
      Column {
        Text(stringResource(R.string.registra_sos_dialog_body))
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = it },
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        ) {
          OutlinedTextField(
            value = stringResource(labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.sos_type_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
              Modifier
                .menuAnchor()
                .fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SOS_TYPES.forEach { (code, res) ->
              DropdownMenuItem(
                text = { Text(stringResource(res)) },
                onClick = {
                  onTypeChange(code)
                  expanded = false
                },
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onProceed,
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
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.registra_sos_dialog_dismiss))
      }
    },
  )
}

@Composable
fun SosCountdownDialog(
  secondsRemaining: Int,
  onCancel: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onCancel,
    title = { Text(stringResource(R.string.sos_countdown_title)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          stringResource(R.string.sos_countdown_body),
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = secondsRemaining.toString(),
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
          textAlign = TextAlign.Center,
          fontSize = 48.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error,
        )
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onCancel) {
        Text(stringResource(R.string.sos_countdown_cancel))
      }
    },
  )
}

@Composable
fun SosCancelActiveDialog(
  onDismiss: () -> Unit,
  onMistake: () -> Unit,
  onResolved: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sos_revoke_title)) },
    text = { Text(stringResource(R.string.sos_revoke_body)) },
    confirmButton = {
      Button(onClick = onMistake) {
        Text(stringResource(R.string.sos_revoke_mistake))
      }
    },
    dismissButton = {
      TextButton(onClick = onResolved) {
        Text(stringResource(R.string.sos_revoke_resolved))
      }
    },
  )
}
