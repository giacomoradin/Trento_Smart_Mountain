package it.trentosmartmountain.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingEmergencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingEmergencyEntity)

    @Query("SELECT * FROM pending_emergencies ORDER BY createdAtMs ASC")
    suspend fun getAll(): List<PendingEmergencyEntity>

    @Query("DELETE FROM pending_emergencies WHERE idempotencyKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT COUNT(*) FROM pending_emergencies")
    suspend fun count(): Int
}
