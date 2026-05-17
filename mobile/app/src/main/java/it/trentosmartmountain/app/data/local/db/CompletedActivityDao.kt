package it.trentosmartmountain.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: CompletedActivityEntity)

    /**
     * Flow reattivo — emette una nuova lista ogni volta che la tabella cambia.
     * Usato da [ActivityListViewModel] per aggiornare la UI automaticamente
     * dopo ogni `upsert` (sia da RegistraViewModel che da syncCompletedSessionsToRoom).
     */
    @Query("SELECT * FROM completed_activities ORDER BY completed_at DESC")
    fun observeAll(): Flow<List<CompletedActivityEntity>>

    /** Versione sospesa one-shot per quando serve una lettura singola. */
    @Query("SELECT * FROM completed_activities ORDER BY completed_at DESC")
    suspend fun getAll(): List<CompletedActivityEntity>

    @Query("SELECT * FROM completed_activities WHERE id = :id")
    suspend fun getById(id: String): CompletedActivityEntity?

    @Query("SELECT * FROM completed_activities WHERE session_id = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): CompletedActivityEntity?

    @Query("SELECT * FROM completed_activities WHERE is_synced = 0")
    suspend fun getUnsynced(): List<CompletedActivityEntity>

    @Query("UPDATE completed_activities SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM completed_activities WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Conta le attività per mese (1-12) in un dato anno, sulla base di completed_at. */
    @Query("""
        SELECT strftime('%m', datetime(completed_at/1000, 'unixepoch')) AS month,
               COUNT(*) as count
        FROM completed_activities
        WHERE strftime('%Y', datetime(completed_at/1000, 'unixepoch')) = :year
        GROUP BY month
    """)
    suspend fun getMonthlyCountForYear(year: String): List<MonthCount>
}

data class MonthCount(val month: String, val count: Int)
