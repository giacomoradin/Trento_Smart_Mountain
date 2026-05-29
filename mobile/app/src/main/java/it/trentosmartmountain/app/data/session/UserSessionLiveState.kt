package it.trentosmartmountain.app.data.session

/**
 * Stato locale per sessione: traccia se l'utente partecipa al live GPS di gruppo,
 * ha lasciato il live senza uscire dalla sessione, o sta facendo una prova tracciato.
 */
enum class UserSessionLiveState {
    NOT_IN_LIVE,
    IN_GROUP_LIVE,
    LEFT_LIVE,
    SOLO_PRACTICE,
}
