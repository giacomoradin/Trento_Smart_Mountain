package it.trentosmartmountain.app.data.gamification

data class LevelResult(
    val lv: Int,
    val name: String,
    val min: Int,
    val max: Int?,
    val nextLv: Int?,
    val nextName: String?,
    val nextMin: Int?,
    val progressPct: Float,
    val creditsToNext: Int,
)

object LevelCalculator {
    private data class LevelDef(val lv: Int, val name: String, val min: Int, val max: Int?)

    private val LEVELS = listOf(
        LevelDef(1,  "Sentiero",        0,     249),
        LevelDef(2,  "Rifugio",         250,   499),
        LevelDef(3,  "Bivacco",         500,   999),
        LevelDef(4,  "Alpinista",       1000,  1499),
        LevelDef(5,  "Cima",            1500,  2499),
        LevelDef(6,  "Esploratore",     2500,  3999),
        LevelDef(7,  "Veterano",        4000,  5999),
        LevelDef(8,  "Guida Alpina",    6000,  8999),
        LevelDef(9,  "Maestro",         9000,  12999),
        LevelDef(10, "Leggenda Alpina", 13000, null),
    )

    fun compute(credits: Int): LevelResult {
        val current = LEVELS.lastOrNull { credits >= it.min } ?: LEVELS.first()
        // Lookup esplicito per `lv` invece di `LEVELS[current.lv]`: il vecchio approccio si appoggiava
        // all'invariante "lv == index+1" — se domani si rinumerano i livelli o si inserisce un livello
        // intermedio quel codice romperebbe silenziosamente.
        val next = LEVELS.find { it.lv == current.lv + 1 }
        val range = current.max?.let { it - current.min + 1 } ?: 1
        val progress = current.max?.let { ((credits - current.min).toFloat() / range).coerceIn(0f, 1f) } ?: 1f
        return LevelResult(
            lv = current.lv,
            name = current.name,
            min = current.min,
            max = current.max,
            nextLv = next?.lv,
            nextName = next?.name,
            nextMin = next?.min,
            progressPct = progress,
            creditsToNext = next?.let { maxOf(0, it.min - credits) } ?: 0,
        )
    }
}
