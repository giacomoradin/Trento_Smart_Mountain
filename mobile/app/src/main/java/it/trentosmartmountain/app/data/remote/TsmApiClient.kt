package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Client HTTP condiviso (Retrofit + OkHttp). Base URL da [BuildConfig.BASE_URL]. */
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

  inline fun <reified T> service(): T = retrofit.create(T::class.java)
}
