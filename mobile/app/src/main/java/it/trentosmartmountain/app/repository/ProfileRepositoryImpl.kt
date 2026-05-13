package it.trentosmartmountain.app.repository

import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.local.db.CachedUserProfileEntity
import it.trentosmartmountain.app.data.local.db.ProfileDao
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiService
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/** Profilo: prima cache Room, poi `GET /users/{id}`; in caso di errore di rete mantiene i dati in cache. */
class ProfileRepositoryImpl(
  private val api: TsmApiService,
  private val tokenStorage: TokenStorage,
  private val profileDao: ProfileDao,
) : ProfileRepository {

  override fun observeCurrentProfile(): Flow<ProfileObserveState> =
    flow {
      val token = tokenStorage.getToken()
      if (token.isNullOrBlank()) {
        emit(ProfileObserveState(null, false, false, "Sessione non disponibile."))
        return@flow
      }

      val userId = JwtDecoder.userIdFrom(token)
      if (userId.isNullOrBlank()) {
        emit(ProfileObserveState(null, false, false, "Token non valido."))
        return@flow
      }

      val cached =
        withContext(Dispatchers.IO) {
          profileDao.getByUserId(userId)
        }

      emit(
        ProfileObserveState(
          username = cached?.username,
          isRefreshing = true,
          isStale = cached != null,
          errorMessage = null,
        ),
      )

      try {
        val response = withContext(Dispatchers.IO) { api.getUserById(userId) }
        if (response.isSuccessful) {
          val username = response.body()?.username
          if (username.isNullOrBlank()) {
            emit(
              ProfileObserveState(
                username = cached?.username,
                isRefreshing = false,
                isStale = cached != null,
                errorMessage = "Profilo senza username.",
              ),
            )
          } else {
            withContext(Dispatchers.IO) {
              profileDao.upsert(
                CachedUserProfileEntity(
                  userId = userId,
                  username = username,
                  updatedAtEpochMs = System.currentTimeMillis(),
                ),
              )
            }
            emit(
              ProfileObserveState(
                username = username,
                isRefreshing = false,
                isStale = false,
                errorMessage = null,
              ),
            )
          }
        } else {
          emit(
            ProfileObserveState(
              username = cached?.username,
              isRefreshing = false,
              isStale = cached != null,
              errorMessage =
                if (cached != null) {
                  "Impossibile aggiornare dal server (${response.code()}). Mostro l’ultimo dato salvato."
                } else {
                  "Impossibile caricare il profilo (${response.code()})."
                },
            ),
          )
        }
      } catch (_: IOException) {
        emit(
          ProfileObserveState(
            username = cached?.username,
            isRefreshing = false,
            isStale = cached != null,
            errorMessage =
              if (cached != null) {
                "Server non raggiungibile. Mostro l’ultimo dato salvato sul dispositivo."
              } else {
                "Impossibile raggiungere il server. Verifica che il backend sia avviato e l’URL in BuildConfig."
              },
          ),
        )
      } catch (e: Exception) {
        emit(
          ProfileObserveState(
            username = cached?.username,
            isRefreshing = false,
            isStale = cached != null,
            errorMessage = e.message ?: "Errore imprevisto.",
          ),
        )
      }
    }

  override suspend fun clearLocalCache() {
    withContext(Dispatchers.IO) {
      profileDao.deleteAll()
    }
  }
}
