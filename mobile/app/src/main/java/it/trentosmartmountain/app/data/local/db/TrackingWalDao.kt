package it.trentosmartmountain.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrackingWalDao {

  @Insert
  suspend fun insert(point: TrackingWalEntity): Long

  @Insert
  suspend fun insertAll(points: List<TrackingWalEntity>)

  @Query("SELECT * FROM tracking_wal WHERE track_id = :trackId ORDER BY ts ASC, id ASC")
  suspend fun getAllForTrack(trackId: String): List<TrackingWalEntity>

  @Query("SELECT COUNT(*) FROM tracking_wal WHERE track_id = :trackId")
  suspend fun countForTrack(trackId: String): Int

  @Query("DELETE FROM tracking_wal WHERE track_id = :trackId")
  suspend fun clearForTrack(trackId: String)

  /**
   * Restituisce i track_id distinti presenti nella WAL — utile per il
   * recovery dialog ("trovate N tracciamenti interrotti, vuoi recuperare?").
   */
  @Query("SELECT DISTINCT track_id FROM tracking_wal")
  suspend fun listOrphanTrackIds(): List<String>

  /** Wipe completo — usato al logout. */
  @Query("DELETE FROM tracking_wal")
  suspend fun deleteAll()
}
