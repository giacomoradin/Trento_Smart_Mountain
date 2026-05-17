package it.trentosmartmountain.app.data.remote.dto

data class ActivityStatsResponse(
    val year: Int,
    val totalActivities: Int,
    val totalDistanceKm: Double,
    val totalElevationGainM: Int,
    val totalPoints: Int,
    /** 12 valori (Jan=0 ... Dec=11): numero di attività completate per mese. */
    val monthlyActivityCount: List<Int>,
    /** 12 valori (0.0–1.0): difficoltà media per mese. T=0.25, E=0.5, EE=0.75, EEA=1.0 */
    val monthlyAvgDifficulty: List<Double>,
)
