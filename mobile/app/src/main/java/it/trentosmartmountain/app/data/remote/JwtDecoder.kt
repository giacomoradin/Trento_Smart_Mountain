package it.trentosmartmountain.app.data.remote

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** Payload minimo del JWT emesso dal backend (`userId`, `role`, `exp`). */
private data class JwtPayload(
  @SerializedName("userId") val userId: String? = null,
  @SerializedName("role") val role: String? = null,
  @SerializedName("exp") val exp: Long? = null,
)

/** Estrae `userId` dal JWT senza verificare la firma (solo per indirizzare le API protette). */
object JwtDecoder {

  fun userIdFrom(token: String): String? = decodePayload(token)?.userId

  /** Ruolo account (`rifugio`, `groupLeader`, `admin`, …) come nel backend. */
  fun roleFrom(token: String): String? =
    decodePayload(token)?.role?.trim()?.takeIf { it.isNotEmpty() }

  /** Verifica locale minima: payload leggibile, `userId` presente e `exp` futuro. */
  fun hasValidLocalSession(token: String): Boolean {
    val payload = decodePayload(token) ?: return false
    if (payload.userId.isNullOrBlank()) return false
    val expiresAtSeconds = payload.exp ?: return false
    return expiresAtSeconds > System.currentTimeMillis() / 1000
  }

  /** Decodifica il payload JWT (secondo segmento Base64URL) senza verificare la firma. */
  private fun decodePayload(token: String): JwtPayload? {
    val payloadSegment = token.split(".").getOrNull(1) ?: return null
    return runCatching {
      val decoded =
        String(
          Base64.decode(
            payloadSegment,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
          ),
        )
      Gson().fromJson(decoded, JwtPayload::class.java)
    }.getOrNull()
  }
}
