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
    TrackingWalEntity::class,
    PendingEmergencyEntity::class,
    ViewedStoryEntity::class,
  ],
  // v4: aggiunti retry_count, last_retry_at_ms, remote_id per il sync incrementale
  //     (sync libere → POST /activities, sync sessioni → PATCH /complete).
  // v5: nuova tabella `tracking_wal` (crash-safety GPS — vedi TrackingWalEntity).
  //     Migration esplicita in TsmMigrations.MIGRATION_4_5 → niente data loss.
  // v6: coda SOS offline (`pending_emergencies`) — merge branch SOS.
  // v7: tabella `viewed_stories` per la priority-row Social (locale-only,
  //     vedi ViewedStoryEntity). Migration esplicita MIGRATION_6_7.
  version = 7,
  exportSchema = false,
)
abstract class TsmDatabase : RoomDatabase() {
  abstract fun profileDao(): ProfileDao
  abstract fun completedActivityDao(): CompletedActivityDao
  abstract fun trackingWalDao(): TrackingWalDao
  abstract fun pendingEmergencyDao(): PendingEmergencyDao
  abstract fun viewedStoryDao(): ViewedStoryDao
}
