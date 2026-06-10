package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import it.trentosmartmountain.app.data.remote.dto.LiveExcludedParticipantDto
import it.trentosmartmountain.app.data.remote.dto.LiveLocationItemDto
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupRosterMenu(
  activeParticipants: List<LiveLocationItemDto>,
  excludedParticipants: List<LiveExcludedParticipantDto>,
  onDismiss: () -> Unit,
  onActiveParticipantClick: (LiveLocationItemDto) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = TsmSurface,
  ) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .padding(bottom = 32.dp)
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.group_roster_title),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.group_roster_active_header),
        style = MaterialTheme.typography.labelMedium,
        color = TsmPrimary,
        fontWeight = FontWeight.SemiBold,
      )

      if (activeParticipants.isEmpty()) {
        Text(
          text = stringResource(R.string.group_roster_active_empty),
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray,
        )
      } else {
        activeParticipants.forEach { item ->
          RosterActiveRow(
            item = item,
            onClick = { onActiveParticipantClick(item) },
          )
        }
      }

      if (excludedParticipants.isNotEmpty()) {
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
        Text(
          text = stringResource(R.string.group_roster_excluded_header),
          style = MaterialTheme.typography.labelMedium,
          color = Color.Gray,
          fontWeight = FontWeight.SemiBold,
        )
        excludedParticipants.forEach { excluded ->
          RosterExcludedRow(excluded = excluded)
        }
      }
    }
  }
}

@Composable
private fun RosterActiveRow(
  item: LiveLocationItemDto,
  onClick: () -> Unit,
) {
  val user = item.user
  // Sesso se il backend lo espone (gate visibilità profilo).
  val sexLabel = formatSex(user.sex)
  val secondary =
    if (sexLabel != "—") {
      "${formatRosterTrackingStatus(item.location.trackingStatus)} · $sexLabel"
    } else {
      formatRosterTrackingStatus(item.location.trackingStatus)
    }
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AvatarImage(
      avatarUrl = user.avatarUrl,
      fallbackName = user.displayLabel(),
      size = 40.dp,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = user.displayLabel().ifBlank { user.username ?: "—" },
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        fontWeight = FontWeight.Medium,
      )
      Text(
        text = secondary,
        style = MaterialTheme.typography.bodySmall,
        color = TsmAccent,
      )
    }
    StatusDot(isActive = true)
  }
}

@Composable
private fun RosterExcludedRow(excluded: LiveExcludedParticipantDto) {
  val user = excluded.user
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AvatarImage(
      avatarUrl = user.avatarUrl,
      fallbackName = user.displayLabel(),
      size = 40.dp,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = user.displayLabel().ifBlank { user.username ?: "—" },
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
      )
      Text(
        text = formatExcludedReason(excluded.reason),
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray.copy(alpha = 0.8f),
      )
    }
    StatusDot(isActive = false)
  }
}

@Composable
private fun StatusDot(isActive: Boolean) {
  Surface(
    modifier = Modifier.size(10.dp),
    shape = MaterialTheme.shapes.extraLarge,
    color = if (isActive) TsmAccent else Color.Gray,
  ) {}
}

@Composable
private fun formatExcludedReason(reason: String): String =
  when (reason.uppercase()) {
    "TOO_FAR_FROM_ROUTE" -> stringResource(R.string.group_roster_reason_far)
    "STALE" -> stringResource(R.string.group_roster_reason_stale)
    "NO_SIGNAL" -> stringResource(R.string.group_roster_reason_no_signal)
    "MANUAL" -> stringResource(R.string.group_roster_reason_manual)
    else -> stringResource(R.string.group_roster_reason_other)
  }

private fun formatRosterTrackingStatus(status: String?): String =
  when (status?.uppercase()) {
    "PAUSED" -> "In pausa"
    "MOVING" -> "In movimento"
    else -> "In movimento"
  }
