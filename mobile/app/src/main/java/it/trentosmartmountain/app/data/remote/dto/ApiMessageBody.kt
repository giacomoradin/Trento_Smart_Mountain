package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Corpo errore tipico del backend (`{ "message": "..." }`) per 401/500. */
data class ApiMessageBody(
  @SerializedName("message")
  val message: String? = null,
)
