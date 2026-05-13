package it.trentosmartmountain.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
  entities = [CachedUserProfileEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class TsmDatabase : RoomDatabase() {
  abstract fun profileDao(): ProfileDao
}
