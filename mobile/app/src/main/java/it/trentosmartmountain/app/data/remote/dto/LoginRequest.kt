package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Body JSON atteso da `POST /auth/login` sul backend Express (email + password in chiaro; il server applica bcrypt). */
data class LoginRequest(
  @SerializedName("email")
  val email: String,
  @SerializedName("password")
  val password: String,
)
