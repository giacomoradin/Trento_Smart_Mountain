package it.trentosmartmountain.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val geoPoints = remember(points) {
        points.map { GeoPoint(it.lat, it.lon) }
    }

    val mapView = remember(interactive) {
        MapView(context).apply {
            setTileSource(openTopoMapTileSource())
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            if (interactive) {
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                isFlingEnabled = true
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

    LaunchedEffect(geoPoints, interactive, lineColor) {
        trackPolyline.outlinePaint.color = lineColor
        if (!interactive) {
            mapView.applyFixedRoutePreviewMode()
        }
        mapView.overlays.clear()
        if (geoPoints.size >= 2) {
            trackPolyline.setPoints(ArrayList(geoPoints))
            mapView.overlays.add(trackPolyline)

            mapView.overlays.add(
                Marker(mapView).apply {
                    position = geoPoints.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createCircleIcon(context, android.graphics.Color.parseColor("#4CAF50"))
                    title = "Inizio"
                },
            )
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = geoPoints.last()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createCheckeredIcon(context)
                    title = "Fine"
                },
            )

            val bbox = BoundingBox.fromGeoPoints(geoPoints)
            if (interactive) {
                mapView.fitRouteWhenLaidOut(bbox, paddingPx = 60, lockScroll = false)
            } else {
                mapView.fitRouteWhenLaidOut(bbox, paddingPx = 60, lockScroll = true)
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
) {
    fun applyFit() {
        minZoomLevel = 3.0
        maxZoomLevel = 19.0
        runCatching {
            zoomToBoundingBox(bbox, false, paddingPx)
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

private fun createCircleIcon(
    context: android.content.Context,
    color: Int,
): android.graphics.drawable.BitmapDrawable {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

private fun createCheckeredIcon(context: android.content.Context): android.graphics.drawable.BitmapDrawable {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    val cellSize = size / 4f
    val offset = size / 4f
    paint.color = android.graphics.Color.BLACK
    canvas.drawRect(offset, offset, offset + cellSize, offset + cellSize, paint)
    canvas.drawRect(offset + cellSize, offset + cellSize, offset + 2 * cellSize, offset + 2 * cellSize, paint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}
