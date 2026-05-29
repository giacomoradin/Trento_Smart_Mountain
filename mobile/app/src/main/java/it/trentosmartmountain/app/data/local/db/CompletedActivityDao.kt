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
    @Query("SELECT * FROM completed_activities WHERE hidden = 0 ORDER BY completed_at DESC")
    fun observeAll(): Flow<List<CompletedActivityEntity>>

    /** Versione sospesa one-shot per quando serve una lettura singola. */
    @Query("SELECT * FROM completed_activities WHERE hidden = 0 ORDER BY completed_at DESC")
    suspend fun getAll(): List<CompletedActivityEntity>

    @Query("SELECT * FROM completed_activities WHERE id = :id")
    suspend fun getById(id: String): CompletedActivityEntity?

    @Query("SELECT * FROM completed_activities WHERE session_id = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): CompletedActivityEntity?

    /**
     * Cerca per ID backend. Necessario perché le attività registrate sul device
     * hanno `id` = UUID locale e `remote_id` = ID backend: il sync deve poterle
     * riconoscere via remote_id per non re-importarle come duplicato.
     * NON filtra `hidden` → è un controllo di esistenza (rispetta i tombstone).
     */
    @Query("SELECT * FROM completed_activities WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): CompletedActivityEntity?

    @Query("SELECT * FROM completed_activities WHERE is_synced = 0")
    suspend fun getUnsynced(): List<CompletedActivityEntity>

    @Query("UPDATE completed_activities SET is_synced = 1, remote_id = :remoteId WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String?)

    /** Variante backward-compatible per le sessioni dove l'id locale = sessionId. */
    @Query("UPDATE completed_activities SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    /**
     * Registra un tentativo di sync fallito: incrementa retry_count e aggiorna il timestamp.
     * Usato dal [SyncWorker] per gestire backoff incrementale.
     */
    @Query("UPDATE completed_activities SET retry_count = retry_count + 1, last_retry_at_ms = :nowMs WHERE id = :id")
    suspend fun bumpRetry(id: String, nowMs: Long)

    @Query("DELETE FROM completed_activities WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Marca l'attività come eliminata (tombstone) senza rimuovere la riga.
     * La riga resta come marcatore così che i controlli di esistenza del sync
     * (getById / getBySessionId) la vedano e NON la re-importino dal backend.
     */
    @Query("UPDATE completed_activities SET hidden = 1 WHERE id = :id")
    suspend fun markHidden(id: String)

    /** Variante per id locale != id: marca anche per sessionId (sessioni di gruppo). */
    @Query("UPDATE completed_activities SET hidden = 1 WHERE session_id = :sessionId")
    suspend fun markHiddenBySessionId(sessionId: String)

    // Wipe completo della tabella. Usato al logout per evitare che un secondo
    // utente sullo stesso device veda le attività dell'utente precedente.
    @Query("DELETE FROM completed_activities")
    suspend fun deleteAll()

    /** Conta le attività per mese (1-12) in un dato anno, sulla base di completed_at. */
    @Query("""
        SELECT strftime('%m', datetime(completed_at/1000, 'unixepoch')) AS month,
               COUNT(*) as count
        FROM completed_activities
        WHERE hidden = 0 AND strftime('%Y', datetime(completed_at/1000, 'unixepoch')) = :year
        GROUP BY month
    """)
    suspend fun getMonthlyCountForYear(year: String): List<MonthCount>
}

data class MonthCount(val month: String, val count: Int)
