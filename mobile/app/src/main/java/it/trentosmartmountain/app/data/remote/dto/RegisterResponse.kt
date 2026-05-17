package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Body di `POST /users` (201): messaggio server e utente creato senza `passwordHash`. */
data class RegisterResponse(
  @SerializedName("message") val message: String? = null,
  @SerializedName("user") val user: RegisterUserDto? = null,
)

/** Utente restituito dal backend dopo la registrazione. */
data class RegisterUserDto(
  @SerializedName("_id") val id: String? = null,
  @SerializedName("username") val username: String? = null,
  @SerializedName("email") val email: String? = null,
  @SerializedName("role") val role: String? = null,
)
