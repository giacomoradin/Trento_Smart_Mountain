package it.trentosmartmountain.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit test di [RelativeTime], il formattatore "tempo trascorso" condiviso da
 * FeedCard e CommentsBottomSheet. Verifichiamo parsing tollerante, fallback e
 * branch temporali senza dipendere dal timezone (le date assolute sono
 * controllate via regex `dd/MM`, non con un valore fisso).
 */
class RelativeTimeTest {

    @Test
    fun `long e vuoto su input null o blank`() {
        assertEquals("", RelativeTime.long(null))
        assertEquals("", RelativeTime.long(""))
    }

    @Test
    fun `short e 'ora' come fallback su null`() {
        assertEquals("ora", RelativeTime.short(null))
    }

    @Test
    fun `input malformato non lancia e degrada al fallback`() {
        assertEquals("", RelativeTime.long("non-una-data"))
        assertEquals("ora", RelativeTime.short("non-una-data"))
    }

    @Test
    fun `data vecchia degrada a data assoluta dd_MM`() {
        // Oltre 7 giorni → formato dd/MM (regex per essere robusti al timezone).
        val datePattern = Regex("""\d{2}/\d{2}""")
        assertTrue(RelativeTime.long("2020-03-14T12:00:00.000Z").matches(datePattern))
        assertTrue(RelativeTime.short("2020-03-14T12:00:00Z").matches(datePattern))
    }

    @Test
    fun `parsa varianti ISO con e senza millisecondi e Z`() {
        val datePattern = Regex("""\d{2}/\d{2}""")
        assertTrue(RelativeTime.long("2020-03-14T12:00:00").matches(datePattern))
        assertTrue(RelativeTime.long("2020-03-14T12:00:00.123Z").matches(datePattern))
    }

    @Test
    fun `pochi minuti fa reso in forma estesa e compatta`() {
        val fiveMinAgo = Instant.now().minusSeconds(5 * 60).toString()
        assertEquals("5 min fa", RelativeTime.long(fiveMinAgo))
        assertEquals("5m", RelativeTime.short(fiveMinAgo))
    }

    @Test
    fun `appena adesso reso come 'ora'`() {
        val justNow = Instant.now().minusSeconds(3).toString()
        assertEquals("ora", RelativeTime.long(justNow))
        assertEquals("ora", RelativeTime.short(justNow))
    }
}
