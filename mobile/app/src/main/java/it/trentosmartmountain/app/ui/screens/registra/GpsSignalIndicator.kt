package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmSurface

/** Indicatore qualità segnale GPS (barre + accuratezza in metri) sopra la mappa. */
@Composable
fun GpsSignalIndicator(
  signalLevel: Int,
  accuracyLabel: String?,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(20.dp))
        .background(TsmSurface.copy(alpha = 0.92f))
        .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = stringResource(R.string.registra_gps_label),
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.85f),
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      val barHeights = listOf(6.dp, 8.dp, 10.dp, 12.dp)
      barHeights.forEachIndexed { index, height ->
        val active = index < signalLevel
        Box(
          modifier =
            Modifier
              .width(5.dp)
              .height(height)
              .clip(CircleShape)
              .background(if (active) TsmAccent else Color.Gray.copy(alpha = 0.35f)),
        )
      }
    }
    accuracyLabel?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
      )
    }
  }
}
