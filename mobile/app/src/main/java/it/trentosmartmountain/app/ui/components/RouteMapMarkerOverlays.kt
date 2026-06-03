package it.trentosmartmountain.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Marker inizio (verde) e fine (a scacchi) su MapView OSMdroid. */
object RouteMapMarkerOverlays {

    fun attachStartEndMarkers(mapView: MapView, context: Context, geoPoints: List<GeoPoint>) {
        if (geoPoints.size < 2) return
        mapView.overlays.add(
            Marker(mapView).apply {
                position = geoPoints.first()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createStartIcon(context)
                title = "Inizio"
            },
        )
        mapView.overlays.add(
            Marker(mapView).apply {
                position = geoPoints.last()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createEndIcon(context)
                title = "Fine"
            },
        )
    }

    fun createStartIcon(context: Context): BitmapDrawable =
        createCircleIcon(context, android.graphics.Color.parseColor("#4CAF50"))

    fun createEndIcon(context: Context): BitmapDrawable = createCheckeredIcon(context)

    private fun createCircleIcon(context: Context, color: Int): BitmapDrawable {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createCheckeredIcon(context: Context): BitmapDrawable {
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
        canvas.drawRect(
            offset + cellSize,
            offset + cellSize,
            offset + 2 * cellSize,
            offset + 2 * cellSize,
            paint,
        )
        return BitmapDrawable(context.resources, bitmap)
    }
}
