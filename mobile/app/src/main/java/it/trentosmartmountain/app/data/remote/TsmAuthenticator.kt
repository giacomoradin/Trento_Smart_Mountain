package it.trentosmartmountain.app.data.remote

import android.util.Log
import com.google.gson.Gson
import it.trentosmartmountain.app.BuildConfig
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.dto.LoginResponse
import it.trentosmartmountain.app.data.remote.dto.RefreshRequest
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

private const val TAG = "TsmAuthenticator"
private const val AUTH_HEADER = "Authorization"
private const val MAX_RETRIES = 1

/**
 * OkHttp [Authenticator] che gestisce trasparentemente la rotation del refresh
 * token quando il backend risponde 401 (access token scaduto).
 *
 * Flusso:
 *  1. Una request va in 401 (JWT scaduto o invalidato).
 *  2. OkHttp chiama [authenticate] passando la response.
 *  3. Leggiamo il refresh token da [TokenStorage], chiamiamo POST /auth/refresh
 *     in modo sincrono (questo metodo è invocato fuori dal main thread da OkHttp).
 *  4. Se il refresh va a buon fine, salviamo la nuova coppia e ricostruiamo la
 *     request con l'header Authorization aggiornato.
 *  5. Se il refresh fallisce (401: refresh scaduto/revocato/reused), cancelliamo
 *     i token locali — la UI riceverà 401 e router-erà al login.
 *
 * Mutua esclusione: se più request in parallelo vanno in 401 (es. all'avvio
 * dell'app), [synchronized] su [refreshLock] garantisce che il refresh venga
 * fatto UNA volta sola. Le altre coroutine, una volta acquisito il lock,
 * trovano il nuovo token già salvato e lo riusano.
 */
class TsmAuthenticator(
  private val tokenStorage: TokenStorage,
  private val gson: Gson = Gson(),
) : Authenticator {

  private val refreshLock = Any()

  // Client interno SENZA Authenticator (evita loop infinito) e SENZA
  // AuthInterceptor (la route /auth/refresh non richiede Bearer).
  private val refreshClient =
    OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .build()

  override fun authenticate(route: Route?, response: Response): Request? {
    // Limita i retry: se siamo già al 2° tentativo, smetti.
    if (responseCount(response) >= MAX_RETRIES + 1) {
      Log.w(TAG, "Max retries raggiunto, abbandono refresh")
      tokenStorage.clearToken()
      return null
    }
    // Non tentare refresh per la route /auth/refresh stessa (defensive,
    // l'Authenticator non viene chiamato per request senza Authorization).
    if (response.request.url.encodedPath.endsWith("/auth/refresh")) {
      return null
    }

    val refreshToken = tokenStorage.getRefreshToken()
    if (refreshToken.isNullOrBlank()) {
      Log.d(TAG, "Nessun refresh token salvato, no-op")
      return null
    }

    synchronized(refreshLock) {
      // Double-check: un'altra coroutine potrebbe aver già fatto il refresh
      // mentre attendevamo il lock. Se il token corrente è diverso da quello
      // della request fallita, riusa direttamente quello nuovo.
      val currentAccess = tokenStorage.getToken()
      val staleAccess = response.request.header(AUTH_HEADER)?.removePrefix("Bearer ")?.trim()
      if (currentAccess != null && currentAccess != staleAccess) {
        Log.d(TAG, "Token già refreshato da altra coroutine, riuso")
        return response.request.newBuilder()
          .header(AUTH_HEADER, "Bearer $currentAccess")
          .build()
      }

      // Fai la POST /auth/refresh in modo sincrono.
      val newAccess = doRefresh(refreshToken)
      if (newAccess == null) {
        Log.w(TAG, "Refresh fallito, clear token (richiesta re-login)")
        tokenStorage.clearToken()
        return null
      }

      return response.request.newBuilder()
        .header(AUTH_HEADER, "Bearer $newAccess")
        .build()
    }
  }

  private fun doRefresh(refreshToken: String): String? {
    val body = gson.toJson(RefreshRequest(refreshToken))
    val request =
      Request.Builder()
        .url("${BuildConfig.BASE_URL.trimEnd('/')}/auth/refresh")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()

    return try {
      refreshClient.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
          Log.w(TAG, "Refresh HTTP ${resp.code}")
          return@use null
        }
        val raw = resp.body?.string() ?: return@use null
        val parsed = gson.fromJson(raw, LoginResponse::class.java)
        val newAccess = parsed.accessToken ?: parsed.token
        if (newAccess.isBlank()) return@use null
        // Salva entrambi: rotation ha emesso nuovo refresh.
        tokenStorage.saveTokens(
          accessToken = newAccess,
          refreshToken = parsed.refreshToken,
          refreshExpiresAtIso = parsed.refreshExpiresAt,
        )
        newAccess
      }
    } catch (e: Exception) {
      Log.e(TAG, "Eccezione durante refresh", e)
      null
    }
  }

  /** Conta quante volte la stessa request è già stata "priorizzata" da Authenticator. */
  private fun responseCount(response: Response): Int {
    var count = 1
    var prior = response.priorResponse
    while (prior != null) {
      count++
      prior = prior.priorResponse
    }
    return count
  }
}
