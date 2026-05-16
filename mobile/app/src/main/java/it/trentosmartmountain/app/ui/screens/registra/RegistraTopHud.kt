package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.data.location.TrackingStatus

/**
 * HUD in alto: striscia metriche (solo in registrazione) + indicatore segnale GPS,
 * impilati in modo compatto.
 */
@Composable
fun RegistraTopHud(
  isTrackingActive: Boolean,
  trackingStatus: TrackingStatus,
  gpsSignalLevel: Int,
  gpsAccuracyLabel: String?,
  elapsedSeconds: Long,
  distanceMeters: Double,
  elevationGainMeters: Int,
  altitudeMeters: Int?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .padding(top = RegistraLayout.topInset, start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (isTrackingActive) {
      RegistraMetricStrip(
        trackingStatus = trackingStatus,
        elapsedSeconds = elapsedSeconds,
        distanceMeters = distanceMeters,
        elevationGainMeters = elevationGainMeters,
        altitudeMeters = altitudeMeters,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    GpsSignalIndicator(
      signalLevel = gpsSignalLevel,
      accuracyLabel = gpsAccuracyLabel,
      modifier =
        Modifier.padding(
          top = if (isTrackingActive) RegistraLayout.metricsToGpsGap else 0.dp,
        ),
    )
  }
}
