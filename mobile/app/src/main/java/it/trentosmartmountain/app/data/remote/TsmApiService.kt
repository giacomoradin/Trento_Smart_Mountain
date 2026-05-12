package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.data.remote.dto.LoginRequest
import it.trentosmartmountain.app.data.remote.dto.LoginResponse
import it.trentosmartmountain.app.data.remote.dto.RegisterRequest
import it.trentosmartmountain.app.data.remote.dto.RegisterResponse
import it.trentosmartmountain.app.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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

  /** Allinea a `GET /users/{id}` (endpoint protetto). */
  @GET("users/{id}")
  suspend fun getUserById(@Path("id") id: String): Response<UserResponse>
}
