package it.trentosmartmountain.app

import android.app.Application
import androidx.room.Room
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.local.db.TsmDatabase
import it.trentosmartmountain.app.data.remote.TsmApiClient

/** Entry point applicazione: inizializza il client HTTP con accesso al JWT locale. */
class TsmApplication : Application() {
  lateinit var tokenStorage: TokenStorage
    private set

  lateinit var database: TsmDatabase
    private set

  override fun onCreate() {
    super.onCreate()
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
  }
}
