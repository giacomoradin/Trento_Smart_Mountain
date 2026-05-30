package it.trentosmartmountain.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formattazione "tempo trascorso" da timestamp ISO-8601 UTC.
 *
 * Centralizza la logica che era copiata (con micro-differenze) in FeedCard,
 * CommentsBottomSheet, ActivityDetailScreen e altri: stesso parsing tollerante
 * (con o senza millisecondi, con o senza `Z`), due rese a seconda dello spazio.
 *
 * Tutto in UTC in input (il backend serializza sempre in UTC); il delta è
 * calcolato su `System.currentTimeMillis()` locale.
 */
object RelativeTime {

    private val parserNoMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val parserMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parse(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        val trimmed = iso.removeSuffix("Z").take(23)
        return runCatching {
            if (trimmed.length > 19) parserMs.parse(trimmed) else parserNoMs.parse(trimmed.take(19))
        }.getOrNull()
    }

    /**
     * Forma estesa: "ora", "5 min fa", "2 h fa", "3 g fa", altrimenti data dd/MM.
     * Ritorna stringa vuota se [iso] non è parsabile (così la UI può ometterla).
     */
    fun long(iso: String?): String {
        val date = parse(iso) ?: return ""
        return format(date, compact = false)
    }

    /**
     * Forma compatta per spazi stretti: "ora", "5m", "2h", "3g", altrimenti dd/MM.
     * Ritorna "ora" come fallback (usata in liste di commenti dove un vuoto
     * starebbe male accanto allo username).
     */
    fun short(iso: String?): String {
        val date = parse(iso) ?: return "ora"
        return format(date, compact = true)
    }

    private fun format(date: Date, compact: Boolean): String {
        val seconds = (System.currentTimeMillis() - date.time) / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            seconds < 60 -> "ora"
            minutes < 60 -> if (compact) "${minutes}m" else "$minutes min fa"
            hours < 24 -> if (compact) "${hours}h" else "$hours h fa"
            days < 7 -> if (compact) "${days}g" else "$days g fa"
            else -> SimpleDateFormat("dd/MM", Locale.ITALIAN).format(date)
        }
    }
}
