package it.trentosmartmountain.app.repository

import com.google.gson.Gson
import it.trentosmartmountain.app.data.remote.TsmApiService
import it.trentosmartmountain.app.data.remote.dto.ApiMessageBody
import it.trentosmartmountain.app.data.remote.dto.RegisterRequest
import java.io.IOException

/** Implementazione registrazione: `POST /users` come definito nel backend del team. */
class RegistrationRepositoryImpl(
  private val api: TsmApiService,
  private val gson: Gson,
) : RegistrationRepository {

  override suspend fun register(
    username: String,
    email: String,
    password: String,
  ): RegisterResult {
    return try {
      val response =
        api.register(
          RegisterRequest(
            username = username,
            email = email,
            password = password,
          ),
        )
      if (response.isSuccessful) {
        val body = response.body()
        val email = body?.user?.email?.trim().orEmpty()
        if (email.isEmpty()) {
          RegisterResult.Failure("Risposta senza email utente.")
        } else {
          RegisterResult.Success(
            email = email,
            serverMessage = body?.message?.trim()?.takeIf { it.isNotEmpty() },
          )
        }
      } else {
        val raw = response.errorBody()?.string()
        val parsed =
          raw?.let {
            runCatching { gson.fromJson(it, ApiMessageBody::class.java).message }.getOrNull()
          }
        val fallback =
          when (response.code()) {
            409 -> "Username o email già in uso."
            else -> "Registrazione non riuscita (${response.code()})."
          }
        RegisterResult.Failure(parsed ?: fallback)
      }
    } catch (_: IOException) {
      RegisterResult.Failure(
        "Impossibile raggiungere il server. Verifica che il backend sia avviato e l’URL in BuildConfig.",
      )
    } catch (e: Exception) {
      RegisterResult.Failure(e.message ?: "Errore imprevisto.")
    }
  }
}
