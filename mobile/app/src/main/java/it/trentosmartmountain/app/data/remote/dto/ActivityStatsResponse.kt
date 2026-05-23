package it.trentosmartmountain.app.data.remote.dto

/**
 * Statistiche aggregate per l'escursionista (uscite totali, km, metri di dislivello, punti).
 * Include dati per il grafico mensile (count e difficoltà media).
 */
data class ActivityStatsResponse(
    val totalActivities: Int,
    val totalDistanceKm: Double,
    val totalElevationGainM: Int,
    val totalPoints: Int,
    /** Conteggio attività per ogni mese (12 elementi). */
    val monthlyActivityCount: List<Int>,
    /** Difficoltà media per ogni mese (12 elementi). */
    val monthlyAvgDifficulty: List<Double>,
)
