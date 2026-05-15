package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.ui.unit.dp

/** Altezze indicative per allineare FAB destra al pannello tracking in basso. */
object RegistraLayout {
  val gpsIndicatorTop = 20.dp
  val gpsIndicatorApproxHeight = 36.dp
  val autoPauseBelowGps = 8.dp

  val bottomInset = 20.dp
  val trackingControlsHeight = 56.dp
  val metricsGap = 10.dp
  val metricsStripHeight = 52.dp

  /** FAB GPS+SOS a riposo (angolo in basso a destra). */
  val fabBottomRest = bottomInset

  /** FAB sollevati appena sopra il pannello Pausa/Stop + metriche. */
  val fabBottomRaised =
    bottomInset + trackingControlsHeight + metricsGap + metricsStripHeight + 12.dp
}
