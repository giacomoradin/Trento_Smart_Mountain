package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.trentosmartmountain.app.data.location.LocationSnapshot
import it.trentosmartmountain.app.data.remote.dto.LiveLocationDto
import it.trentosmartmountain.app.data.remote.dto.LiveLocationItemDto
import it.trentosmartmountain.app.data.remote.dto.LiveUserDto
import it.trentosmartmountain.app.data.remote.dto.SosMapMarkerDto
import android.graphics.Paint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Centro predefinito: area Trento (zoom escursionistico). */
val TSM_DEFAULT_MAP_CENTER = GeoPoint(46.0664, 11.1257)

private const val USER_MARKER_ID = "tsm_user_location"
private const val TRACK_POLYLINE_ID = "tsm_live_track"
private const val PLANNED_ROUTE_POLYLINE_ID = "tsm_planned_route"
private const val LIVE_MARKER_PREFIX = "live_"
private const val SOS_MARKER_PREFIX = "sos_"

private fun MapView.applyPlannedRouteLayer(
  context: android.content.Context,
  points: List<GeoPoint>,
  plannedRoutePolyline: Polyline,
  trackPolyline: Polyline,
  trackGeoPoints: List<GeoPoint>,
) {
  overlays.remove(plannedRoutePolyline)
  overlays.remove(trackPolyline)
  PlannedRouteMapDecoration.removeEndpointMarkers(this)

  var insertAt = 0
  when {
    points.size >= 2 -> {
      plannedRoutePolyline.setPoints(ArrayList(points))
      overlays.add(insertAt++, plannedRoutePolyline)
    }
    points.size == 1 -> {
      val p = points.first()
      plannedRoutePolyline.setPoints(arrayListOf(p, p))
      overlays.add(insertAt++, plannedRoutePolyline)
    }
  }

  if (points.isNotEmpty()) {
    val endpoints = PlannedRouteMapDecoration.createEndpointMarkers(this, context, points)
    endpoints.forEach { marker ->
      overlays.add(insertAt++, marker)
    }
  }

  when {
    trackGeoPoints.size >= 2 -> {
      trackPolyline.setPoints(ArrayList(trackGeoPoints))
      overlays.add(insertAt, trackPolyline)
    }
    trackGeoPoints.size == 1 -> {
      val p = trackGeoPoints.first()
      trackPolyline.setPoints(arrayListOf(p, p))
      overlays.add(insertAt, trackPolyline)
    }
  }
}

/**
 * Mappa OSMdroid con marker live colorati:
 * verde = te stesso, azzurro = altri, oro = capogruppo, rosso = SOS.
 */
