package it.trentosmartmountain.app.data.remote.dto

data class RegisterRifugioRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: String = "rifugio",
    val rifugioDetails: RifugioDetails,
)

data class RifugioDetails(
    val rifugioName: String,
    val caiCode: String?,
    val quota: Int?,
    val posti: Int?,
    val coordinates: String?,
)
