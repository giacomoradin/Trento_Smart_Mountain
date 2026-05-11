package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Utente creato (201); il backend non espone `passwordHash`. */
data class RegisterResponse(
  @SerializedName("_id") val id: String? = null,
  @SerializedName("username") val username: String? = null,
  @SerializedName("email") val email: String? = null,
  @SerializedName("role") val role: String? = null,
)
