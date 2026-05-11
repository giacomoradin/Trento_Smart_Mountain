package it.trentosmartmountain.app.data.local

import android.content.Context

/**
 * Persistenza locale del JWT dopo login.
 *
 * Usa [SharedPreferences] private dell’app (semplice e compatibile). Per ambienti di produzione si può
 * migrare a **EncryptedSharedPreferences** (AndroidX Security) senza cambiare il contratto pubblico della classe.
 */
class TokenStorage(context: Context) {

  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

  fun saveToken(token: String) {
    prefs.edit().putString(KEY_JWT, token).apply()
  }

  fun getToken(): String? = prefs.getString(KEY_JWT, null)

  fun clearToken() {
    prefs.edit().remove(KEY_JWT).apply()
  }

  companion object {
    private const val PREFS_FILE = "tsm_auth"
    private const val KEY_JWT = "jwt_access_token"
  }
}
