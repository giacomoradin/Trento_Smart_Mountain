package it.trentosmartmountain.app.data.estimation

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Calcoli ufficiali CAI per tempo di percorrenza e modello matematico TSM per
 * il punteggio escursionistico.
 *
 * Riferimenti:
 *  - Tempo CAI: polinomio sulla pendenza (P = H/M*100), risultato in minuti per km
 *  - Distanza Equivalente: D_eq = D + H/100  (100m dislivello ≡ 1km piano)
 *  - Naismith Time: T_nom = D/4 + H/300  (4 km/h piano + 1h per 300m salita)
 *  - Efficienza: μ = T_nom / T_reale, clipping a [0.8, 1.2]
 *  - Punteggio: P = round(K × D_eq × μ_clip)
 */
object HikeEstimation {

    /** Costante di scalamento punti: 10 punti base per km equivalente. */
    const val SCALING_K: Int = 10

    /**
     * Tempo di percorrenza secondo la formula ufficiale CAI.
     *
     * Pendenza P = (H / M) × 100 dove M è la distanza in METRI.
     * La formula restituisce minuti per km, moltiplicato per i km dà i minuti totali.
     *
     * Branch 1 (P < -10):    discesa ripida — coefficiente quadratico crescente
     * Branch 2 (-10 ≤ P < 10): pianura/lieve pendenza — polinomio di 4° grado
     * Branch 3 (P ≥ 10):     salita ripida — coefficiente lineare dominante
     *
     * @param distanceKm Distanza planimetrica in km
     * @param elevationGainM Dislivello positivo in metri
     * @return Tempo di percorrenza in ore
     */
    fun caiTimeHours(distanceKm: Double, elevationGainM: Int): Double {
        if (distanceKm <= 0.0) return 0.0
        val distanceMeters = distanceKm * 1000.0
        val p = (elevationGainM.toDouble() / distanceMeters) * 100.0

        val minutesPerKm = when {
            p < -10.0 -> 0.0023 * p.pow(2) - 0.6665 * p + 9.664
            p < 10.0 -> 0.00002 * p.pow(4) - 0.0013 * p.pow(3) + 0.0307 * p.pow(2) + 0.3729 * p + 14.123
            else -> -0.0002 * p.pow(2) + 1.5041 * p + 2.1252
        }
        // Clamping minimo difensivo: il polinomio centrale non va mai sotto ~9 min/km
        val safeMinPerKm = minutesPerKm.coerceAtLeast(5.0)
        val totalMinutes = safeMinPerKm * distanceKm
        return totalMinutes / 60.0
    }

    /**
     * Distanza equivalente CAI — normalizza l'orografia trasformando il dislivello
     * in distanza planimetrica. 100m di salita ≡ 1km piano.
     */
    fun equivalentDistance(distanceKm: Double, elevationGainM: Int): Double =
        distanceKm + elevationGainM / 100.0

    /**
     * Tempo nominale Naismith — baseline biomeccanica standard.
     * Velocità in piano: 4 km/h; velocità ascensionale: 300 m/h.
     */
    fun naismithTimeHours(distanceKm: Double, elevationGainM: Int): Double =
        distanceKm / 4.0 + elevationGainM / 300.0

    /**
     * Punteggio stimato in fase di pianificazione (μ = 1.0, nessun tempo reale ancora).
     *
     *   P_stim = round(K × D_eq)
     */
    fun estimatedPoints(distanceKm: Double, elevationGainM: Int, k: Int = SCALING_K): Int =
        (k * equivalentDistance(distanceKm, elevationGainM)).roundToInt()

    /**
     * Punteggio finale post-escursione, pesato sull'efficienza cinematica reale.
     *
     *   μ = T_nom / T_reale
     *   μ_clip = clip(μ, 0.8, 1.2)
     *   P = round(K × D_eq × μ_clip)
     *
     * Il clipping limita derive lineari (discese estreme, pause prolungate).
     */
    fun finalPoints(
        distanceKm: Double,
        elevationGainM: Int,
        actualHours: Double,
        k: Int = SCALING_K,
    ): Int {
        if (actualHours <= 0.0) return estimatedPoints(distanceKm, elevationGainM, k)
        val tNom = naismithTimeHours(distanceKm, elevationGainM)
        val mu = (tNom / actualHours).coerceIn(0.8, 1.2)
        return (k * equivalentDistance(distanceKm, elevationGainM) * mu).roundToInt()
    }

    /** Formatta un tempo in ore come stringa "Xh YYm" (es. "2h 30m"). */
    fun formatHours(hours: Double): String {
        if (hours <= 0.0) return "—"
        val totalMinutes = (hours * 60.0).roundToInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}
