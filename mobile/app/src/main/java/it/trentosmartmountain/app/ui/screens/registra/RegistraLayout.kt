package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.ui.unit.dp

/**
 * Costanti di layout per la tab Registra (padding FAB, altezze pannelli).
 * Evita sovrapposizioni tra mappa, indicatori GPS e controlli in basso.
 */
object RegistraLayout {
  val topInset = 16.dp
  val metricsToGpsGap = 6.dp
  val gpsIndicatorApproxHeight = 32.dp
  val autoPauseBelowGps = 8.dp

  /** Altezza approssimativa della striscia metriche (per posizionare banner auto-pausa). */
  val metricsStripHeight = 46.dp

  val bottomInset = 20.dp

  /** Play/pausa, centra GPS e SOS — stessa dimensione. */
  val primaryFabSize = 65.dp
  val primaryFabIconSize = 33.dp

  /** Stop — leggermente più piccolo di play/pausa. */
  val secondaryFabSize = 53.dp
  val secondaryFabIconSize = 27.dp

  /** Padding inferiore FAB GPS+SOS (angolo in basso a destra). */
  val fabBottomRest = bottomInset

  /** Offset verticale del banner auto-pausa sotto l’HUD GPS (+ metriche se attive). */
  fun autoPauseTop(isTrackingActive: Boolean) =
    topInset +
      (if (isTrackingActive) metricsStripHeight + metricsToGpsGap else 0.dp) +
      gpsIndicatorApproxHeight +
      autoPauseBelowGps
}
