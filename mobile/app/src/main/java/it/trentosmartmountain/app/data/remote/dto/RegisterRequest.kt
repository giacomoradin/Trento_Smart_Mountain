package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Body per `POST /users` (password in chiaro; hash lato server). */
data class RegisterRequest(
  @SerializedName("username") val username: String,
  @SerializedName("email") val email: String,
  @SerializedName("password") val password: String,
)
