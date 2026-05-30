package it.trentosmartmountain.app.ui.screens.session

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.trentosmartmountain.app.ui.screens.registra.TSM_DEFAULT_MAP_CENTER
import it.trentosmartmountain.app.ui.screens.registra.openTopoMapTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Categoria del marker, determina colore/dimensione del pin. */
enum class SentieroMarkerType { DESTINATION, SELECTED_DESTINATION, START }

/** Marker generico per [TsmSentieriMapView]. [id] identifica l'entità (nome destinazione o codice sentiero). */
data class SentieroMapMarker(
    val id: String,
    val point: GeoPoint,
    val title: String,
    val type: SentieroMarkerType,
)

private const val SENTIERO_POLYLINE_ID = "tsm_sentiero_track"

/**
 * Variante di mappa OSMdroid per la scelta percorso da DB: gestisce una lista di
 * **marker custom** cliccabili e una **polyline** opzionale del sentiero selezionato.
 *
 * Riusa le stesse basi di [it.trentosmartmountain.app.ui.screens.registra.TsmMapView]
 * (tile OpenTopoMap, ciclo di vita) ma senza marker posizione utente né tracking.
 */
@Composable
fun TsmSentieriMapView(
    modifier: Modifier = Modifier,
    markers: List<SentieroMapMarker>,
    polyline: List<GeoPoint> = emptyList(),
    onMarkerClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(openTopoMapTileSource())
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(12.0)
            controller.setCenter(TSM_DEFAULT_MAP_CENTER)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
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
        }
    }

    val trackPolyline = remember(mapView) {
        Polyline(mapView).apply {
            id = SENTIERO_POLYLINE_ID
            outlinePaint.color = android.graphics.Color.parseColor("#FF7043")
            outlinePaint.strokeWidth = 10f
        }
    }

    // Marker icons cache per tipo (evita di ricreare il bitmap a ogni marker).
    val iconCache = remember { mutableMapOf<SentieroMarkerType, Drawable>() }
    fun iconFor(type: SentieroMarkerType): Drawable =
        iconCache.getOrPut(type) { buildMarkerIcon(context.resources.displayMetrics.density, type) }

    LaunchedEffect(markers, polyline) {
        mapView.overlays.clear()

        // Polyline (sotto i marker)
        if (polyline.size >= 2) {
            trackPolyline.setPoints(ArrayList(polyline))
            mapView.overlays.add(trackPolyline)
        }

        // Marker
        markers.forEach { m ->
            val marker = Marker(mapView).apply {
                position = m.point
                title = m.title
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = iconFor(m.type)
                setOnMarkerClickListener { mk, _ ->
                    mk.showInfoWindow()
                    onMarkerClick(m.id)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        // Auto-fit ai contenuti (polyline ha priorità, poi marker).
        val fitPoints = when {
            polyline.size >= 2 -> polyline
            markers.isNotEmpty() -> markers.map { it.point }
            else -> emptyList()
        }
        if (fitPoints.isNotEmpty()) {
            mapView.post {
                if (fitPoints.size == 1) {
                    mapView.controller.setZoom(14.0)
                    mapView.controller.setCenter(fitPoints.first())
                } else {
                    runCatching {
                        mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(fitPoints), false, 80)
                    }
                }
            }
        }
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier, update = { it.invalidate() })
}

/** Costruisce un pin a goccia colorato in base al tipo di marker. */
private fun buildMarkerIcon(density: Float, type: SentieroMarkerType): Drawable {
    val color = when (type) {
        SentieroMarkerType.DESTINATION -> android.graphics.Color.parseColor("#4FC3F7")
        SentieroMarkerType.SELECTED_DESTINATION -> android.graphics.Color.parseColor("#66BB6A")
        SentieroMarkerType.START -> android.graphics.Color.parseColor("#FFB300")
    }
    val sizePx = (28 * density).toInt().coerceAtLeast(24)
    val bmp = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bmp)
    val r = sizePx / 2f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(r, r, r - 2f * density, fill)
    canvas.drawCircle(r, r, r - 2f * density, stroke)
    return BitmapDrawable(android.content.res.Resources.getSystem(), bmp)
}
