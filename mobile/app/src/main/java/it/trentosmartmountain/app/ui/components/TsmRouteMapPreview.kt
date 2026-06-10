package it.trentosmartmountain.app.ui.components

import android.graphics.Paint
import android.view.ViewTreeObserver
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
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

/** Step di quantizzazione della fase frecce: limita gli invalidate della MapView a ~7,5 Hz. */
private const val ARROWS_PHASE_STEPS = 18

/**
 * Anteprima statica (senza interazione) di un percorso su mappa OSMdroid.
 *
 * Comportamento tipo immagine: niente pinch, doppio tap, pan o zoom.
 * Il tracciato viene centrato dopo il layout della view (evita vista "globo").
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TsmRouteMapPreview(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    lineColor: Int = android.graphics.Color.parseColor("#4FC3F7"),
    /** Se true, mappa interattiva (solo casi speciali); default anteprima fissa. */
    interactive: Boolean = false,
    /** Se false, mostra solo le tile (es. sfondo editor storie senza traccia duplicata). */
    showTrack: Boolean = true,
    /**
     * Scena storia senza foto: fit più largo sulla polyline, ripetizione orizzontale tile,
     * zoom leggermente ridotto per evitare bordi neri dopo pan/zoom/rotazione.
     */
    storySceneMode: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Sanitizza: coordinate NaN/Inf o fuori range fanno crashare OSMdroid nel
    // calcolo del bounding box / proiezione (schermata bianca). Le filtriamo via.
    val geoPoints = remember(points) {
        points
            .filter {
                it.lat.isFinite() && it.lon.isFinite() &&
                    it.lat in -90.0..90.0 && it.lon in -180.0..180.0
            }
            .map { GeoPoint(it.lat, it.lon) }
    }

    // Animazione della fase delle chevron (frecce direzionali stile Komoot):
    // 0→1 in ~2.4s lineare = effetto "scorrimento" continuo verso il fine percorso.
    // Salviamo la fase in una mutable state per leggerla dal callback Overlay.draw.
    val transition = rememberInfiniteTransition(label = "route-arrows-phase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "route-arrows-tween",
    )
    // Quantizzazione a 18 step/ciclo: il tween cambia valore a ogni frame (~60 Hz)
    // e ogni cambio invalida l'intera MapView (tile + polyline + overlay). Con la
    // fase quantizzata l'invalidate scende a ~7,5 Hz — visivamente identico per
    // chevron in scorrimento lento, ma ~87% di redraw in meno (batteria/jank,
    // moltiplicato per ogni mappa visibile nel feed).
    var arrowsPhase by remember { mutableStateOf(0f) }
    arrowsPhase = (phase * ARROWS_PHASE_STEPS).toInt() / ARROWS_PHASE_STEPS.toFloat()

    val mapView = remember(interactive, storySceneMode) {
        MapView(context).apply {
            setTileSource(openTopoMapTileSource())
            isTilesScaledToDpi = false
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            if (interactive) {
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                isFlingEnabled = true
            } else if (storySceneMode) {
                applyStoryScenePreviewMode()
            } else {
                applyFixedRoutePreviewMode()
            }
        }
    }

    val trackPolyline = remember(mapView) {
        Polyline(mapView).apply {
            outlinePaint.color = lineColor
            outlinePaint.strokeWidth = 8f
            outlinePaint.isAntiAlias = true
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
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
            mapView.overlays.clear()
        }
    }

    // Overlay frecce direzionali: ricalcola i pixel a ogni invalidate (zoom/pan),
    // legge la fase animata dalla state Compose `arrowsPhase`.
    val directionArrows = remember(mapView) {
        RouteDirectionArrowsOverlay(
            pointsProvider = { geoPoints },
            phaseProvider = { arrowsPhase },
        )
    }

    // Tick di redraw allineato all'animazione della fase: ogni cambio invalida la
    // MapView così l'overlay frecce ridisegna nella nuova posizione.
    LaunchedEffect(arrowsPhase, geoPoints) {
        if (geoPoints.size >= 2 && showTrack) mapView.invalidate()
    }

    // Fingerprint per-valore della traccia: protegge l'auto-fit da ricomposizioni
    // del parent che ricreano la lista punti con lo stesso contenuto. Senza,
    // sulle mappe interattive ogni re-fit resettava lo zoom dell'utente al
    // livello bounding-box ("zoom bloccato troppo distante").
    val routeKey = remember(geoPoints) {
        buildString {
            append(geoPoints.size)
            geoPoints.firstOrNull()?.let { append('|'); append(it.latitude); append(','); append(it.longitude) }
            geoPoints.lastOrNull()?.let { append('|'); append(it.latitude); append(','); append(it.longitude) }
        }
    }

    LaunchedEffect(routeKey, interactive, lineColor, showTrack, storySceneMode) {
        trackPolyline.outlinePaint.color = lineColor
        when {
            interactive -> Unit
            storySceneMode -> mapView.applyStoryScenePreviewMode()
            else -> mapView.applyFixedRoutePreviewMode()
        }
        mapView.overlays.clear()
        if (geoPoints.size >= 2) {
            if (showTrack) {
                trackPolyline.setPoints(ArrayList(geoPoints))
                mapView.overlays.add(trackPolyline)
                // Frecce direzionali sopra alla polyline ma sotto ai marker
                // start/end (così rimangono "incollate" alla traccia senza
                // sovrapporsi all'iconografia dei capi del percorso).
                mapView.overlays.add(directionArrows)
                RouteMapMarkerOverlays.attachStartEndMarkers(mapView, context, geoPoints)
            }
            val bbox = BoundingBox.fromGeoPoints(geoPoints)
            when {
                interactive ->
                    mapView.fitRouteWhenLaidOut(bbox, paddingPx = 60, lockScroll = false, zoomOutExtra = 0.0)
                storySceneMode ->
                    mapView.fitRouteWhenLaidOut(
                        bbox,
                        paddingPx = 60,
                        lockScroll = false,
                        zoomOutExtra = 0.65,
                        paddingScale = 0.22f,
                    )
                else ->
                    mapView.fitRouteWhenLaidOut(bbox, paddingPx = 60, lockScroll = true, zoomOutExtra = 0.0)
            }
        }
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.then(
            if (!interactive) {
                Modifier.pointerInteropFilter { true }
            } else {
                Modifier
            },
        ),
        onRelease = { view ->
            view.onPause()
            view.overlays.clear()
        },
        update = { it.invalidate() },
    )
}

