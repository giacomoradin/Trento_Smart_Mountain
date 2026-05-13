package it.trentosmartmountain.app

import android.app.Application
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.TsmApiClient

/** Entry point applicazione: inizializza il client HTTP con accesso al JWT locale. */
class TsmApplication : Application() {
  lateinit var tokenStorage: TokenStorage
    private set

  override fun onCreate() {
    super.onCreate()
    tokenStorage = TokenStorage.getInstance(this)
    TsmApiClient.init(tokenStorage)
  }
}
