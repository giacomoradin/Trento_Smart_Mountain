package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Body per `POST /auth/register/hiker` (registrazione escursionista).
 * Il ruolo è impostato implicitamente dal backend (discriminator "groupLeader").
 */
data class RegisterRequest(
  @SerializedName("username") val username: String,
  @SerializedName("email") val email: String,
  @SerializedName("password") val password: String,
)
