package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.location.LocationSnapshot
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Centro predefinito: area Trento (zoom escursionistico). */
val TSM_DEFAULT_MAP_CENTER = GeoPoint(46.0664, 11.1257)

private const val USER_MARKER_ID = "tsm_user_location"

private const val TRACK_POLYLINE_ID = "tsm_live_track"

/**
 * Wrapper Compose per [MapView] **OSMdroid**: mappa escursionistica con tile OpenTopoMap.
 *
 * - Marker posizione utente e polyline del percorso registrato
 * - Ciclo di vita allineato all'Activity (`onResume` / `onPause`)
 * - Centratura mappa solo su incremento di [centerOnUserTick] (tap FAB, non ad ogni fix GPS)
 *
 * La configurazione globale OSMdroid è in [it.trentosmartmountain.app.TsmApplication].
 */
@Composable
fun TsmMapView(
  modifier: Modifier = Modifier,
  userLocation: LocationSnapshot?,
  trackGeoPoints: List<GeoPoint>,
  centerOnUserTick: Int,
  hasLocationPermission: Boolean,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val mapView =
    remember {
      MapView(context).apply {
        setTileSource(openTopoMapTileSource())
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        isTilesScaledToDpi = true
        controller.setZoom(13.0)
        controller.setCenter(TSM_DEFAULT_MAP_CENTER)
      }
    }

  val userMarker =
    remember(mapView, context) {
      Marker(mapView).apply {
        id = USER_MARKER_ID
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "La tua posizione"
        icon = ContextCompat.getDrawable(context, R.drawable.ic_user_location)
      }
    }

  DisposableEffect(lifecycleOwner, mapView) {
    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_RESUME -> mapView.onResume()
          Lifecycle.Event.ON_PAUSE -> mapView.onPause()
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    mapView.onResume()
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView.onPause()
      mapView.overlays.remove(userMarker)
    }
  }

  val trackPolyline =
    remember(mapView) {
      Polyline(mapView).apply {
        id = TRACK_POLYLINE_ID
        outlinePaint.color = android.graphics.Color.parseColor("#4FC3F7")
        outlinePaint.strokeWidth = 10f
      }
    }

  LaunchedEffect(trackGeoPoints) {
    mapView.overlays.remove(trackPolyline)
    if (trackGeoPoints.size >= 2) {
      trackPolyline.setPoints(ArrayList(trackGeoPoints))
      mapView.overlays.add(0, trackPolyline)
    }
    mapView.invalidate()
  }

  LaunchedEffect(hasLocationPermission, userLocation) {
    if (hasLocationPermission && userLocation != null) {
      val point = GeoPoint(userLocation.latitude, userLocation.longitude)
      userMarker.position = point
      if (!mapView.overlays.contains(userMarker)) {
        mapView.overlays.add(userMarker)
      }
    } else {
      mapView.overlays.remove(userMarker)
    }
    mapView.invalidate()
  }

  // Centra solo al tap sul pulsante (non ad ogni aggiornamento GPS).
  LaunchedEffect(centerOnUserTick) {
    if (centerOnUserTick == 0) return@LaunchedEffect
    val snap = userLocation ?: return@LaunchedEffect
    mapView.controller.animateTo(GeoPoint(snap.latitude, snap.longitude))
  }

  AndroidView(
    factory = { mapView },
    modifier = modifier,
    update = { it.invalidate() },
  )
}
