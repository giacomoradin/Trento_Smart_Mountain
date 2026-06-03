package it.trentosmartmountain.app.ui.screens.home.story

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.components.RouteMapMarkerOverlays
import it.trentosmartmountain.app.ui.screens.registra.openTopoMapTileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

/**
 * Render off-screen di MapView OSMdroid per export JPEG.
 * Richiede un [Activity] per allegare la view alla finestra (le tile non caricano altrimenti).
 */
object StoryMapSnapshotter {

    private const val TILE_SETTLE_MS = 1_400L
    private const val CAPTURE_TIMEOUT_MS = 15_000L

    /** Campione luminosità: sotto questa soglia lo snapshot è considerato vuoto/nero. */
    fun isUsableSnapshot(bitmap: Bitmap?): Boolean {
        if (bitmap == null || bitmap.width < 8 || bitmap.height < 8) return false
        val w = bitmap.width
        val h = bitmap.height
        val stepX = max(1, w / 12)
        val stepY = max(1, h / 12)
        var bright = 0
        var total = 0
        var y = stepY / 2
        while (y < h) {
            var x = stepX / 2
            while (x < w) {
                val p = bitmap.getPixel(x, y)
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                if (r + g + b > 40) bright++
                total++
                x += stepX
            }
            y += stepY
        }
        return total > 0 && bright.toFloat() / total > 0.08f
    }

    suspend fun captureScene(
        hostActivity: Activity,
        points: List<RoutePoint>,
        width: Int,
        height: Int,
        lineColorArgb: Int,
        storyScene: Boolean,
    ): Bitmap? =
        capture(
            hostActivity = hostActivity,
            points = points,
            width = width,
            height = height,
            lineColorArgb = lineColorArgb,
            storyScene = storyScene,
        )

    private suspend fun capture(
        hostActivity: Activity,
        points: List<RoutePoint>,
        width: Int,
        height: Int,
        lineColorArgb: Int,
        storyScene: Boolean,
    ): Bitmap? {
        if (points.size < 2 || width <= 0 || height <= 0) return null
        return withContext(Dispatchers.Main) {
            withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val content =
                        hostActivity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                    val container =
                        FrameLayout(hostActivity).apply {
                            layoutParams =
                                ViewGroup.LayoutParams(width, height)
                            translationX = width * 3f
                            alpha = 0f
                        }
                    val mapView =
                        MapView(hostActivity).apply {
                            layoutParams =
                                FrameLayout.LayoutParams(width, height)
                            setTileSource(openTopoMapTileSource())
                            isTilesScaledToDpi = true
                            minZoomLevel = 3.0
                            maxZoomLevel = 19.0
                            setMultiTouchControls(false)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                            isFlingEnabled = false
                            isClickable = false
                            if (storyScene) {
                                setHorizontalMapRepetitionEnabled(true)
                                setVerticalMapRepetitionEnabled(false)
                            }
                        }
                    container.addView(mapView)
                    content.addView(container)

                    val geoPoints = points.map { GeoPoint(it.lat, it.lon) }
                    val polyline =
                        Polyline(mapView).apply {
                            outlinePaint.color = lineColorArgb
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.isAntiAlias = true
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            setPoints(ArrayList(geoPoints))
                        }
                    mapView.overlays.add(polyline)
                    RouteMapMarkerOverlays.attachStartEndMarkers(mapView, hostActivity, geoPoints)
                    val bbox = BoundingBox.fromGeoPoints(geoPoints)

                    fun release() {
                        mapView.onPause()
                        mapView.overlays.clear()
                        content.removeView(container)
                    }

                    cont.invokeOnCancellation { release() }

                    mapView.onResume()
                    mapView.layout(0, 0, width, height)
                    mapView.post {
                        fitRoute(mapView, bbox, storyScene)
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!cont.isActive) {
                                release()
                                return@postDelayed
                            }
                            val bmp =
                                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.rgb(27, 40, 56))
                            mapView.draw(canvas)
                            release()
                            cont.resume(if (isUsableSnapshot(bmp)) bmp else bmp.also { it.recycle(); null })
                        }, TILE_SETTLE_MS)
                    }
                }
            }
        }
    }

    private fun fitRoute(mapView: MapView, bbox: BoundingBox, storyScene: Boolean) {
        val pad = if (storyScene) 72 else 40
        val dynamicPad =
            if (storyScene && mapView.width > 0 && mapView.height > 0) {
                (min(mapView.width, mapView.height) * 0.18f).toInt()
            } else {
                0
            }
        runCatching {
            mapView.zoomToBoundingBox(bbox, false, pad + dynamicPad)
        }
        if (storyScene) {
            runCatching {
                val z = mapView.zoomLevelDouble - 0.5
                mapView.controller.setZoom(z.coerceAtLeast(mapView.minZoomLevel))
            }
        }
        mapView.invalidate()
    }
}
