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

    // ── Stabilità: crash logger globale + persistenza locale ───────────────
    // Le "schermate bianche" segnalate non lasciavano traccia in Logcat col
    // nostro package (erano crash di composizione/thread non loggati). Qui
    // intercettiamo OGNI eccezione non gestita, la logghiamo (tag `TSM-CRASH`),
    // la **persistiamo su file** (così è recuperabile anche dopo il kill del
    // processo — foundation per un crash reporter tipo Crashlytics/Sentry), poi
    // rilanciamo all'handler di default (dialog di sistema + reporter esterni).
    // Al riavvio successivo l'eventuale crash precedente viene riletto e loggato.
    logPreviousCrashIfAny()
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Log.e("TSM-CRASH", "Uncaught on '${thread.name}': ${throwable.message}", throwable)
      persistCrash(thread, throwable)
      previousHandler?.uncaughtException(thread, throwable)
    }

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
    //  2. ✓ Migration esplicite registrate (`TsmMigrations.ALL`, 4→8): coprono il
    //     percorso recente; il fallback resta SOLO come ultima rete di sicurezza.
    //     Ad ogni bump `version` aggiungere la nuova Migration in [TsmMigrations.ALL].
    //  3. Pre-produzione: backup JSON best-effort delle attività con isSynced=0
    //     prima dell'eventuale destructive migration (TODO).
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
        // Overload non-deprecato: `dropAllTables = true` esplicita che il
        // fallback (last-resort) ricrea TUTTE le tabelle.
        .fallbackToDestructiveMigration(dropAllTables = true)
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

  /** File su cui persistiamo l'ultimo crash non gestito (best-effort). */
  private fun crashFile() = java.io.File(filesDir, "last_crash.log")

  /** Scrive timestamp + thread + stack-trace completo del crash su file. */
  private fun persistCrash(thread: Thread, throwable: Throwable) {
    runCatching {
      val sw = java.io.StringWriter()
      throwable.printStackTrace(java.io.PrintWriter(sw))
      val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        .format(java.util.Date())
      crashFile().writeText("[$ts] thread='${thread.name}'\n$sw")
    }
  }

  /**
   * Se al avvio esiste un crash persistito (l'app era stata uccisa da
   * un'eccezione), lo logghiamo con tag dedicato e lo rimuoviamo. Quando si
   * integrerà un crash reporter remoto (Crashlytics/Sentry), QUI è il punto in
   * cui inoltrarlo prima di cancellarlo.
   */
  private fun logPreviousCrashIfAny() {
    runCatching {
      val f = crashFile()
      if (f.exists()) {
        Log.e("TSM-CRASH-PREV", "Crash dalla sessione precedente:\n${f.readText()}")
        f.delete()
      }
    }
  }
}
