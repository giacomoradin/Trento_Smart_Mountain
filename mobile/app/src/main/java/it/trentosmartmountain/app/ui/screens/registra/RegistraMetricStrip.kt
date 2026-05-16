package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.location.TrackingStatus
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmSurface

/** Striscia metriche live: tempo, distanza, dislivello, quota (visibile solo con tracking attivo). */
@Composable
fun RegistraMetricStrip(
  trackingStatus: TrackingStatus,
  elapsedSeconds: Long,
  distanceMeters: Double,
  elevationGainMeters: Int,
  altitudeMeters: Int?,
  modifier: Modifier = Modifier,
) {
  if (trackingStatus == TrackingStatus.IDLE) return

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(TsmSurface.copy(alpha = 0.94f))
        .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MetricColumn(
      label = stringResource(R.string.registra_metric_time),
      value = formatElapsed(elapsedSeconds),
      highlight = trackingStatus == TrackingStatus.RECORDING,
    )
    MetricColumn(
      label = stringResource(R.string.registra_metric_distance),
      value = formatDistance(distanceMeters),
    )
    MetricColumn(
      label = stringResource(R.string.registra_metric_altitude),
      value = altitudeMeters?.let { "$it m" } ?: "—",
    )
    MetricColumn(
      label = stringResource(R.string.registra_metric_elevation),
      value = "+$elevationGainMeters m",
    )
  }
}

@Composable
private fun MetricColumn(
  label: String,
  value: String,
  highlight: Boolean = false,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.Gray,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = if (highlight) TsmAccent else Color.White,
    )
  }
}

private fun formatElapsed(seconds: Long): String {
  val h = seconds / 3600
  val m = (seconds % 3600) / 60
  val s = seconds % 60
  return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatDistance(meters: Double): String {
  return if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)
}
