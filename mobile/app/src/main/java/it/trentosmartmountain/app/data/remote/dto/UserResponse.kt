package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Utente restituito da `GET /users/{id}` (senza password). */
data class UserResponse(
  @SerializedName("_id") val id: String? = null,
  @SerializedName("username") val username: String? = null,
  @SerializedName("email") val email: String? = null,
  @SerializedName("role") val role: String? = null,
)
