package it.trentosmartmountain.app

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.local.db.TsmDatabase
import it.trentosmartmountain.app.data.local.db.TsmMigrations
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.sync.SyncManager
import org.osmdroid.config.Configuration

/**
 * Entry point dell'app Android.
 *
 * Responsabilità principali:
 * - Configurazione globale **OSMdroid** (user-agent e preferenze) per le mappe in [it.trentosmartmountain.app.ui.screens.registra.TsmMapView]
 * - [TokenStorage] per il JWT di sessione
 * - Database Room [TsmDatabase] (profilo utente in cache)
 * - Inizializzazione di [it.trentosmartmountain.app.data.remote.TsmApiClient]
 */
class TsmApplication : Application() {
  lateinit var tokenStorage: TokenStorage
    private set

  lateinit var database: TsmDatabase
    private set

  override fun onCreate() {
    super.onCreate()
    Configuration.getInstance().userAgentValue = packageName
    Configuration.getInstance().load(
      applicationContext,
      getSharedPreferences("osmdroid", MODE_PRIVATE),
    )
    tokenStorage = TokenStorage.getInstance(this)
    // ── Politica migration Room (lesson learned audit 26-05) ────────────────
    // `fallbackToDestructiveMigration` cancella TUTTI i dati locali se la
    // versione dello schema cambia senza una migration esplicita. Per le
    // attività non ancora sincronizzate (es. escursione in montagna senza
    // rete) significa perdita definitiva di dati utente.
    //
    // Mitigazioni applicate qui:
    //  1. `onDestructiveMigration` callback logga il fatto in modo visibile
    //     così, se mai accade in produzione, lo vediamo nei crash reporter.
    //  2. TODO produzione: per ogni bump version aggiungere `.addMigrations(MIGRATION_N_TO_M)`
    //     PRIMA del fallback, in modo che il fallback sia un last-resort.
    //  3. Pre-produzione: implementare backup JSON best-effort delle
    //     attività con isSynced=0 prima del destructive migration.
    database =
      Room.databaseBuilder(
        applicationContext,
        TsmDatabase::class.java,
        "tsm.db",
      )
        // Migration esplicite registrate per prime — Room le preferisce al
        // destructive fallback. Quando si bumpa lo schema, aggiungere la
        // nuova Migration in [TsmMigrations.ALL].
        .addMigrations(*TsmMigrations.ALL)
        .fallbackToDestructiveMigration()
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
          override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            // Log alto profilo: in produzione questo va loggato anche su Sentry/Crashlytics
            // appena disponibili (TODO Sprint 3).
            Log.w(
              "TsmDatabase",
              "⚠ DESTRUCTIVE MIGRATION ESEGUITA — dati locali persi. " +
                "Aggiungere Migration esplicita prima del prossimo bump version.",
            )
          }
        })
        .build()
    TsmApiClient.init(tokenStorage)
    // Avvia il poll loop per il sync delle attività con isSynced=0.
    // Backoff fine (1m → 5m → 30m → 1h) per record. Il loop gira finché il
    // process è vivo; al riavvio dell'app riparte e processa il backlog.
    SyncManager.start(this)
  }
}
