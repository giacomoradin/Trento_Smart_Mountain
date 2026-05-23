package it.trentosmartmountain.app

import android.app.Application
import androidx.room.Room
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.local.db.TsmDatabase
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
    database =
      Room.databaseBuilder(
        applicationContext,
        TsmDatabase::class.java,
        "tsm.db",
      )
        // In produzione sostituire con Migration esplicite; per sviluppo evita crash su bump versione.
        .fallbackToDestructiveMigration()
        .build()
    TsmApiClient.init(tokenStorage)
    // Avvia il poll loop per il sync delle attività con isSynced=0.
    // Backoff fine (1m → 5m → 30m → 1h) per record. Il loop gira finché il
    // process è vivo; al riavvio dell'app riparte e processa il backlog.
    SyncManager.start(this)
  }
}
