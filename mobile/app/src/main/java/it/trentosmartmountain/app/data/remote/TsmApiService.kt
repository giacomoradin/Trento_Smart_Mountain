package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.data.remote.dto.AccountUpdateRequest
import it.trentosmartmountain.app.data.remote.dto.AccountUpdateResponse
import it.trentosmartmountain.app.data.remote.dto.ActivityResponse
import it.trentosmartmountain.app.data.remote.dto.ActivityStatsResponse
import it.trentosmartmountain.app.data.remote.dto.ApiMessageBody
import it.trentosmartmountain.app.data.remote.dto.ChangePasswordRequest
import it.trentosmartmountain.app.data.remote.dto.CompleteSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreditHistoryResponse
import it.trentosmartmountain.app.data.remote.dto.CreditsResponse
import it.trentosmartmountain.app.data.remote.dto.CreateActivityRequest
import it.trentosmartmountain.app.data.remote.dto.CreateSessionRequest
import it.trentosmartmountain.app.data.remote.dto.DeleteAccountRequest
import it.trentosmartmountain.app.data.remote.dto.ForgotPasswordRequest
import it.trentosmartmountain.app.data.remote.dto.GoalsResponse
import it.trentosmartmountain.app.data.remote.dto.GoalsUpdateRequest
import it.trentosmartmountain.app.data.remote.dto.WeeklyStatsResponse
import it.trentosmartmountain.app.data.remote.dto.Challenge
import it.trentosmartmountain.app.data.remote.dto.ChallengeDetailResponse
import it.trentosmartmountain.app.data.remote.dto.ChallengeRespondRequest
import it.trentosmartmountain.app.data.remote.dto.CreateChallengeRequest
import it.trentosmartmountain.app.data.remote.dto.BadgeItem
import it.trentosmartmountain.app.data.remote.dto.CertificateItem
import it.trentosmartmountain.app.data.remote.dto.JoinSessionRequest
import it.trentosmartmountain.app.data.remote.dto.LoginRequest
import it.trentosmartmountain.app.data.remote.dto.LoginResponse
import it.trentosmartmountain.app.data.remote.dto.LogoutRequest
import it.trentosmartmountain.app.data.remote.dto.RefreshRequest
import it.trentosmartmountain.app.data.remote.dto.NfcScanRequest
import it.trentosmartmountain.app.data.remote.dto.NfcScanResponse
import it.trentosmartmountain.app.data.remote.dto.NextQuizResponse
import it.trentosmartmountain.app.data.remote.dto.PersonalInfo
import it.trentosmartmountain.app.data.remote.dto.PersonalInfoResponse
import it.trentosmartmountain.app.data.remote.dto.Experience
import it.trentosmartmountain.app.data.remote.dto.ExperienceResponse
import it.trentosmartmountain.app.data.remote.dto.Preferences
import it.trentosmartmountain.app.data.remote.dto.PreferencesResponse
import it.trentosmartmountain.app.data.remote.dto.ProfileCompleteResponse
import it.trentosmartmountain.app.data.remote.dto.NfcTotemResponse
import it.trentosmartmountain.app.data.remote.dto.QuizCategoryProgressResponse
import it.trentosmartmountain.app.data.remote.dto.QuizDetailResponse
import it.trentosmartmountain.app.data.remote.dto.QuizListItemResponse
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionRequest
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionResponse
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

  /**
   * Scambia un refresh token con una nuova coppia (access + refresh ruotato).
   * Vedi [it.trentosmartmountain.app.data.remote.TsmAuthenticator] per l'uso
   * trasparente all'interno dell'OkHttp client.
   */
  @POST("auth/refresh")
  suspend fun refresh(@Body body: RefreshRequest): Response<LoginResponse>

  @POST("auth/logout")
  suspend fun logout(@Body body: LogoutRequest): Response<ApiMessageBody>

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

  // ── Activity (attività libere senza sessione di gruppo) ──

  /** Crea una nuova attività libera sul server. Usato dal sync worker dopo il tracking. */
  @POST("api/v1/activities")
  suspend fun createActivity(@Body body: CreateActivityRequest): Response<ActivityResponse>

  /** Lista delle attività libere dell'utente loggato (sync cloud → locale). */
  @GET("api/v1/activities")
  suspend fun getMyActivities(): Response<List<ActivityResponse>>

  /**
   * Statistiche aggregate annuali/mensili per l'utente loggato (sessioni completate
   * + attività libere). L'endpoint è esposto da hikeSessionRoutes (non activityRoutes)
   * perché aggrega entrambe le sorgenti: vedi backend/src/services/hikeSessionService.js.
   */
  @GET("api/v1/sessions/stats")
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

  // ── Credits & Level ──

  @GET("api/v1/users/me/credits")
  suspend fun getMyCredits(): Response<CreditsResponse>

  @GET("api/v1/users/me/credits/history")
  suspend fun getCreditHistory(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
    @Query("source") source: String? = null,
  ): Response<CreditHistoryResponse>

  // ── Quiz ──

  @GET("api/v1/quiz/categories")
  suspend fun getQuizCategories(): Response<List<QuizCategoryProgressResponse>>

  @GET("api/v1/quiz/categories/{slug}/quizzes")
  suspend fun getQuizzesByCategory(@Path("slug") slug: String): Response<List<QuizListItemResponse>>

  /** Risolve "Continua →" della FormazioneScreen al primo quiz non superato. */
  @GET("api/v1/quiz/categories/{slug}/next")
  suspend fun getNextQuizForCategory(@Path("slug") slug: String): Response<NextQuizResponse>

  @GET("api/v1/quiz/{id}")
  suspend fun getQuizDetail(@Path("id") id: String): Response<QuizDetailResponse>

  @POST("api/v1/quiz/{id}/submit")
  suspend fun submitQuiz(
    @Path("id") id: String,
    @Body body: QuizSubmissionRequest,
  ): Response<QuizSubmissionResponse>

  // ── NFC ──

  @GET("api/v1/nfc/totems")
  suspend fun getNfcTotems(
    @Query("lon") lon: Double? = null,
    @Query("lat") lat: Double? = null,
    @Query("maxDistance") maxDistance: Int? = null,
  ): Response<List<NfcTotemResponse>>

  @POST("api/v1/nfc/scan")
  suspend fun scanNfcTotem(@Body body: NfcScanRequest): Response<NfcScanResponse>

  @GET("api/v1/users/me/nfc-history")
  suspend fun getNfcHistory(@Query("page") page: Int = 1): Response<List<NfcScanResponse>>

  // ── Account management ──

  @PATCH("api/v1/users/me")
  suspend fun updateAccount(@Body body: AccountUpdateRequest): Response<AccountUpdateResponse>

  @POST("api/v1/users/change-password")
  suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiMessageBody>

  @DELETE("api/v1/users/me")
  suspend fun deleteAccount(@Body body: DeleteAccountRequest): Response<ApiMessageBody>

  @PATCH("api/v1/users/me/goals")
  suspend fun updateGoals(@Body body: GoalsUpdateRequest): Response<GoalsResponse>

  // ── Profilo v2 ──

  @PATCH("api/v1/users/me/personal-info")
  suspend fun updatePersonalInfo(@Body body: PersonalInfo): Response<PersonalInfoResponse>

  @PATCH("api/v1/users/me/experience")
  suspend fun updateExperience(@Body body: Experience): Response<ExperienceResponse>

  @PATCH("api/v1/users/me/preferences")
  suspend fun updatePreferences(@Body body: Preferences): Response<PreferencesResponse>

  @POST("api/v1/users/me/profile-complete")
  suspend fun markProfileComplete(): Response<ProfileCompleteResponse>

  @GET("api/v1/users/me/weekly-stats")
  suspend fun getWeeklyStats(): Response<WeeklyStatsResponse>

  // ── Challenges ──

  @GET("api/v1/challenges")
  suspend fun listChallenges(): Response<List<Challenge>>

  @POST("api/v1/challenges")
  suspend fun createChallenge(@Body body: CreateChallengeRequest): Response<Challenge>

  @GET("api/v1/challenges/{id}")
  suspend fun getChallengeDetail(@Path("id") id: String): Response<ChallengeDetailResponse>

  @POST("api/v1/challenges/{id}/respond")
  suspend fun respondToChallenge(@Path("id") id: String, @Body body: ChallengeRespondRequest): Response<Challenge>

  @DELETE("api/v1/challenges/{id}")
  suspend fun cancelChallenge(@Path("id") id: String): Response<ApiMessageBody>

  // ── Badges + Certificates ──

  @GET("api/v1/users/me/badges")
  suspend fun getMyBadges(): Response<List<BadgeItem>>

  @GET("api/v1/users/me/certificates")
  suspend fun getMyCertificates(): Response<List<CertificateItem>>
}
