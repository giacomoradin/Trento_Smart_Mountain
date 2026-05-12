package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/** Aggiunge `Authorization: Bearer` alle richieste se è presente un JWT salvato. */
class AuthInterceptor(
  private val tokenStorage: TokenStorage,
) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val token = tokenStorage.getToken()
    val request =
      if (token.isNullOrBlank()) {
        chain.request()
      } else {
        chain
          .request()
          .newBuilder()
          .header("Authorization", "Bearer $token")
          .build()
      }
    return chain.proceed(request)
  }
}
