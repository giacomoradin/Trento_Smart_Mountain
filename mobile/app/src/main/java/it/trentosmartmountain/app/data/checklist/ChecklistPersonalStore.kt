package it.trentosmartmountain.app.data.checklist

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persistenza locale per stato checklist per-utente (checked, item personali, ordine).
 * Chiave: sessionId + userId — non sincronizzato col backend.
 */
object ChecklistPersonalStore {

    private const val PREFS_NAME = "tsm_checklist_personal"
    private val gson = Gson()

    data class PersonalItem(
        val id: String,
        val text: String,
    )

    data class Snapshot(
        val checkedIds: Set<String> = emptySet(),
        val personalItems: List<PersonalItem> = emptyList(),
        val itemOrder: List<String> = emptyList(),
    )

    fun load(context: Context, sessionId: String, userId: String): Snapshot {
        if (userId.isBlank()) return Snapshot()
        val raw = prefs(context).getString(key(sessionId, userId), null) ?: return Snapshot()
        return runCatching {
            gson.fromJson<Snapshot>(raw, object : TypeToken<Snapshot>() {}.type)
        }.getOrDefault(Snapshot())
    }

    fun save(context: Context, sessionId: String, userId: String, snapshot: Snapshot) {
        if (userId.isBlank()) return
        prefs(context).edit()
            .putString(key(sessionId, userId), gson.toJson(snapshot))
            .apply()
    }

    private fun key(sessionId: String, userId: String) = "${sessionId}_$userId"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
