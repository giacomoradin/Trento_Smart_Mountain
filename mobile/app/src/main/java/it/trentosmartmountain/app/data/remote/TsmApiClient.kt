package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.BuildConfig
import it.trentosmartmountain.app.data.local.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Punto unico per creare Retrofit verso il backend.
 *
 * Va inizializzato da [it.trentosmartmountain.app.TsmApplication] prima di qualsiasi chiamata API.
 */
object TsmApiClient {
  private lateinit var retrofit: Retrofit

  /** Collega [TokenStorage] all’interceptor HTTP; va chiamato una sola volta all’avvio app. */
  fun init(tokenStorage: TokenStorage) {
    retrofit = buildRetrofit(tokenStorage)
  }

  private val logging =
    HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

  private fun buildRetrofit(tokenStorage: TokenStorage): Retrofit {
    // OkHttp: prima il Bearer (se presente), poi log di base per debug.
    val client =
      OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStorage))
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    return Retrofit.Builder()
      .baseUrl(BuildConfig.BASE_URL)
      .client(client)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
  }

  /** Restituisce il contratto Retrofit verso le route Express del backend. */
  fun service(): TsmApiService {
    check(::retrofit.isInitialized) { "TsmApiClient.init() must be called from Application.onCreate" }
    return retrofit.create(TsmApiService::class.java)
  }
}
