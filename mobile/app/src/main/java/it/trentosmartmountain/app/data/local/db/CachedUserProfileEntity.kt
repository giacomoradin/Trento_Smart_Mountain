package it.trentosmartmountain.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Snapshot locale del profilo utente dopo `GET /users/{id}`. */
@Entity(tableName = "cached_user_profile")
data class CachedUserProfileEntity(
  @PrimaryKey val userId: String,
  val username: String,
  val updatedAtEpochMs: Long,
)
