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