/** Scena storia a tutta area: tile ripetibili, nessun limite stretto sul bbox. */
private fun MapView.applyStoryScenePreviewMode() {
    setMultiTouchControls(false)
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    isFlingEnabled = false
    setHorizontalMapRepetitionEnabled(true)
    setVerticalMapRepetitionEnabled(false)
    isClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
    setOnTouchListener { _, _ -> true }
    resetScrollableAreaLimitLatitude()
    resetScrollableAreaLimitLongitude()
}

/** Disabilita ogni gesto sulla MapView (anteprima fissa come immagine). */
private fun MapView.applyFixedRoutePreviewMode() {
    setMultiTouchControls(false)
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    isFlingEnabled = false
    setHorizontalMapRepetitionEnabled(false)
    isClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
    setOnTouchListener { _, _ -> true }
}

/**
 * Esegue [zoomToBoundingBox] solo quando width/height > 0.
 * Senza attendere il layout, OSMdroid resta allo zoom mondo (0,0).
 *
 * Non blocchiamo min/max zoom (rompeva il fit); l'interazione è già
 * disabilitata da [applyFixedRoutePreviewMode] e dal filtro Compose.
 */
private fun MapView.fitRouteWhenLaidOut(
    bbox: BoundingBox,
    paddingPx: Int,
    lockScroll: Boolean,
    zoomOutExtra: Double = 0.0,
    paddingScale: Float = 0f,
) {
    fun applyFit() {
        minZoomLevel = 3.0
        maxZoomLevel = 19.0
        val dynamicPad =
            if (paddingScale > 0f && width > 0 && height > 0) {
                (kotlin.math.min(width, height) * paddingScale).toInt()
            } else {
                0
            }
        val totalPad = paddingPx + dynamicPad
        runCatching {
            zoomToBoundingBox(bbox, false, totalPad)
        }
        if (zoomOutExtra > 0.0) {
            runCatching {
                val z = zoomLevelDouble - zoomOutExtra
                controller.setZoom(z.coerceAtLeast(minZoomLevel))
            }
        }
        if (lockScroll) {
            val pad = 0.02
            setScrollableAreaLimitDouble(
                BoundingBox(
                    bbox.latNorth + pad,
                    bbox.lonEast + pad,
                    bbox.latSouth - pad,
                    bbox.lonWest - pad,
                ),
            )
        } else {
            resetScrollableAreaLimitLatitude()
            resetScrollableAreaLimitLongitude()
        }
        invalidate()
    }

    fun scheduleFit() {
        post {
            if (width > 0 && height > 0) {
                applyFit()
            } else {
                viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (width <= 0 || height <= 0) return
                            viewTreeObserver.removeOnGlobalLayoutListener(this)
                            post { applyFit() }
                        }
                    },
                )
            }
        }
    }

    scheduleFit()
}

