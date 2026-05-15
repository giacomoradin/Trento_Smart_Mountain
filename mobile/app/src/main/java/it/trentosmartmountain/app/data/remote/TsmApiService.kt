package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.data.remote.dto.ApiMessageBody
import it.trentosmartmountain.app.data.remote.dto.CreateSessionRequest
import it.trentosmartmountain.app.data.remote.dto.ForgotPasswordRequest
import it.trentosmartmountain.app.data.remote.dto.JoinSessionRequest
import it.trentosmartmountain.app.data.remote.dto.LoginRequest
import it.trentosmartmountain.app.data.remote.dto.LoginResponse
import it.trentosmartmountain.app.data.remote.dto.RegisterRequest
import it.trentosmartmountain.app.data.remote.dto.RegisterResponse
import it.trentosmartmountain.app.data.remote.dto.RegisterRifugioRequest
import it.trentosmartmountain.app.data.remote.dto.SessionCreatedResponse
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionRequest
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionStatusRequest
import it.trentosmartmountain.app.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TsmApiService {
  @POST("auth/login")
  suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

  @POST("users")
  suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

  @POST("users")
  suspend fun registerRifugio(@Body body: RegisterRifugioRequest): Response<RegisterResponse>

  @GET("users/{id}")
  suspend fun getUserById(@Path("id") id: String): Response<UserResponse>

  @POST("auth/forgot-password")
  suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<ApiMessageBody>

  @POST("api/v1/sessions")
  suspend fun createSession(@Body body: CreateSessionRequest): Response<SessionCreatedResponse>

  @GET("api/v1/sessions/my")
  suspend fun getMySessions(): Response<List<SessionResponse>>

  @GET("api/v1/sessions/{id}")
  suspend fun getSessionById(@Path("id") id: String): Response<SessionResponse>

  @POST("api/v1/sessions/join")
  suspend fun joinSession(@Body body: JoinSessionRequest): Response<SessionResponse>

  @POST("api/v1/sessions/{id}/leave")
  suspend fun leaveSession(@Path("id") id: String): Response<ApiMessageBody>

  @PATCH("api/v1/sessions/{id}")
  suspend fun updateSession(@Path("id") id: String, @Body body: UpdateSessionRequest): Response<SessionResponse>

  @PATCH("api/v1/sessions/{id}/status")
  suspend fun updateSessionStatus(
    @Path("id") id: String,
    @Body body: UpdateSessionStatusRequest,
  ): Response<SessionResponse>
}
