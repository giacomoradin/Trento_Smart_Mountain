package it.trentosmartmountain.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val ENCRYPTED_PREFS_FILE = "tsm_auth_encrypted"
private const val LEGACY_PREFS_FILE = "tsm_auth"
private const val KEY_JWT = "jwt_access_token"

/**
 * Persistenza locale del JWT dopo login tramite [EncryptedSharedPreferences].
 *
 * Alla prima apertura dopo l’aggiornamento migra un eventuale token salvato nel file legacy in chiaro.
 */
class TokenStorage private constructor(
  context: Context,
) {
  private val appContext = context.applicationContext
  private val prefs: SharedPreferences = createEncryptedPrefs(appContext)

  init {
    migrateLegacyTokenIfNeeded()
  }

  fun saveToken(token: String) {
    prefs.edit().putString(KEY_JWT, token).apply()
  }

  fun getToken(): String? = prefs.getString(KEY_JWT, null)

  fun clearToken() {
    prefs.edit().clear().apply()
  }

  private fun migrateLegacyTokenIfNeeded() {
    if (!getToken().isNullOrBlank()) return

    val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
    val legacyToken = legacyPrefs.getString(KEY_JWT, null) ?: return

    saveToken(legacyToken)
    legacyPrefs.edit().clear().apply()
  }

  companion object {
    @Volatile
    private var instance: TokenStorage? = null

    fun getInstance(context: Context): TokenStorage =
      instance ?: synchronized(this) {
        instance ?: TokenStorage(context.applicationContext).also { instance = it }
      }
  }
}

private fun createEncryptedPrefs(context: Context): SharedPreferences {
  return try {
    val masterKey =
      MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    EncryptedSharedPreferences.create(
      context,
      ENCRYPTED_PREFS_FILE,
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  } catch (error: Exception) {
    Log.e(TokenStorage::class.java.simpleName, "Impossibile aprire EncryptedSharedPreferences", error)
    throw error
  }
}
