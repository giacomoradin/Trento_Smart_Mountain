package it.trentosmartmountain.app.data.remote

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** Payload minimo del JWT emesso dal backend (`userId`, `role`). */
private data class JwtPayload(
  @SerializedName("userId") val userId: String? = null,
)

/** Estrae `userId` dal JWT senza verificare la firma (solo per indirizzare le API protette). */
object JwtDecoder {

  fun userIdFrom(token: String): String? {
    val payloadSegment = token.split(".").getOrNull(1) ?: return null
    return runCatching {
      /** Payload JWT: seconda parte del token, codificata in Base64 URL-safe.*/
      val decoded =
        String(
          Base64.decode(
            payloadSegment,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
          ),
        )
      Gson().fromJson(decoded, JwtPayload::class.java).userId
    }.getOrNull()
  }
}
