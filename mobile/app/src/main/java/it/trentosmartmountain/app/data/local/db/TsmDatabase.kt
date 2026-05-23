package it.trentosmartmountain.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Database Room locale dell'app: cache offline per dati che devono sopravvivere
 * a assenza di rete (es. username profilo dopo il primo fetch).
 */
@Database(
  
  entities = [
    CachedUserProfileEntity::class,
    CompletedActivityEntity::class,
  ],
  // v4: aggiunti retry_count, last_retry_at_ms, remote_id per il sync incrementale
  // (sync libere → POST /activities, sync sessioni → PATCH /complete).
  version = 4,
  exportSchema = false,
)
abstract class TsmDatabase : RoomDatabase() {
  abstract fun profileDao(): ProfileDao
  abstract fun completedActivityDao(): CompletedActivityDao
}
