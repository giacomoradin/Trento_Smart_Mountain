package it.trentosmartmountain.app.data.session

import it.trentosmartmountain.app.data.remote.dto.SessionResponse
/**
 * Azioni primarie mostrate su card Unisciti e schermata dettaglio.
 */
data class SessionParticipationUi(
    val primary: PrimaryAction? = null,
    val showLeaveLive: Boolean = false,
    val statusHint: String? = null,
) {
    enum class PrimaryAction {
        LEADER_START,
        LEADER_STOP,
        JOIN_LIVE,
        SOLO_PRACTICE,
    }
}

object SessionParticipationResolver {

    fun resolve(
        session: SessionResponse,
        isCreator: Boolean,
        localState: UserSessionLiveState,
    ): SessionParticipationUi {
        val isActive = session.status == "ACTIVE"
        val isPlanned = session.status == "PLANNED"

        if (isCreator) {
            return when {
                isPlanned ->
                    SessionParticipationUi(primary = SessionParticipationUi.PrimaryAction.LEADER_START)
                isActive ->
                    SessionParticipationUi(primary = SessionParticipationUi.PrimaryAction.LEADER_STOP)
                else -> SessionParticipationUi()
            }
        }

        if (isActive) {
            return when (localState) {
                UserSessionLiveState.IN_GROUP_LIVE ->
                    SessionParticipationUi(showLeaveLive = true)
                UserSessionLiveState.LEFT_LIVE ->
                    SessionParticipationUi(
                        primary = SessionParticipationUi.PrimaryAction.JOIN_LIVE,
                        statusHint = "Hai lasciato il live. Puoi riunirti.",
                    )
                else ->
                    SessionParticipationUi(
                        primary = SessionParticipationUi.PrimaryAction.JOIN_LIVE,
                        statusHint = "Sessione avviata — unisciti",
                    )
            }
        }

        // Sessione non ancora avviata dal capogruppo: il partecipante può provare il tracciato in locale.
        if (isPlanned) {
            return when (localState) {
                UserSessionLiveState.SOLO_PRACTICE ->
                    SessionParticipationUi(showLeaveLive = true, statusHint = "Prova tracciato in corso")
                UserSessionLiveState.IN_GROUP_LIVE ->
                    SessionParticipationUi(showLeaveLive = true)
                UserSessionLiveState.LEFT_LIVE,
                UserSessionLiveState.NOT_IN_LIVE,
                ->
                    SessionParticipationUi(
                        primary = SessionParticipationUi.PrimaryAction.SOLO_PRACTICE,
                        statusHint = "Sessione non ancora avviata — puoi provare il tracciato",
                    )
            }
        }

        return SessionParticipationUi()
    }
}
