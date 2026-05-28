package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.data.remote.dto.ActivityResponse
import it.trentosmartmountain.app.data.remote.dto.ActivityStatsResponse
import it.trentosmartmountain.app.data.remote.dto.ApiMessageBody
import it.trentosmartmountain.app.data.remote.dto.CompleteSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreateActivityRequest
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
import it.trentosmartmountain.app.data.remote.dto.LiveLocationsResponse
import it.trentosmartmountain.app.data.remote.dto.PostLiveLocationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contratto Retrofit verso il backend Express (auth, utenti, sessioni escursionistiche, meteo).
 *
 * Layer remoto del modello MVVM: i [it.trentosmartmountain.app.repository] incapsulano
 * error handling, cache e mapping verso la UI.
 */
interface TsmApiService {

  // ── Auth ──

  @POST("auth/login")
  suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

  @POST("auth/forgot-password")
  suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<ApiMessageBody>

  // ── Registrazione per ruolo (post-refactor discriminator) ──
  // Endpoints semantici sotto /auth/register/* preferiti rispetto a /hikers e /refuges
  // perché tengono il flusso "registrazione" raccolto sotto la categoria auth in Swagger.

  /** POST /auth/register/hiker → crea un account escursionista (groupLeader). */
  @POST("auth/register/hiker")
  suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

  /** POST /auth/register/refuge → crea un account rifugio con metadati flat. */
  @POST("auth/register/refuge")
  suspend fun registerRifugio(@Body body: RegisterRifugioRequest): Response<RegisterResponse>

  // ── Hiker (profilo escursionista) ──

  /** GET /hikers/:id → profilo escursionista (richiede JWT). */
  @GET("hikers/{id}")
  suspend fun getHikerById(@Path("id") id: String): Response<UserResponse>

  // ── Refuge (profilo rifugio) ──

  /** GET /refuges/:id → profilo rifugio con metadati struttura. */
  @GET("refuges/{id}")
  suspend fun getRefugeById(@Path("id") id: String): Response<UserResponse>

  /**
   * GET /users/{id} — alias retro-compatibile.
   *
   * Mantenuto temporaneamente per non rompere il [ProfileViewModel] che legge il
   * profilo senza conoscere a priori il ruolo dell'utente. In Sprint 2 verrà
   * sostituito da una chiamata a `/hikers/{id}` o `/refuges/{id}` in base al ruolo
   * decodificato dal JWT. Il backend mantiene un'alias route per smistare.
   */
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

  /**
   * Marca la sessione COMPLETED e persiste le metriche reali del tracking.
   * Usato da [RegistraViewModel.confirmStopTracking] al termine di una sessione live.
   */
  @PATCH("api/v1/sessions/{id}/complete")
  suspend fun completeSession(
    @Path("id") id: String,
    @Body body: CompleteSessionRequest,
  ): Response<ApiMessageBody>

  /** POST /api/v1/sessions/:id/live-location → invia la posizione live dell'utente */
  @POST("api/v1/sessions/{id}/live-location")
  suspend fun postLiveLocation(
    @Path("id") sessionId: String,
    @Body body: PostLiveLocationRequest,
  ): Response<ApiMessageBody>

  /** GET /api/v1/sessions/:id/live-locations → recupera le posizioni di tutti i partecipanti */
  @GET("api/v1/sessions/{id}/live-locations")
  suspend fun getLiveLocations(
    @Path("id") sessionId: String,
    @Query("maxAgeSec") maxAgeSec: Int? = 30,
  ): Response<LiveLocationsResponse>
  // ── Activity (attività libere senza sessione di gruppo) ──

  /** Crea una nuova attività libera sul server. Usato dal sync worker dopo il tracking. */
  @POST("api/v1/activities")
  suspend fun createActivity(@Body body: CreateActivityRequest): Response<ActivityResponse>

  /** Lista delle attività libere dell'utente loggato (sync cloud → locale). */
  @GET("api/v1/activities")
  suspend fun getMyActivities(): Response<List<ActivityResponse>>

  /** Statistiche aggregate annuali/mensili per l'utente loggato. */
  @GET("api/v1/activities/stats")
  suspend fun getActivityStats(@Query("year") year: Int): Response<ActivityStatsResponse>

  /** Elimina un'attività libera. Solo il proprietario è autorizzato (verificato lato server). */
  @DELETE("api/v1/activities/{id}")
  suspend fun deleteActivity(@Path("id") id: String): Response<ApiMessageBody>

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
