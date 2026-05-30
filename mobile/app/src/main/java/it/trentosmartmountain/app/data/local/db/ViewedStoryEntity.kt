package it.trentosmartmountain.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Tabella locale-only: traccia le "stories" della Avatar Row che l'utente
 * ha già visualizzato. Non viene mai sincronizzata con il backend — è una
 * preferenza di UX dell'utente corrente sul SUO device.
 *
 * Logica anello story:
 *  - Server ritorna `status: "story"` per un utente seguito SE c'è
 *    un'Activity/HikeSession con `sharedAt > now - 24h`
 *  - Client filtra: se la story è in `viewed_stories` E `viewedAtMs >
 *    sharedAt - 24h`, allora la nascondi (l'anello diventa "neutral"/"goal")
 *  - Quando l'utente tappa l'anello → markViewed e ri-calcola
 *
 * @PrimaryKey: `(activityRefId, kind)` non disponibile come compound senza
 *              @Entity(primaryKeys=...). Usiamo `activityRefId` come chiave
 *              singola perché gli `_id` MongoDB sono globalmente unici tra
 *              le collection. Conflict resolution: REPLACE (re-mark = update).
 */
@Entity(tableName = "viewed_stories")
data class ViewedStoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "activity_ref_id")
    val activityRefId: String,
    @ColumnInfo(name = "kind") val kind: String,        // "activity" | "session"
    @ColumnInfo(name = "viewed_at_ms") val viewedAtMs: Long,
)

@Dao
interface ViewedStoryDao {

    /**
     * Marca una story come vista. REPLACE on conflict così re-viewing la
     * stessa story aggiorna semplicemente il timestamp (utile per UX
     * "scrolla all'inizio della story → tap" che riapre quella già vista).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markViewed(entity: ViewedStoryEntity)

    /**
     * Restituisce gli `activityRefId` delle stories già viste dopo
     * `sinceMs`. La social-row filtra contro questo set per rimuovere
     * gli anelli "story" duplicati. Soglia tipica: now - 24h.
     */
    @Query("SELECT activity_ref_id FROM viewed_stories WHERE viewed_at_ms > :sinceMs")
    suspend fun getViewedSince(sinceMs: Long): List<String>

    /**
     * GC opzionale: rimuove gli entry più vecchi di `cutoffMs` (es.
     * 7 giorni). Una story non più valida sul server (sharedAt > 24h fa)
     * non sarà mai mostrata come "story" comunque, ma evitiamo growth
     * indefinito della tabella per device che non chiudono mai l'app.
     */
    @Query("DELETE FROM viewed_stories WHERE viewed_at_ms < :cutoffMs")
    suspend fun pruneOlderThan(cutoffMs: Long)
}
