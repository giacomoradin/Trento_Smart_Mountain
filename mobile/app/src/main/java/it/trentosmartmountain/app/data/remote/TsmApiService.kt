package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.data.remote.dto.LoginRequest
import it.trentosmartmountain.app.data.remote.dto.LoginResponse
import it.trentosmartmountain.app.data.remote.dto.RegisterRequest
import it.trentosmartmountain.app.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Contratto API REST verso il backend Node.js (`express`, montato in server su `/auth`, `/users`, …).
 */
interface TsmApiService {
  /** Allinea a `POST /auth/login` nel backend (JWT in body). */
  @POST("auth/login")
  suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

  /** Allinea a `POST /users` nel backend (creazione account). */
  @POST("users")
  suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>
}
