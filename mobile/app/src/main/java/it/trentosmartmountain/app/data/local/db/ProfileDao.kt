package it.trentosmartmountain.app.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * Accesso Room alla cache del profilo utente (layer dati offline-first).
 *
 * I repository leggono prima da qui e aggiornano dopo le chiamate di rete.
 */
@Dao
interface ProfileDao {
  @Query("SELECT * FROM cached_user_profile WHERE userId = :userId LIMIT 1")
  suspend fun getByUserId(userId: String): CachedUserProfileEntity?

  @Upsert
  suspend fun upsert(profile: CachedUserProfileEntity)

  @Query("DELETE FROM cached_user_profile")
  suspend fun deleteAll()
}
