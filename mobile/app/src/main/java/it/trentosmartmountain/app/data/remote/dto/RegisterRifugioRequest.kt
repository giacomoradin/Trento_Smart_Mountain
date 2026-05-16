package it.trentosmartmountain.app.data.remote.dto

/** Body per `POST /users` con ruolo rifugio e metadati struttura (CAI, quota, posti). */
data class RegisterRifugioRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: String = "rifugio",
    val rifugioDetails: RifugioDetails,
)

/** Dettagli anagrafici del rifugio allegati alla registrazione. */
data class RifugioDetails(
    val rifugioName: String,
    val caiCode: String?,
    val quota: Int?,
    val posti: Int?,
    val coordinates: String?,
)
