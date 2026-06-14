package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Corpo errore tipico del backend.
 *
 * Due shape possibili:
 *  - route/service: `{ "message": "..." }` (401/403/404/409/500);
 *  - rate limiter:  `{ "error": "...", "retryAfter": <sec> }` (429).
 *
 * Esponiamo entrambi così il client può mostrare un messaggio sensato anche
 * quando scatta il limiter (prima il 429 diventava un generico "Accesso non
 * riuscito (429)" e l'utente non capiva di dover solo aspettare).
 */
data class ApiMessageBody(
  @SerializedName("message")
  val message: String? = null,
  @SerializedName("error")
  val error: String? = null,
  /** Secondi di attesa suggeriti dal rate limiter (solo 429). */
  @SerializedName("retryAfter")
  val retryAfter: Int? = null,
)
