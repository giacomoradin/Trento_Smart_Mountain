package it.trentosmartmountain.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.screens.registra.openTopoMapTileSource
import it.trentosmartmountain.app.ui.theme.TsmColors
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Anteprima statica (senza interazione) di un percorso su mappa OSMdroid.
 *
 * Visualizza una polyline su sfondo OpenTopoMap, centrata e zoomata per
 * contenere l'intero tracciato. Usata nel feed e nei dettagli post per
 * dare contesto geografico immediato senza appesantire la UI con mappe interattive.
 *
 * Include marker di inizio (cerchio verde) e fine (bandiera a scacchi).
 */
@Composable
fun TsmRouteMapPreview(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    lineColor: Int = android.graphics.Color.parseColor("#4FC3F7"),
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
            setMultiTouchControls(false) // niente pinch-zoom: è un'anteprima statica
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            isFlingEnabled = false
            // IMPORTANTE: NON consumare i tocchi (prima ritornava true). Se la mappa
            // "mangia" il gesto, il contenitore non lo riceve mai: niente swipe
            // orizzontale nel HorizontalPager (mappa↔altimetria) né scroll verticale
            // nel LazyColumn del feed. Ritornando false lasciamo che il genitore
            // gestisca i gesti; lo swipe orizzontale è poi pilotato esplicitamente
            // dall'overlay in TsmRouteElevationPager.
            setOnTouchListener { _, _ -> false }
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

                // Marker Inizio: Cerchio Verde
                val startMarker = Marker(view).apply {
                    position = geoPoints.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createCircleIcon(android.graphics.Color.parseColor("#4CAF50"))
                    title = "Inizio"
                }
                view.overlays.add(startMarker)

                // Marker Fine: Bandiera a scacchi
                val endMarker = Marker(view).apply {
                    position = geoPoints.last()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createCheckeredIcon()
                    title = "Fine"
                }
                view.overlays.add(endMarker)

                // Zoom sul tracciato
                val bbox = BoundingBox.fromGeoPoints(geoPoints)
                view.post {
                    view.zoomToBoundingBox(bbox, false, 60)
                }
            }
            view.invalidate()
        }
    )
}

/** Crea un'icona circolare colorata per il marker di inizio. */
private fun createCircleIcon(color: Int): android.graphics.drawable.BitmapDrawable {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Bordo bianco per stacco
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Cerchio interno colorato
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)
    
    return android.graphics.drawable.BitmapDrawable(null, bitmap)
}

/** Crea un'icona a scacchi (🏁) per il marker di fine. */
private fun createCheckeredIcon(): android.graphics.drawable.BitmapDrawable {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Sfondo bianco
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Griglia a scacchi 2x2 centrale
    val cellSize = size / 4f
    val offset = size / 4f
    paint.color = android.graphics.Color.BLACK
    canvas.drawRect(offset, offset, offset + cellSize, offset + cellSize, paint)
    canvas.drawRect(offset + cellSize, offset + cellSize, offset + 2 * cellSize, offset + 2 * cellSize, paint)
    
    return android.graphics.drawable.BitmapDrawable(null, bitmap)
}
