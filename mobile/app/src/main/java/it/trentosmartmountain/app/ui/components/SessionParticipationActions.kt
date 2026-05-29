package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.session.SessionParticipationUi
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSos

@Composable
fun SessionParticipationActions(
    ui: SessionParticipationUi,
    onLeaderStart: () -> Unit,
    onLeaderStop: () -> Unit,
    onJoinLive: () -> Unit,
    onSoloPractice: () -> Unit,
    onLeaveLive: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val hasPrimary = ui.primary != null
    val hasLeave = ui.showLeaveLive
    if (!hasPrimary && !hasLeave && ui.statusHint.isNullOrBlank()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.statusHint?.let { hint ->
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
        if (hasPrimary || hasLeave) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (ui.primary) {
                    SessionParticipationUi.PrimaryAction.LEADER_START -> {
                        ParticipationButton(
                            label = stringResource(R.string.session_card_avvia),
                            containerColor = TsmPrimary,
                            onClick = onLeaderStart,
                            modifier = Modifier.weight(1f),
                            height = if (compact) 40.dp else 52.dp,
                        )
                    }
                    SessionParticipationUi.PrimaryAction.LEADER_STOP -> {
                        ParticipationButton(
                            label = stringResource(R.string.session_card_arresta),
                            containerColor = TsmSos,
                            onClick = onLeaderStop,
                            modifier = Modifier.weight(1f),
                            height = if (compact) 40.dp else 52.dp,
                        )
                    }
                    SessionParticipationUi.PrimaryAction.JOIN_LIVE -> {
                        ParticipationButton(
                            label = stringResource(R.string.session_card_unisciti_live),
                            containerColor = TsmPrimary,
                            onClick = onJoinLive,
                            modifier = Modifier.weight(1f),
                            height = if (compact) 40.dp else 52.dp,
                        )
                    }
                    SessionParticipationUi.PrimaryAction.SOLO_PRACTICE -> {
                        ParticipationButton(
                            label = stringResource(R.string.session_card_prova_tracciato),
                            containerColor = TsmPrimary,
                            onClick = onSoloPractice,
                            modifier = Modifier.weight(1f),
                            height = if (compact) 40.dp else 52.dp,
                        )
                    }
                    null -> Unit
                }
                if (ui.showLeaveLive) {
                    OutlinedButton(
                        onClick = onLeaveLive,
                        modifier = Modifier
                            .weight(if (hasPrimary) 1f else 2f)
                            .height(if (compact) 40.dp else 52.dp),
                        border = BorderStroke(1.dp, TsmSos),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.session_card_arresta_per_me),
                            color = TsmSos,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipationButton(
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}
