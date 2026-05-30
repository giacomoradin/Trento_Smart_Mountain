package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Risposta 200 da `POST /auth/login` o `POST /auth/refresh`.
 *
 * - [token]: alias backward-compat di [accessToken] — su server vecchi
 *   (pre-refresh) è l'unico campo presente.
 * - [accessToken]: JWT firmato, TTL breve (default 15 min server-side).
 * - [refreshToken]: opaque random, TTL 30g, usato per rotation via /auth/refresh.
 *   Può essere null se il server non supporta ancora il flow refresh.
 * - [refreshExpiresAt]: ISO 8601 — solo informativo, NON validare client-side.
 */
data class LoginResponse(
  @SerializedName("token")
  val token: String,
  @SerializedName("accessToken")
  val accessToken: String? = null,
  @SerializedName("refreshToken")
  val refreshToken: String? = null,
  @SerializedName("refreshExpiresAt")
  val refreshExpiresAt: String? = null,
)

/** Body per `POST /auth/refresh`. */
data class RefreshRequest(
  @SerializedName("refreshToken")
  val refreshToken: String,
)

/** Body per `POST /auth/logout`. */
data class LogoutRequest(
  @SerializedName("refreshToken")
  val refreshToken: String?,
)
