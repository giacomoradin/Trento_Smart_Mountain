package it.trentosmartmountain.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formati data sessione: API `YYYY-MM-DD`, UI `dd MMM yyyy` (italiano). */
object SessionDateFormats {
    private val API_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val DISPLAY_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN)
    private val API_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun formatApiFromMillis(millis: Long): String = API_FORMAT.format(Date(millis))

    fun todayApi(): String = API_FORMAT.format(Date())

    /** Data di domani in formato API (`YYYY-MM-DD`), timezone locale del dispositivo. */
    fun tomorrowApi(): String {
        val cal = java.util.Calendar.getInstance(Locale.getDefault())
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        return API_FORMAT.format(cal.time)
    }

    /** Ora di ritrovo predefinita in pianificazione (`HH:mm`). */
    const val DEFAULT_MEETING_TIME = "12:00"

    fun apiDateToMillis(apiDate: String): Long? {
        val raw = apiDate.trim()
        if (!API_PATTERN.matches(raw)) return null
        return try {
            API_FORMAT.parse(raw)!!.time
        } catch (_: Exception) {
            null
        }
    }

    /** Interpreta `HH:mm`; null se formato non valido. */
    fun parseMeetingTime(time: String?): Pair<Int, Int>? {
        val raw = time?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    fun formatDisplayFromApi(apiDate: String?): String {
        val raw = apiDate?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        if (!API_PATTERN.matches(raw)) return raw
        return try {
            DISPLAY_FORMAT.format(API_FORMAT.parse(raw)!!)
        } catch (_: Exception) {
            raw
        }
    }

    /** Converte valore UI o API in `YYYY-MM-DD` per POST/PATCH; null se vuoto. */
    fun toApiOrNull(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return null
        if (API_PATTERN.matches(raw)) return raw
        return try {
            API_FORMAT.format(DISPLAY_FORMAT.parse(raw)!!)
        } catch (_: Exception) {
            null
        }
    }

    fun isTodayApi(apiDate: String?): Boolean {
        val raw = apiDate?.trim().orEmpty()
        if (raw.isEmpty()) return false
        val dateOnly = raw.take(10)
        if (API_PATTERN.matches(dateOnly)) return dateOnly == todayApi()
        return raw == todayApi()
    }
}
