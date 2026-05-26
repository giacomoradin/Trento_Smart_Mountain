package it.trentosmartmountain.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrazioni esplicite del database Room.
 *
 * Politica adottata dopo l'audit 2026-05 (vedi anche [TsmApplication.onCreate]):
 *  - Ogni bump della `@Database(version = N)` DEVE essere accompagnato da una
 *    Migration esplicita registrata in [ALL].
 *  - `fallbackToDestructiveMigration()` rimane come ultima rete di sicurezza, ma
 *    è un best-effort che cancella TUTTI i dati locali — incluse le attività
 *    libere con `isSynced=0` non ancora caricate al backend (es. escursione in
 *    montagna senza rete). La migration esplicita evita questa perdita.
 *
 * ## Come aggiungere una nuova migration
 *
 * Esempio: bump da v4 a v5 con aggiunta di una colonna `note: String?` su
 * `completed_activities` (nullable, default NULL).
 *
 * 1. Bumpa `version` in [TsmDatabase].
 * 2. Aggiungi qui sotto:
 *    ```
 *    val MIGRATION_4_5 = object : Migration(4, 5) {
 *      override fun migrate(db: SupportSQLiteDatabase) {
 *        db.execSQL("ALTER TABLE completed_activities ADD COLUMN note TEXT")
 *      }
 *    }
 *    ```
 * 3. Aggiungilo all'array [ALL] sotto.
 * 4. Esporta lo schema (`exportSchema = true`) e committa i JSON in
 *    `app/schemas/` per consentire i test di migration.
 *
 * ## Test di migration (raccomandato dal prossimo bump)
 *
 * Aggiungere `androidx.room:room-testing` come `androidTestImplementation` e
 * scrivere uno smoke test con `MigrationTestHelper` che apre il DB alla
 * versione N-1 con qualche record, applica la migration, e verifica che i
 * record siano ancora presenti e che la nuova colonna sia letta correttamente.
 */
object TsmMigrations {

  /**
   * v4 → v5: aggiunta tabella `tracking_wal` per il Write-Ahead Log dei punti
   * GPS durante un tracking attivo (audit 2026-05).
   *
   * Preserva: `completed_activities` (incluse quelle con isSynced=0) e
   * `cached_user_profile`. Nessun dato utente perso.
   */
  val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `tracking_wal` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          `track_id` TEXT NOT NULL,
          `lat` REAL NOT NULL,
          `lon` REAL NOT NULL,
          `alt` REAL,
          `ts` INTEGER NOT NULL
        )
        """.trimIndent(),
      )
      db.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_tracking_wal_track_id` ON `tracking_wal` (`track_id`)",
      )
    }
  }

  /**
   * Tutte le migration esplicite registrate, in ordine cronologico.
   * Passate a Room via `Room.databaseBuilder(...).addMigrations(*ALL)`.
   */
  val ALL: Array<Migration> = arrayOf(MIGRATION_4_5)
}
