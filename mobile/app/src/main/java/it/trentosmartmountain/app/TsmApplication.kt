package it.trentosmartmountain.app

import android.app.Application
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.TsmApiClient

/** Entry point applicazione: inizializza il client HTTP con accesso al JWT locale. */
class TsmApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    TsmApiClient.init(TokenStorage(this))
  }
}
