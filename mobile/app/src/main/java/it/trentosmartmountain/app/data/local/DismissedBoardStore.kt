package it.trentosmartmountain.app.data.local

import android.content.Context

/**
 * Persistenza locale degli avvisi di bacheca **rimossi dall'utente** dalla propria
 * vista di consultazione.
 *
 * La bacheca rifugi non è di proprietà dell'utente (i post appartengono ai
 * rifugisti), quindi "eliminare" un avviso lato utente è un **hide per-dispositivo**:
 * memorizziamo gli ID rimossi in SharedPreferences e li filtriamo al caricamento.
 * Nessuna chiamata al backend → niente impatto sugli altri utenti. Stesso spirito
 * dell'eliminazione notifiche, ma senza cancellazione server-side.
 */
object DismissedBoardStore {
    private const val PREFS = "tsm_board_dismissed"
    private const val KEY = "ids"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun dismissed(context: Context): Set<String> =
        prefs(context).getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun dismiss(context: Context, id: String) {
        val cur = dismissed(context).toMutableSet()
        cur.add(id)
        prefs(context).edit().putStringSet(KEY, cur).apply()
    }

    fun dismissAll(context: Context, ids: Collection<String>) {
        val cur = dismissed(context).toMutableSet()
        cur.addAll(ids)
        prefs(context).edit().putStringSet(KEY, cur).apply()
    }

    /** Reset (es. se l'utente volesse rivedere tutti gli avvisi). */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }
}
