package it.trentosmartmountain.app.data.preferences

/**
 * Helper di formattazione adattivo metric/imperial. Lo state `units` proviene
 * da `Hiker.preferences.units` su backend → si sincronizza via PreferencesHolder
 * a livello di app, quindi qualsiasi schermata leggendo da quel flow vede subito
 * il cambio di unità.
 *
 * NOTA: per ora copre solo i casi base (km/miles, m/ft, kg/lb). Localizzazione
 * di velocità o pressione si aggiungono quando servono.
 */
object UnitsFormatter {

    enum class Units { METRIC, IMPERIAL }

    fun parse(value: String?): Units =
        if (value == "imperial") Units.IMPERIAL else Units.METRIC

    // ── Distanza ──────────────────────────────────────────────────────────

    /** Formatta km in km o miles a seconda di units. Restituisce stringa pronta UI. */
    fun distance(km: Double, units: Units, decimals: Int = 1): String {
        if (units == Units.IMPERIAL) {
            val miles = km * 0.621371
            return "%.${decimals}f mi".format(miles)
        }
        return "%.${decimals}f km".format(km)
    }

    /** Solo la label dell'unità ("km" vs "mi"). Utile per separare valore + suffix. */
    fun distanceUnit(units: Units): String = if (units == Units.IMPERIAL) "mi" else "km"

    // ── Dislivello / altitudine ───────────────────────────────────────────

    fun elevation(meters: Int, units: Units): String {
        if (units == Units.IMPERIAL) {
            val ft = (meters * 3.28084).toInt()
            return "$ft ft"
        }
        return "$meters m"
    }

    fun elevationUnit(units: Units): String = if (units == Units.IMPERIAL) "ft" else "m"

    // ── Peso ──────────────────────────────────────────────────────────────

    fun weight(kg: Double, units: Units, decimals: Int = 1): String {
        if (units == Units.IMPERIAL) {
            val lb = kg * 2.20462
            return "%.${decimals}f lb".format(lb)
        }
        return "%.${decimals}f kg".format(kg)
    }

    fun weightUnit(units: Units): String = if (units == Units.IMPERIAL) "lb" else "kg"
}
