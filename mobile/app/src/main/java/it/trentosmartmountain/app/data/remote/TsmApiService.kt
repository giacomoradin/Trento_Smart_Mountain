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
import it.trentosmartmountain.app.data.remote.dto.WeatherForecastResponse
import it.trentosmartmountain.app.data.remote.dto.WeatherLocationsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TsmApiService {

  // ── Auth ──

  @POST("auth/login")
  suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

  @POST("auth/forgot-password")
  suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<ApiMessageBody>

  // ── Users ──

  @POST("users")
  suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

  @POST("users")
  suspend fun registerRifugio(@Body body: RegisterRifugioRequest): Response<RegisterResponse>

  @GET("users/{id}")
  suspend fun getUserById(@Path("id") id: String): Response<UserResponse>

  // ── Sessions ──

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

  @DELETE("api/v1/sessions/{id}")
  suspend fun deleteSession(@Path("id") id: String): Response<ApiMessageBody>

  // Return type ApiMessageBody invece di SessionResponse:
  // Gson deserializza il response body in modo eager nel thread Retrofit.
  // Se il backend restituisce participants.userId come ObjectId raw (non popolato),
  // Gson crashava con "Expected BEGIN_OBJECT but was STRING" (IllegalStateException).
  // saveEdit() non legge il body — ricarica la sessione via getSessionById() — quindi
  // ApiMessageBody (solo { message? }) è il contratto corretto e non crash su nessun JSON.
  @PATCH("api/v1/sessions/{id}")
  suspend fun updateSession(@Path("id") id: String, @Body body: UpdateSessionRequest): Response<ApiMessageBody>

  // Stesso principio: updateSessionStatus non necessita del payload SessionResponse completo.
  // Se si ha bisogno della sessione aggiornata, richiamare getSessionById() dopo.
  @PATCH("api/v1/sessions/{id}/status")
  suspend fun updateSessionStatus(
    @Path("id") id: String,
    @Body body: UpdateSessionStatusRequest,
  ): Response<ApiMessageBody>

  // ── Weather (implementazione di Marco via meteo.report / TINIA) ──

  /**
   * Trova le location meteo più vicine a una coordinata GPS.
   * Backend: GET /weather/locations/nearby?lon=&lat=&maxDistance=&type=&limit=
   *
   * type: "town" per previsioni complete, "poi" per punti di interesse.
   * Il DB deve essere seedato una volta con POST /weather/seed (admin, eseguito dal server).
   */
  @GET("weather/locations/nearby")
  suspend fun getWeatherLocationsNearby(
    @Query("lon") lon: Double,
    @Query("lat") lat: Double,
    @Query("maxDistance") maxDistance: Int? = null,
    @Query("type") type: String? = null,
    @Query("limit") limit: Int? = null,
  ): Response<WeatherLocationsResponse>

  /**
   * Cerca location meteo per nome.
   * Backend: GET /weather/locations/search?q=Trento&type=town
   */
  @GET("weather/locations/search")
  suspend fun searchWeatherLocations(
    @Query("q") query: String,
    @Query("type") type: String? = null,
    @Query("limit") limit: Int? = null,
  ): Response<WeatherLocationsResponse>

  /**
   * Restituisce le previsioni complete per una location (3h + 24h).
   * Backend: GET /weather/forecast/:externalId
   *
   * Cache server-side 1h. Se forceRefresh=true, bypassa la cache.
   * I POI vengono automaticamente risolti alla loro town di riferimento.
   */
  @GET("weather/forecast/{externalId}")
  suspend fun getWeatherForecast(
    @Path("externalId") externalId: String,
    @Query("forceRefresh") forceRefresh: Boolean? = null,
  ): Response<WeatherForecastResponse>
}
