package it.trentosmartmountain.app.ui.screens.home

import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import kotlin.math.cos

/** Punto proiettato in coordinate planari relative (gradi scalati). */
data class ProjectedPoint(val x: Float, val y: Float)

/**
 * Proietta lat/lon in coordinate planari relative al primo punto (equirettangolare locale).
 * Condiviso tra anteprima traccia e export bitmap storie.
 */
fun projectRoutePoints(points: List<RoutePoint>): List<ProjectedPoint> {
    if (points.isEmpty()) return emptyList()
    val meanLat = points.sumOf { it.lat } / points.size
    val k = cos(Math.toRadians(meanLat))
    val refLat = points[0].lat
    val refLon = points[0].lon
    return points.map { p ->
        ProjectedPoint(
            x = ((p.lon - refLon) * k).toFloat(),
            y = (p.lat - refLat).toFloat(),
        )
    }
}
