package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Risposta 200 da `POST /auth/login`: JWT firmato dal server. */
data class LoginResponse(
  @SerializedName("token")
  val token: String,
)
