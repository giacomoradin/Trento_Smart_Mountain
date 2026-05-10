package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Punto unico per creare Retrofit verso il backend.
 *
 * [BuildConfig.BASE_URL] viene generato da Gradle (default emulatore: `http://10.0.2.2:3000/` → host della macchina di sviluppo).
 * Il client OkHttp logga in BASIC su Logcat (utile in debug); in release si può ridurre o disattivare.
 */
object TsmApiClient {
  private val logging =
    HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

  private val client: OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(logging)
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()

  val retrofit: Retrofit =
    Retrofit.Builder()
      .baseUrl(BuildConfig.BASE_URL)
      .client(client)
      .addConverterFactory(GsonConverterFactory.create())
      .build()

  /** Istanza Retrofit dell’interfaccia API (es. [it.trentosmartmountain.app.data.remote.TsmApiService]). */
  inline fun <reified T> service(): T = retrofit.create(T::class.java)
}
