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
  val autoPauseBannerHeight = 40.dp
  val sosBelowAutoPauseGap = 8.dp
  val sosBelowGpsGap = 24.dp

  /** Altezza approssimativa della striscia metriche (per posizionare banner auto-pausa). */
  val metricsStripHeight = 46.dp

  val bottomInset = 10.dp

  /** Bacheca rifugi / centra GPS in alto. */
  val topActionButtonSize = 44.dp
  val topActionPaddingTop = 8.dp
  val topActionPaddingEnd = 12.dp
  val topActionPaddingStart = 12.dp
  val topActionSpacing = 8.dp

  /** Play/pausa, SOS, partecipanti — stessa dimensione. */
  val primaryFabSize = 65.dp
  val primaryFabIconSize = 33.dp
  val primaryGlowSize = 124.dp

  /** Stop — leggermente più piccolo di play/pausa. */
  val secondaryFabSize = 53.dp
  val secondaryFabIconSize = 27.dp

  /** Padding orizzontale barra controlli in basso. */
  val bottomBarHorizontalPadding = 12.dp

  /** Offset verticale del banner auto-pausa sotto l’HUD GPS (+ metriche se attive). */
  fun autoPauseTop(isTrackingActive: Boolean) =
    topInset +
      (if (isTrackingActive) metricsStripHeight + metricsToGpsGap else 0.dp) +
      gpsIndicatorApproxHeight +
      autoPauseBelowGps

  /** Icona emergenze in entrata: sotto striscia metriche + indicatore GPS. */
  fun incomingEmergencyIconTop(isTrackingActive: Boolean) =
    autoPauseTop(isTrackingActive) + 8.dp

  /** Banner SOS attivo: sotto auto-pausa se presente, altrimenti sotto HUD GPS. */
  fun sosBannerTop(isTrackingActive: Boolean, isAutoPaused: Boolean) =
    if (isAutoPaused) {
      autoPauseTop(isTrackingActive) + autoPauseBannerHeight + sosBelowAutoPauseGap
    } else {
      topInset +
        (if (isTrackingActive) metricsStripHeight + metricsToGpsGap else 0.dp) +
        gpsIndicatorApproxHeight +
        sosBelowGpsGap
    }
}
