package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.remote.dto.LiveLocationItemDto
import it.trentosmartmountain.app.data.remote.dto.LiveUserDto
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveParticipantSheet(
  participant: LiveLocationItemDto?,
  isGroupLeaderViewer: Boolean,
  isSelfViewer: Boolean = false,
  onDismiss: () -> Unit,
) {
  if (participant == null) return
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = TsmSurface,
  ) {
    LiveParticipantContent(
      participant = participant,
      isGroupLeaderViewer = isGroupLeaderViewer,
      isSelfViewer = isSelfViewer,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp),
    )
  }
}

@Composable
fun LiveParticipantContent(
  participant: LiveLocationItemDto,
  isGroupLeaderViewer: Boolean,
  isSelfViewer: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val user = participant.user
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      AvatarImage(
        avatarUrl = user.avatarUrl,
        fallbackName = user.displayLabel(),
        size = 72.dp,
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = user.firstName ?: user.username ?: "—",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = user.lastName ?: "",
          style = MaterialTheme.typography.titleMedium,
          color = Color.LightGray,
        )
        ParticipantRoleBadge(role = user.role)
      }
    }

    // Sesso — rispetta profileVisibility (privato / solo amici / pubblico).
    LeaderDetailRow(
      label = stringResource(R.string.live_participant_sex),
      value = formatSex(user.sex),
    )

    if (isGroupLeaderViewer || isSelfViewer) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            stringResource(
              if (isSelfViewer && !isGroupLeaderViewer) {
                R.string.live_participant_self_section
              } else {
                R.string.live_participant_leader_section
              },
            ),
          style = MaterialTheme.typography.labelMedium,
          color = TsmPrimary,
          fontWeight = FontWeight.SemiBold,
        )
        LeaderDetailRow(
          label = stringResource(R.string.live_participant_altitude),
          value = formatAltitude(participant.location.altitudeM),
        )
        LeaderDetailRow(
          label = stringResource(R.string.live_participant_last_update),
          value = formatUpdatedAt(participant.location.updatedAt),
        )
        LeaderDetailRow(
          label = stringResource(R.string.live_participant_status),
          value = formatTrackingStatus(participant.location.trackingStatus),
        )
      }
    }
  }
}

@Composable
private fun ParticipantRoleBadge(role: String) {
  val isLeader = role == "groupLeader"
  Surface(
    color = (if (isLeader) TsmAccent else Color(0xFF29B6F6)).copy(alpha = 0.2f),
    shape = MaterialTheme.shapes.small,
  ) {
    Text(
      text =
        stringResource(
          if (isLeader) {
            R.string.live_participant_badge_leader
          } else {
            R.string.live_participant_badge_participant
          },
        ),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
      style = MaterialTheme.typography.labelSmall,
      color = if (isLeader) TsmAccent else Color(0xFF29B6F6),
    )
  }
}

@Composable
private fun LeaderDetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = Color.White,
      fontWeight = FontWeight.Medium,
    )
  }
}

fun LiveUserDto.displayLabel(): String {
  val parts = listOfNotNull(firstName, lastName).joinToString(" ").trim()
  return parts.ifBlank { username ?: "" }
}

private fun formatAltitude(altitudeM: Double?): String =
  altitudeM?.let { "${it.toInt()} m" } ?: "—"

fun formatSex(sex: String?): String =
  when (sex?.uppercase(Locale.ROOT)) {
    "M" -> "Maschio"
    "F" -> "Femmina"
    "X" -> "Altro"
    "N" -> "Non specificato"
    else -> "—"
  }

private fun formatTrackingStatus(status: String?): String =
  when (status?.uppercase(Locale.ROOT)) {
    "PAUSED" -> "In pausa"
    "MOVING" -> "In movimento"
    else -> "—"
  }

private fun formatUpdatedAt(iso: String?): String {
  if (iso.isNullOrBlank()) return "—"
  return runCatching {
    val instant = Instant.parse(iso)
    val zoned = instant.atZone(ZoneId.systemDefault())
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ITALY).format(zoned)
  }.getOrElse { iso }
}
