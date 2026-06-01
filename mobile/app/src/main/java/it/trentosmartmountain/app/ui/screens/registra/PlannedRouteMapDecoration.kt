package it.trentosmartmountain.app.ui.screens.registra

import android.content.Context
import it.trentosmartmountain.app.ui.screens.session.RouteEndpointKind
import it.trentosmartmountain.app.ui.screens.session.buildRouteEndpointIcon
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Marker partenza/arrivo del percorso pianificato su [TsmMapView]. */
object PlannedRouteMapDecoration {
    const val ENDPOINT_PREFIX = "planned_ep_"

    fun removeEndpointMarkers(mapView: MapView) {
        mapView.overlays.removeAll(
            mapView.overlays.filterIsInstance<Marker>().filter { marker ->
                marker.id?.startsWith(ENDPOINT_PREFIX) == true
            }.toSet(),
        )
    }

    fun createEndpointMarkers(
        mapView: MapView,
        context: Context,
        points: List<GeoPoint>,
    ): List<Marker> {
        if (points.isEmpty()) return emptyList()
        val density = context.resources.displayMetrics.density
        val markers = mutableListOf<Marker>()

        points.firstOrNull()?.let { start ->
            markers.add(
                endpointMarker(
                    mapView = mapView,
                    id = "${ENDPOINT_PREFIX}start",
                    point = start,
                    title = "Partenza",
                    kind = RouteEndpointKind.START,
                    density = density,
                ),
            )
        }
        if (points.size > 1) {
            markers.add(
                endpointMarker(
                    mapView = mapView,
                    id = "${ENDPOINT_PREFIX}end",
                    point = points.last(),
                    title = "Arrivo",
                    kind = RouteEndpointKind.FINISH,
                    density = density,
                ),
            )
        }
        return markers
    }

    private fun endpointMarker(
        mapView: MapView,
        id: String,
        point: GeoPoint,
        title: String,
        kind: RouteEndpointKind,
        density: Float,
    ): Marker =
        Marker(mapView).apply {
            this.id = id
            position = point
            if (kind == RouteEndpointKind.FINISH) {
                setAnchor(0.15f, 0.92f)
            } else {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            this.title = title
            icon = buildRouteEndpointIcon(density, kind)
            isFlat = true
            setOnMarkerClickListener { _, _ -> true }
        }
}
