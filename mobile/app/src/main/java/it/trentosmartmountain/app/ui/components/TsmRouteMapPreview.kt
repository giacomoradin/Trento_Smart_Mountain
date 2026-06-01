package it.trentosmartmountain.app.ui.components

import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.screens.registra.openTopoMapTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/**
 * Anteprima statica (senza interazione) di un percorso su mappa OSMdroid.
 *
 * Visualizza una polyline su sfondo OpenTopoMap, centrata e zoomata per
 * contenere l'intero tracciato. Usata nel feed e nei dettagli post per
 * dare contesto geografico immediato senza appesantire la UI con mappe interattive.
 */
@Composable
fun TsmRouteMapPreview(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    lineColor: Int = android.graphics.Color.parseColor("#4DD0E1"),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Trasformiamo i DTO in GeoPoint
    val geoPoints = remember(points) {
        points.map { GeoPoint(it.lat, it.lon) }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(openTopoMapTileSource())
            setMultiTouchControls(false) // Disabilita interazione per performance e stabilità in lista
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            setOnClickListener { /* Impedisce click pass-through */ }
            setOnTouchListener { _, _ -> true } // Consuma tocchi per non far scrollare la mappa
        }
    }

    // Gestione ciclo di vita MapView
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.overlays.clear()
            if (geoPoints.size >= 2) {
                val polyline = Polyline(view).apply {
                    setPoints(ArrayList(geoPoints))
                    outlinePaint.color = lineColor
                    outlinePaint.strokeWidth = 8f
                    outlinePaint.isAntiAlias = true
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                view.overlays.add(polyline)

                // Zoom sul tracciato
                val bbox = BoundingBox.fromGeoPoints(geoPoints)
                // Usiamo post così l'animazione/zoom avviene dopo che la vista è misurata
                view.post {
                    view.zoomToBoundingBox(bbox, false, 40)
                }
            }
            view.invalidate()
        }
    )
}
