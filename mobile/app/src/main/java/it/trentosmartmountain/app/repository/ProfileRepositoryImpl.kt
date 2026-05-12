package it.trentosmartmountain.app.repository

import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiService
import java.io.IOException

/** Profilo minimo: username dell’utente loggato tramite JWT e `GET /users/{id}`. */
class ProfileRepositoryImpl(
  private val api: TsmApiService,
  private val tokenStorage: TokenStorage,
) : ProfileRepository {

  override suspend fun loadCurrentUsername(): ProfileResult {
    val token = tokenStorage.getToken()
    if (token.isNullOrBlank()) {
      return ProfileResult.Failure("Sessione non disponibile.")
    }

    // Il JWT contiene solo userId: serve per chiamare GET /users/{id}.
    val userId = JwtDecoder.userIdFrom(token)
    if (userId.isNullOrBlank()) {
      return ProfileResult.Failure("Token non valido.")
    }

    return try {
      val response = api.getUserById(userId)
      if (response.isSuccessful) {
        val username = response.body()?.username
        if (username.isNullOrBlank()) {
          ProfileResult.Failure("Profilo senza username.")
        } else {
          ProfileResult.Success(username)
        }
      } else {
        ProfileResult.Failure("Impossibile caricare il profilo (${response.code()}).")
      }
    } catch (_: IOException) {
      ProfileResult.Failure(
        "Impossibile raggiungere il server. Verifica che il backend sia avviato e l’URL in BuildConfig.",
      )
    } catch (e: Exception) {
      ProfileResult.Failure(e.message ?: "Errore imprevisto.")
    }
  }
}
