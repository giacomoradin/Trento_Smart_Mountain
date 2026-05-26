package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.remote.dto.EmergencyResponse
import it.trentosmartmountain.app.ui.theme.TsmSos

/** Bordo rosso lampeggiante su Registra (solo alert capogruppo). */
@Composable
fun SosAlertBorderOverlay(show: Boolean, modifier: Modifier = Modifier) {
  if (!show) return
  val transition = rememberInfiniteTransition(label = "sos_border")
  val alpha by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
    label = "sos_border_alpha",
  )
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .border(BorderStroke(4.dp, TsmSos.copy(alpha = alpha)))
        .padding(0.dp),
  )
}

@Composable
fun IncomingEmergencyIconButton(
  count: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BadgedBox(
    badge = {
      if (count > 0) {
        Badge { Text(count.toString()) }
      }
    },
    modifier = modifier,
  ) {
    IconButton(
      onClick = onClick,
      modifier =
        Modifier
          .size(48.dp)
          .clip(CircleShape),
    ) {
      Icon(
        Icons.Filled.Emergency,
        contentDescription = stringResource(R.string.sos_incoming_icon_cd),
        tint = TsmSos,
        modifier = Modifier.size(42.dp),
      )
    }
  }
}

@Composable
fun SosIncomingListDialog(
  emergencies: List<EmergencyResponse>,
  onDismiss: () -> Unit,
  onSelect: (EmergencyResponse) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sos_incoming_list_title)) },
    text = {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(emergencies, key = { it.id }) { emergency ->
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable { onSelect(emergency) },
          ) {
            Column(Modifier.padding(12.dp)) {
              Text(
                emergency.profileSnapshot.displayName,
                fontWeight = FontWeight.Bold,
              )
              Text(
                emergencyTypeLabel(emergency.emergencyType),
                style = MaterialTheme.typography.bodySmall,
              )
              Text(
                stringResource(R.string.sos_status_label, emergency.status),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
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
fun SosIncomingDetailDialog(
  emergency: EmergencyResponse,
  isGroupLeader: Boolean,
  onClose: () -> Unit,
  onDismissEmergency: () -> Unit,
  onShareWithGroup: () -> Unit,
) {
  val coords = emergency.coordinates.coordinates
  val lat = coords.getOrNull(1)
  val lon = coords.getOrNull(0)

  AlertDialog(
    onDismissRequest = onClose,
    title = {
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(stringResource(R.string.sos_detail_title))
        IconButton(onClick = onClose) {
          Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sos_close))
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emergency.profileSnapshot.displayName, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.sos_type_label_detail, emergencyTypeLabel(emergency.emergencyType)))
        if (lat != null && lon != null) {
          Text(stringResource(R.string.sos_coords_label, lat, lon))
        }
        emergency.profileSnapshot.personalInfo?.let { info ->
          info.heightCm?.let { Text(stringResource(R.string.sos_profile_height, it)) }
          info.weightKg?.let { Text(stringResource(R.string.sos_profile_weight, it)) }
        }
        emergency.profileSnapshot.experience?.caiLevel?.let {
          Text(stringResource(R.string.sos_profile_cai, it))
        }
        Text(
          stringResource(R.string.sos_beacon_id_label, emergency.beaconInstanceId),
          style = MaterialTheme.typography.labelSmall,
        )
      }
    },
    confirmButton = {
      if (isGroupLeader) {
        Column {
          if (emergency.status == "ACTIVE") {
            Button(
              onClick = onShareWithGroup,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(stringResource(R.string.sos_share_with_group))
            }
          }
          Button(
            onClick = onDismissEmergency,
            modifier = Modifier.fillMaxWidth(),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
              ),
          ) {
            Text(stringResource(R.string.sos_dismiss_emergency))
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onClose) {
        Text(stringResource(R.string.sos_close))
      }
    },
  )
}

@Composable
private fun emergencyTypeLabel(code: String): String {
  val res = SOS_TYPES.firstOrNull { it.first == code }?.second
  return if (res != null) stringResource(res) else code
}