@Composable
fun TsmMapView(
  modifier: Modifier = Modifier,
  userLocation: LocationSnapshot?,
  trackGeoPoints: List<GeoPoint>,
  /** Tracciato GPX/SAT pianificato per la sessione collegata (sotto la traccia live). */
  plannedRouteGeoPoints: List<GeoPoint> = emptyList(),
  centerOnUserTick: Int,
  centerOnLivePointLat: Double? = null,
  centerOnLivePointLon: Double? = null,
  centerOnLivePointTick: Int = 0,
  hasLocationPermission: Boolean,
  currentUserId: String?,
  isCurrentUserLeader: Boolean,
  liveLocations: List<LiveLocationItemDto> = emptyList(),
  sosUserIds: Set<String> = emptySet(),
  /** SOS inviato da questo dispositivo (fase ACTIVE / coda offline). */
  hasOwnActiveSos: Boolean = false,
  sosOnlyMarkers: List<SosMapMarkerDto> = emptyList(),
  onLiveMarkerTap: (LiveLocationItemDto) -> Unit = {},
  onSelfMarkerTap: () -> Unit = {},
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
        // Centro provvisorio (Trento) — verrà sostituito appena il primo fix GPS
        // arriva da [LaunchedEffect autoCenterOnFirstFix] sotto. Lo lasciamo come
        // fallback per il caso in cui il GPS non sia mai disponibile (no permission
        // o GPS spento): meglio vedere Trento che una mappa centrata su (0,0).
        controller.setCenter(TSM_DEFAULT_MAP_CENTER)
      }
    }

  // Flag che ricorda se abbiamo già centrato la mappa sulla posizione utente.
  // Vogliamo farlo UNA volta sola al primo fix GPS valido — successivi
  // ri-centramenti devono passare da [centerOnUserTick] (FAB "centra su di me")
  // altrimenti l'animazione disturberebbe il panning manuale dell'utente.
  // Si resetta quando il composable viene ricreato (tab switch + ritorno),
  // così tornando alla schermata la mappa ricentra sulla posizione corrente.
  var hasCenteredOnUser by remember { mutableStateOf(false) }

  LaunchedEffect(hasLocationPermission, userLocation) {
    if (!hasCenteredOnUser && hasLocationPermission && userLocation != null) {
      mapView.controller.animateTo(
        GeoPoint(userLocation.latitude, userLocation.longitude),
      )
      hasCenteredOnUser = true
    }
  }

  val userMarker =
    remember(mapView) {
      Marker(mapView).apply {
        id = USER_MARKER_ID
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "La tua posizione"
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

  val plannedRoutePolyline =
    remember(mapView) {
      Polyline(mapView).apply {
        id = PLANNED_ROUTE_POLYLINE_ID
        outlinePaint.color = android.graphics.Color.parseColor("#B3FF7043")
        outlinePaint.strokeWidth = 10f
        outlinePaint.isAntiAlias = true
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        isEnabled = true
        infoWindow = null
        setOnClickListener { _, _, _ -> true }
      }
    }

  val trackPolyline =
    remember(mapView) {
      Polyline(mapView).apply {
        id = TRACK_POLYLINE_ID
        outlinePaint.color = android.graphics.Color.parseColor("#4FC3F7")
        outlinePaint.strokeWidth = 12f
        outlinePaint.isAntiAlias = true
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        // isEnabled=false in OSMdroid disabilita anche il draw (non solo i tap).
        isEnabled = true
        infoWindow = null
        // Consuma il tap senza aprire il popup predefinito sul tracciato.
        setOnClickListener { _, _, _ -> true }
      }
    }

  LaunchedEffect(
    plannedRouteGeoPoints,
    trackGeoPoints,
    liveLocations,
    sosOnlyMarkers,
    sosUserIds,
    hasLocationPermission,
    userLocation,
    currentUserId,
    isCurrentUserLeader,
    hasOwnActiveSos,
  ) {
    mapView.applyPlannedRouteLayer(
      context = context,
      points = plannedRouteGeoPoints,
      plannedRoutePolyline = plannedRoutePolyline,
      trackPolyline = trackPolyline,
      trackGeoPoints = trackGeoPoints,
    )

    mapView.overlays.removeAll(
      mapView.overlays.filterIsInstance<Marker>().filter { marker ->
        val id = marker.id
        id?.startsWith(LIVE_MARKER_PREFIX) == true || id?.startsWith(SOS_MARKER_PREFIX) == true
      }.toSet(),
    )

    val liveUserIds = liveLocations.map { it.user.id }.toSet()

    for (item in liveLocations) {
      val point = GeoPoint(item.location.lat, item.location.lon)
      val isLeader = item.user.role == "groupLeader"
      val hasSos = sosUserIds.contains(item.user.id)
      val kind = MapMarkerIcons.kindForUser(isSelf = false, isLeader = isLeader, hasSos = hasSos)
      val sizeDp = MapMarkerIcons.markerSizeDp(kind, isSelf = false)
      val marker =
        Marker(mapView).apply {
          id = "${LIVE_MARKER_PREFIX}${item.user.id}"
          position = point
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
          title = item.user.displayLabel()
          icon = MapMarkerIcons.create(context, kind, sizeDp, withSosBadge = hasSos)
          setOnMarkerClickListener { _, _ ->
            onLiveMarkerTap(item)
            true
          }
        }
      mapView.overlays.add(marker)
    }

    for (sos in sosOnlyMarkers) {
      if (sos.userId in liveUserIds) continue
      val point = GeoPoint(sos.lat, sos.lon)
      val marker =
        Marker(mapView).apply {
          id = "${SOS_MARKER_PREFIX}${sos.userId}"
          position = point
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
          title = sos.displayName ?: "SOS"
          val kind = LiveMarkerKind.SOS
          icon =
            MapMarkerIcons.create(
              context,
              kind,
              MapMarkerIcons.markerSizeDp(kind, isSelf = false),
              withSosBadge = true,
            )
          setOnMarkerClickListener { _, _ ->
            onLiveMarkerTap(
              LiveLocationItemDto(
                user =
                  LiveUserDto(
                    id = sos.userId,
                    username = sos.displayName,
                    firstName = sos.firstName,
                    lastName = sos.lastName,
                    avatarUrl = sos.avatarUrl,
                    role = "hiker",
                  ),
                location = LiveLocationDto(lat = sos.lat, lon = sos.lon),
              ),
            )
            true
          }
        }
      mapView.overlays.add(marker)
    }

    if (hasLocationPermission && userLocation != null) {
      val point = GeoPoint(userLocation.latitude, userLocation.longitude)
      userMarker.position = point
      val selfHasSos =
        currentUserId != null &&
          (hasOwnActiveSos || sosUserIds.contains(currentUserId))
      val kind =
        MapMarkerIcons.kindForUser(
          isSelf = true,
          isLeader = isCurrentUserLeader,
          hasSos = selfHasSos,
        )
      val sizeDp = MapMarkerIcons.markerSizeDp(kind, isSelf = true)
      userMarker.icon = MapMarkerIcons.create(context, kind, sizeDp, withSosBadge = selfHasSos)
      userMarker.setOnMarkerClickListener { _, _ ->
        onSelfMarkerTap()
        true
      }
      if (!mapView.overlays.contains(userMarker)) {
        mapView.overlays.add(userMarker)
      }
    } else {
      mapView.overlays.remove(userMarker)
    }

    mapView.invalidate()
  }

  LaunchedEffect(centerOnUserTick) {
    if (centerOnUserTick == 0) return@LaunchedEffect
    val snap = userLocation ?: return@LaunchedEffect
    mapView.controller.animateTo(GeoPoint(snap.latitude, snap.longitude))
  }

  LaunchedEffect(centerOnLivePointTick, centerOnLivePointLat, centerOnLivePointLon) {
    if (centerOnLivePointTick == 0) return@LaunchedEffect
    val lat = centerOnLivePointLat ?: return@LaunchedEffect
    val lon = centerOnLivePointLon ?: return@LaunchedEffect
    mapView.controller.animateTo(GeoPoint(lat, lon))
  }

  AndroidView(
    factory = { mapView },
    modifier = modifier,
    update = { view ->
      // Aggiorna anche qui: più affidabile di solo LaunchedEffect con AndroidView.
      view.applyPlannedRouteLayer(
        context = context,
        points = plannedRouteGeoPoints,
        plannedRoutePolyline = plannedRoutePolyline,
        trackPolyline = trackPolyline,
        trackGeoPoints = trackGeoPoints,
      )
      view.invalidate()
    },
  )
}
