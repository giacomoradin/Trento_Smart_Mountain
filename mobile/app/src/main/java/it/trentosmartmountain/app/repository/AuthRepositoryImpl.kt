package it.trentosmartmountain.app.repository

import com.google.gson.Gson
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.TsmApiService
import it.trentosmartmountain.app.data.remote.dto.ApiMessageBody
import it.trentosmartmountain.app.data.remote.dto.LoginRequest
import java.io.IOException

/**
 * Implementazione login: chiama `POST /auth/login` come definito nel backend del team e memorizza il JWT.
 * Nessuna modifica al server: solo consumo dell’API esistente.
 */
class AuthRepositoryImpl(
  private val api: TsmApiService,
  private val gson: Gson,
  private val tokenStorage: TokenStorage,
) : AuthRepository {

  override suspend fun login(email: String, password: String): LoginResult {
    return try {
      val response = api.login(LoginRequest(email = email, password = password))
      if (response.isSuccessful) {
        val body = response.body()
        val accessToken = body?.accessToken ?: body?.token
        if (accessToken != null) {
          // Persistenza access + refresh per AuthInterceptor + TsmAuthenticator.
          // Su server pre-refresh, refreshToken sarà null — l'app continua a
          // funzionare ma senza rotation automatica.
          tokenStorage.saveTokens(
            accessToken = accessToken,
            refreshToken = body?.refreshToken,
            refreshExpiresAtIso = body?.refreshExpiresAt,
          )
          LoginResult.Success
        } else {
          LoginResult.Failure("Risposta senza token.")
        }
      } else {
        val raw = response.errorBody()?.string()
        val parsed =
          raw?.let {
            runCatching { gson.fromJson(it, ApiMessageBody::class.java).message }.getOrNull()
          }
        when (response.code()) {
          403 ->
            LoginResult.EmailNotVerified(
              parsed ?: "Verifica l’email ricevuta dopo la registrazione, poi riprova ad accedere.",
            )
          else -> LoginResult.Failure(parsed ?: "Accesso non riuscito (${response.code()}).")
        }
      }
    } catch (_: IOException) {
      LoginResult.Failure(
        "Impossibile raggiungere il server. Verifica che il backend sia avviato e l’URL in BuildConfig.",
      )
    } catch (e: Exception) {
      LoginResult.Failure(e.message ?: "Errore imprevisto.")
    }
  }
}
