package it.trentosmartmountain.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
  entities = [
    CachedUserProfileEntity::class,
    CompletedActivityEntity::class,
  ],
  version = 3,
  exportSchema = false,
)
abstract class TsmDatabase : RoomDatabase() {
  abstract fun profileDao(): ProfileDao
  abstract fun completedActivityDao(): CompletedActivityDao
}
