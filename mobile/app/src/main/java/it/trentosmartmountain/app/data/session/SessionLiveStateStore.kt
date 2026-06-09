package it.trentosmartmountain.app.data.session

import android.content.Context
import it.trentosmartmountain.app.data.remote.dto.SessionResponse

/**
 * Persistenza per [UserSessionLiveState] (SharedPreferences).
 * Sopravvive al kill dell'app; viene riconciliato con lo status server al refresh liste.
 */
object SessionLiveStateStore {

    private const val PREFS_NAME = "tsm_session_live_state"

    fun getState(context: Context, sessionId: String): UserSessionLiveState {
        val raw = prefs(context).getString(sessionId, null) ?: return UserSessionLiveState.NOT_IN_LIVE
        return runCatching { UserSessionLiveState.valueOf(raw) }
            .getOrDefault(UserSessionLiveState.NOT_IN_LIVE)
    }

    fun setState(context: Context, sessionId: String, state: UserSessionLiveState) {
        prefs(context).edit().putString(sessionId, state.name).apply()
    }

    fun remove(context: Context, sessionId: String) {
        prefs(context).edit().remove(sessionId).apply()
    }

    fun snapshot(context: Context): Map<String, UserSessionLiveState> =
        prefs(context).all.mapNotNull { (key, value) ->
            val state = runCatching { UserSessionLiveState.valueOf(value as String) }.getOrNull()
            state?.let { key to it }
        }.toMap()

    /**
     * Allinea lo stato locale quando il server non è più ACTIVE
     * (es. capogruppo ha premuto Arresta).
     */
    fun reconcileWithSessions(context: Context, sessions: List<SessionResponse>) {
        val editor = prefs(context).edit()
        val knownIds = sessions.map { it._id }.toSet()
        prefs(context).all.keys.forEach { sessionId ->
            if (sessionId !in knownIds) {
                editor.remove(sessionId)
            }
        }
        sessions.forEach { session ->
            val local = getState(context, session._id)
            // Riconciliazione ADR-001: lo stato live LOCALE è solo un intento, la
            // verità è lato server. Lo azzeriamo quando essere "live" non ha più
            // senso: sessione PLANNED (non avviata) o COMPLETED (chiusa dal leader).
            // Così, dopo che il capogruppo ha chiuso, nessun membro resta "live"
            // localmente al re-fetch (niente sessione ghost).
            val shouldClear =
                local in LIVE_STATES &&
                    (session.status == "PLANNED" || session.status == "COMPLETED")
            if (shouldClear) {
                editor.putString(session._id, UserSessionLiveState.NOT_IN_LIVE.name)
            }
        }
        editor.apply()
    }

    private val LIVE_STATES = setOf(
        UserSessionLiveState.IN_GROUP_LIVE,
        UserSessionLiveState.LEFT_LIVE,
        UserSessionLiveState.SOLO_PRACTICE,
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
