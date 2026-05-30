package it.trentosmartmountain.app.data.sentieri

import org.osmdroid.util.GeoPoint

/**
 * Mapper e utility per i sentieri SAT: parsing del campo `percorsoCoordinate`,
 * downsampling delle polyline e normalizzazione della difficoltà sull'enum backend.
 */
object SentieroMappers {

    /** Difficoltà accettate da Mongoose/checklist. Qualsiasi altro valore viene normalizzato. */
    val DIFFICULTY_ENUM = listOf("T", "E", "EE", "EEA")

    /**
     * Converte la stringa `percorsoCoordinate` ("lon,lat lon,lat ...") in una lista di
     * [GeoPoint] OSMdroid.
     *
     * **Attenzione all'ordine**: il backend salva `lon,lat`, mentre `GeoPoint` vuole `(lat, lon)`.
     * Eventuali coppie malformate vengono ignorate. Se [maxPoints] > 0, applica il downsampling.
     */
    fun parsePercorsoToGeoPoints(raw: String?, maxPoints: Int = 1000): List<GeoPoint> {
        if (raw.isNullOrBlank()) return emptyList()
        val points = raw.trim().split(Regex("\\s+")).mapNotNull { pair ->
            val parts = pair.split(",")
            if (parts.size < 2) return@mapNotNull null
            val lon = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val lat = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            GeoPoint(lat, lon)
        }
        return if (maxPoints > 0) downsampleGeo(points, maxPoints) else points
    }

    /**
     * Campiona uniformemente la lista mantenendo primo e ultimo punto.
     * Evita lag in Compose/OSMdroid quando la polyline ha molte migliaia di punti.
     */
    fun <T> downsample(points: List<T>, maxPoints: Int): List<T> {
        if (maxPoints <= 0 || points.size <= maxPoints) return points
        val step = (points.size - 1).toDouble() / (maxPoints - 1)
        return (0 until maxPoints).map { i ->
            points[(i * step).toInt().coerceAtMost(points.size - 1)]
        }
    }

    private fun downsampleGeo(points: List<GeoPoint>, maxPoints: Int): List<GeoPoint> =
        downsample(points, maxPoints)

    /**
     * Normalizza la difficoltà del sentiero DB sull'enum `["T","E","EE","EEA"]`.
     * I valori non riconosciuti ricadono su "E" (escursionistico), il default più sicuro
     * per non far rifiutare il documento dalla validazione Mongoose.
     */
    fun normalizeDifficolta(raw: String?): String {
        val normalized = raw?.trim()?.uppercase().orEmpty()
        return when (normalized) {
            "T", "E", "EE", "EEA" -> normalized
            "EAA" -> "EEA" // tollera refusi comuni
            else -> "E"
        }
    }

    /** Bounding box [minLon, minLat, maxLon, maxLat] dei punti, o null se vuoto. */
    fun boundingBox(points: List<GeoPoint>): List<Double>? {
        if (points.isEmpty()) return null
        var minLon = Double.MAX_VALUE
        var minLat = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        points.forEach { p ->
            if (p.longitude < minLon) minLon = p.longitude
            if (p.longitude > maxLon) maxLon = p.longitude
            if (p.latitude < minLat) minLat = p.latitude
            if (p.latitude > maxLat) maxLat = p.latitude
        }
        return listOf(minLon, minLat, maxLon, maxLat)
    }
}
