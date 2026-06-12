package it.trentosmartmountain.app.ui.screens.session

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import it.trentosmartmountain.app.ui.components.RouteDirectionArrowsOverlay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
import org.osmdroid.views.overlay.infowindow.InfoWindow

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
            // Tile scalate alla densità schermo: a 1:1 su display hi-dpi anche
            // lo zoom massimo sembrava "troppo distante" (vera causa B-01).
            isTilesScaledToDpi = true
            // Limiti zoom espliciti: garantiscono il de-zoom (pinch-out) anche sotto
            // il livello di auto-fit. Senza, alcune versioni OSMdroid bloccano il
            // min-zoom dopo zoomToBoundingBox → "non si riesce a rimpicciolire".
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
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
            outlinePaint.strokeWidth = 14f
        }
    }

    // Frecce direzionali animate (Komoot-style) sulla polyline interattiva: stessa
    // logica usata da TsmRouteMapPreview, qui con un periodo leggermente più
    // lungo (2.8s) perché la mappa è interattiva e i gesti si combinano peggio
    // con animazioni veloci.
    val arrowsTransition = rememberInfiniteTransition(label = "sentieri-arrows")
    val arrowsPhaseAnim by arrowsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sentieri-arrows-tween",
    )
    // Holder mutabile letto dal callback overlay (OSMdroid Overlay.draw() gira
    // fuori da Compose, quindi una semplice cella di un FloatArray ci basta).
    // Fase quantizzata a 84 step su ciclo 2,8 s = ~30 Hz di invalidate: fluido
    // alla vista, metà dei redraw del tween puro. NB: meno di ~25 Hz risulta
    // visibilmente scattoso.
    val quantizedPhase = (arrowsPhaseAnim * 84).toInt() / 84f
    val phaseHolder = remember { floatArrayOf(0f) }
    phaseHolder[0] = quantizedPhase
    val directionArrows = remember(mapView) {
        RouteDirectionArrowsOverlay(
            pointsProvider = { trackPolyline.actualPoints ?: emptyList() },
            phaseProvider = { phaseHolder[0] },
        )
    }
    LaunchedEffect(quantizedPhase) { mapView.invalidate() }

    // Marker icons cache per tipo (evita di ricreare il bitmap a ogni marker).
    val iconCache = remember { mutableMapOf<SentieroMarkerType, Drawable>() }
    fun iconFor(type: SentieroMarkerType): android.graphics.drawable.Drawable =
        iconCache.getOrPut(type) {
            buildRouteEndpointIcon(context.resources.displayMetrics.density, type)
        }

    // Info window custom (tema scuro) condivisa da tutti i marker, al posto del bubble di default.
    val sharedInfoWindow = remember(mapView) { TsmMarkerInfoWindow(mapView) }

    // Fingerprint per-valore del contenuto: una nuova identità di lista con lo
    // stesso contenuto (parent che non usa remember) NON deve ri-eseguire il
    // rebuild + auto-fit, altrimenti lo zoom dell'utente viene riportato al
    // livello bounding-box a ogni ricomposizione ("zoom bloccato troppo distante").
    val contentKey = remember(markers, polyline) {
        buildString {
            append(polyline.size)
            polyline.firstOrNull()?.let { append('|'); append(it.latitude); append(','); append(it.longitude) }
            polyline.lastOrNull()?.let { append('|'); append(it.latitude); append(','); append(it.longitude) }
            markers.forEach { append('|'); append(it.id); append(':'); append(it.point.latitude); append(','); append(it.point.longitude) }
        }
    }

    LaunchedEffect(contentKey) {
        InfoWindow.closeAllInfoWindowsOn(mapView)
        mapView.overlays.clear()

        // Polyline (sotto i marker)
        if (polyline.size >= 2) {
            trackPolyline.setPoints(ArrayList(polyline))
            mapView.overlays.add(trackPolyline)
            mapView.overlays.add(directionArrows) // frecce sopra alla traccia
        }

        // Marker
        markers.forEach { m ->
            val marker = Marker(mapView).apply {
                position = m.point
                title = m.title
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = iconFor(m.type)
                infoWindow = sharedInfoWindow
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

/**
 * Info window in tema scuro che sostituisce il bubble di default di OSMdroid (layout bonuspack,
 * sfondo bianco squadrato). Mostra il [Marker.getTitle] in una card arrotondata con bordo accent.
 * Un tap sulla card la chiude.
 */
private class TsmMarkerInfoWindow(mapView: MapView) :
    InfoWindow(buildInfoWindowView(mapView), mapView) {

    override fun onOpen(item: Any?) {
        val marker = item as? Marker ?: return
        (view as? TextView)?.apply {
            text = marker.title
            setOnClickListener { close() }
        }
    }

    override fun onClose() {}
}

/** Card arrotondata (TextView) usata come contenuto della [TsmMarkerInfoWindow]. */
private fun buildInfoWindowView(mapView: MapView): View {
    val ctx = mapView.context
    val density = ctx.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()
    return TextView(ctx).apply {
        setTextColor(Color.WHITE)
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        maxWidth = dp(240)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor("#2C2C2E"))
            setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#4FC3F7"))
        }
    }
}
