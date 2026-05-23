package it.trentosmartmountain.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Database Room locale dell'app: cache offline per dati che devono sopravvivere
 * a assenza di rete (es. username profilo dopo il primo fetch).
 */
@Database(
<<<<<<< HEAD
  entities = [CachedUserProfileEntity::class],
  version = 1,
=======
  entities = [
    CachedUserProfileEntity::class,
    CompletedActivityEntity::class,
  ],
  // v4: aggiunti retry_count, last_retry_at_ms, remote_id per il sync incrementale
  // (sync libere → POST /activities, sync sessioni → PATCH /complete).
  version = 4,
>>>>>>> 7c170be742c0ca0f16c4c6df6f5c273d643d4a7a
  exportSchema = false,
)
abstract class TsmDatabase : RoomDatabase() {
  abstract fun profileDao(): ProfileDao
}
