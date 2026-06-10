package it.trentosmartmountain.app.data.remote

import it.trentosmartmountain.app.data.remote.dto.AccountUpdateRequest
import it.trentosmartmountain.app.data.remote.dto.AccountUpdateResponse
import it.trentosmartmountain.app.data.remote.dto.ActivityResponse
import it.trentosmartmountain.app.data.remote.dto.CreateStoryRequest
import it.trentosmartmountain.app.data.remote.dto.StoriesResponse
import it.trentosmartmountain.app.data.remote.dto.StoryItem
import it.trentosmartmountain.app.data.remote.dto.ApiItemResponse
import it.trentosmartmountain.app.data.remote.dto.ApiListResponse
import it.trentosmartmountain.app.data.remote.dto.ActivityStatsResponse
import it.trentosmartmountain.app.data.remote.dto.ApiMessageBody
import it.trentosmartmountain.app.data.remote.dto.BadgeItem
import it.trentosmartmountain.app.data.remote.dto.BoardListResponse
import it.trentosmartmountain.app.data.remote.dto.WasteSimulationRequest
import it.trentosmartmountain.app.data.remote.dto.WasteSimulationResponse
import it.trentosmartmountain.app.data.remote.dto.BoardPost
import it.trentosmartmountain.app.data.remote.dto.CertificateItem
import it.trentosmartmountain.app.data.remote.dto.CreateBoardPostRequest
import it.trentosmartmountain.app.data.remote.dto.Challenge
import it.trentosmartmountain.app.data.remote.dto.ChallengeDetailResponse
import it.trentosmartmountain.app.data.remote.dto.ChallengeRespondRequest
import it.trentosmartmountain.app.data.remote.dto.ChangePasswordRequest
import it.trentosmartmountain.app.data.remote.dto.ChecklistGenerateRequest
import it.trentosmartmountain.app.data.remote.dto.ChecklistGetResponse
import it.trentosmartmountain.app.data.remote.dto.ChecklistMutationResponse
import it.trentosmartmountain.app.data.remote.dto.CommentListResponse
import it.trentosmartmountain.app.data.remote.dto.CompleteSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreateActivityRequest
import it.trentosmartmountain.app.data.remote.dto.CreateChallengeRequest
import it.trentosmartmountain.app.data.remote.dto.CreateCommentRequest
import it.trentosmartmountain.app.data.remote.dto.CreateCommentResponse
import it.trentosmartmountain.app.data.remote.dto.CreateEmergencyRequest
import it.trentosmartmountain.app.data.remote.dto.CreateSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreditHistoryResponse
import it.trentosmartmountain.app.data.remote.dto.CreditsResponse
import it.trentosmartmountain.app.data.remote.dto.DeleteAccountRequest
import it.trentosmartmountain.app.data.remote.dto.VerifyPasswordRequest
import it.trentosmartmountain.app.data.remote.dto.EmergencyResponse
import it.trentosmartmountain.app.data.remote.dto.Experience
import it.trentosmartmountain.app.data.remote.dto.ExperienceResponse
import it.trentosmartmountain.app.data.remote.dto.FeedResponse
import it.trentosmartmountain.app.data.remote.dto.FollowListResponse
import it.trentosmartmountain.app.data.remote.dto.FollowStatsResponse
import it.trentosmartmountain.app.data.remote.dto.ForgotPasswordRequest
import it.trentosmartmountain.app.data.remote.dto.GoalsResponse
import it.trentosmartmountain.app.data.remote.dto.GoalsUpdateRequest
import it.trentosmartmountain.app.data.remote.dto.HikingStatsResponse
import it.trentosmartmountain.app.data.remote.dto.JoinSessionRequest
import it.trentosmartmountain.app.data.remote.dto.LikeResponse
import it.trentosmartmountain.app.data.remote.dto.LiveLocationsResponse
import it.trentosmartmountain.app.data.remote.dto.LoginRequest
import it.trentosmartmountain.app.data.remote.dto.LoginResponse
import it.trentosmartmountain.app.data.remote.dto.LogoutRequest
import it.trentosmartmountain.app.data.remote.dto.MarkReadResponse
import it.trentosmartmountain.app.data.remote.dto.NotificationsResponse
import it.trentosmartmountain.app.data.remote.dto.NextQuizResponse
import it.trentosmartmountain.app.data.remote.dto.NfcScanRequest
import it.trentosmartmountain.app.data.remote.dto.NfcScanResponse
import it.trentosmartmountain.app.data.remote.dto.NfcTotemResponse
import it.trentosmartmountain.app.data.remote.dto.PatchEmergencyRequest
import it.trentosmartmountain.app.data.remote.dto.PersonalInfo
import it.trentosmartmountain.app.data.remote.dto.PersonalInfoResponse
import it.trentosmartmountain.app.data.remote.dto.PostLiveLocationRequest
import it.trentosmartmountain.app.data.remote.dto.Preferences
import it.trentosmartmountain.app.data.remote.dto.PreferencesResponse
import it.trentosmartmountain.app.data.remote.dto.ProfileCompleteResponse
import it.trentosmartmountain.app.data.remote.dto.PublicUserProfile
import it.trentosmartmountain.app.data.remote.dto.QuizCategoryProgressResponse
import it.trentosmartmountain.app.data.remote.dto.QuizDetailResponse
import it.trentosmartmountain.app.data.remote.dto.QuizListItemResponse
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionRequest
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionResponse
import it.trentosmartmountain.app.data.remote.dto.RefreshRequest
import it.trentosmartmountain.app.data.remote.dto.RefugeDashboardResponse
import it.trentosmartmountain.app.data.remote.dto.RegisterRequest
import it.trentosmartmountain.app.data.remote.dto.RegisterResponse
import it.trentosmartmountain.app.data.remote.dto.RegisterRifugioRequest
import it.trentosmartmountain.app.data.remote.dto.SentieroDettaglioDto
import it.trentosmartmountain.app.data.remote.dto.SentieroListItemDto
import it.trentosmartmountain.app.data.remote.dto.SessionCreatedResponse
import it.trentosmartmountain.app.data.remote.dto.SessionEmergenciesResponse
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.data.remote.dto.ShareRequest
import it.trentosmartmountain.app.data.remote.dto.ShareResponse
import it.trentosmartmountain.app.data.remote.dto.SocialRowResponse
import it.trentosmartmountain.app.data.remote.dto.UnreadCountResponse
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionRequest
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionStatusRequest
import it.trentosmartmountain.app.data.remote.dto.UserResponse
import it.trentosmartmountain.app.data.remote.dto.UserSearchResponse
import it.trentosmartmountain.app.data.remote.dto.WeatherForecastResponse
import it.trentosmartmountain.app.data.remote.dto.WeeklyLeaderboardResponse
import it.trentosmartmountain.app.data.remote.dto.WeatherLocationsResponse
import it.trentosmartmountain.app.data.remote.dto.WeeklyStatsResponse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contratto Retrofit verso il backend Express (auth, utenti, sessioni escursionistiche, meteo).
 *
 * Layer remoto del modello MVVM: i [it.trentosmartmountain.app.repository] incapsulano
 * error handling, cache e mapping verso la UI.
 */
interface TsmApiService {

  // ├ö├Â├ç├ö├Â├ç Auth ├ö├Â├ç├ö├Â├ç

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

  // ├ö├Â├ç├ö├Â├ç Registrazione per ruolo (post-refactor discriminator) ├ö├Â├ç├ö├Â├ç

  @POST("auth/register/hiker")
  suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

  @POST("auth/register/refuge")
  suspend fun registerRifugio(@Body body: RegisterRifugioRequest): Response<RegisterResponse>

  // ├ö├Â├ç├ö├Â├ç Hiker / Refuge ├ö├Â├ç├ö├Â├ç

  @GET("hikers/{id}")
  suspend fun getHikerById(@Path("id") id: String): Response<UserResponse>

  @GET("refuges/{id}")
  suspend fun getRefugeById(@Path("id") id: String): Response<UserResponse>

  /** Dashboard IoT del rifugio loggato (sensori + edge nodes + passaggi, mock). */
  @GET("api/v1/refuge/dashboard")
  suspend fun getRefugeDashboard(): Response<RefugeDashboardResponse>

  /** Simulazione rifiuti & logistica del rifugio (ADR-002, MVP read-only). */
  @POST("api/v1/refuge/waste/simulate")
  suspend fun simulateWaste(
    @Body request: WasteSimulationRequest,
  ): Response<WasteSimulationResponse>

  // ── Bacheca rifugi ──
  /** Feed bacheca consultabile da tutti gli escursionisti. */
  @GET("api/v1/board")
  suspend fun getBoardPosts(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
    @Query("type") type: String? = null,
  ): Response<BoardListResponse>

  /** Post pubblicati dal rifugio loggato. */
  @GET("api/v1/board/mine")
  suspend fun getMyBoardPosts(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<BoardListResponse>

  /** Crea un post in bacheca (solo account rifugio). */
  @POST("api/v1/board")
  suspend fun createBoardPost(@Body body: CreateBoardPostRequest): Response<BoardPost>

  /** Modifica un proprio post della bacheca (autore o admin). */
  @PATCH("api/v1/board/{id}")
  suspend fun updateBoardPost(
    @Path("id") id: String,
    @Body body: CreateBoardPostRequest,
  ): Response<BoardPost>

  /** Elimina un post della bacheca (autore o admin). */
  @DELETE("api/v1/board/{id}")
  suspend fun deleteBoardPost(@Path("id") id: String): Response<ApiMessageBody>

  @GET("users/{id}")
  suspend fun getUserById(@Path("id") id: String): Response<UserResponse>

  // ├ö├Â├ç├ö├Â├ç Sessions ├ö├Â├ç├ö├Â├ç

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

  /** Nasconde una sessione COMPLETED dalla lista "Le mie attività" (hide per-utente). */
  @DELETE("api/v1/sessions/{id}/from-activities")
  suspend fun hideSessionFromActivities(@Path("id") id: String): Response<ApiMessageBody>

  @PATCH("api/v1/sessions/{id}")
  suspend fun updateSession(@Path("id") id: String, @Body body: UpdateSessionRequest): Response<ApiMessageBody>

  @PATCH("api/v1/sessions/{id}/status")
  suspend fun updateSessionStatus(
    @Path("id") id: String,
    @Body body: UpdateSessionStatusRequest,
  ): Response<ApiMessageBody>

  @PATCH("api/v1/sessions/{id}/complete")
  suspend fun completeSession(
    @Path("id") id: String,
    @Body body: CompleteSessionRequest,
  ): Response<ApiMessageBody>

  /** Chiusura forzata della sessione (COMPLETED per tutti). Solo capogruppo. */
  @POST("api/v1/sessions/{id}/close")
  suspend fun closeSession(@Path("id") id: String): Response<ApiMessageBody>

  // Gestione partecipanti (Fase A): approva/rifiuta richieste pending, rimuovi+banna.
  @POST("api/v1/sessions/{id}/participants/{userId}/approve")
  suspend fun approveParticipant(
    @Path("id") id: String,
    @Path("userId") userId: String,
  ): Response<SessionResponse>

  @POST("api/v1/sessions/{id}/participants/{userId}/reject")
  suspend fun rejectParticipant(
    @Path("id") id: String,
    @Path("userId") userId: String,
  ): Response<SessionResponse>

  @DELETE("api/v1/sessions/{id}/participants/{userId}")
  suspend fun removeParticipant(
    @Path("id") id: String,
    @Path("userId") userId: String,
  ): Response<SessionResponse>

  // ── Stories (Fase B/C) ──
  @POST("api/v1/stories")
  suspend fun createStory(@Body body: CreateStoryRequest): Response<StoryItem>

  @GET("api/v1/stories/user/{userId}")
  suspend fun getStoriesByUser(@Path("userId") userId: String): Response<StoriesResponse>

  @POST("api/v1/stories/{id}/view")
  suspend fun markStoryViewed(@Path("id") id: String): Response<ApiMessageBody>

  @DELETE("api/v1/stories/{id}")
  suspend fun deleteStory(@Path("id") id: String): Response<ApiMessageBody>

  // ── Checklist dinamica (US-7) ──

  @GET("api/v1/sessions/{id}/checklist")
  suspend fun getSessionChecklist(@Path("id") id: String): Response<ChecklistGetResponse>

  @POST("api/v1/sessions/{id}/checklist")
  suspend fun generateSessionChecklist(
    @Path("id") id: String,
    @Body body: ChecklistGenerateRequest = ChecklistGenerateRequest(),
  ): Response<ChecklistMutationResponse>

  @PUT("api/v1/sessions/{id}/checklist")
  suspend fun updateSessionChecklist(
    @Path("id") id: String,
    @Body body: ChecklistGenerateRequest = ChecklistGenerateRequest(),
  ): Response<ChecklistMutationResponse>

  // ├ö├Â├ç├ö├Â├ç Realtime Monitoring ├ö├Â├ç├ö├Â├ç

  /** GET /api/v1/sessions/:id/live-locations ├ö├Ñ├å recupera le posizioni di tutti i partecipanti */
  @GET("api/v1/sessions/{id}/live-locations")
  suspend fun getLiveLocations(
    @Path("id") sessionId: String,
    @Query("maxAgeSec") maxAgeSec: Int? = 30,
  ): Response<LiveLocationsResponse>

  @POST("api/v1/sessions/{id}/live-location")
  suspend fun postLiveLocation(
    @Path("id") id: String,
    @Body body: PostLiveLocationRequest,
  ): Response<ApiMessageBody>


  @GET("api/v1/sessions/{id}/emergencies")
  suspend fun getSessionEmergencies(@Path("id") sessionId: String): Response<SessionEmergenciesResponse>

  // ├ö├Â├ç├ö├Â├ç Emergenze SOS ├ö├Â├ç├ö├Â├ç

  @POST("api/v1/emergencies")
  suspend fun createEmergency(@Body body: CreateEmergencyRequest): Response<EmergencyResponse>

  @GET("api/v1/emergencies/{id}")
  suspend fun getEmergency(@Path("id") id: String): Response<EmergencyResponse>

  @PATCH("api/v1/emergencies/{id}")
  suspend fun patchEmergency(
    @Path("id") id: String,
    @Body body: PatchEmergencyRequest,
  ): Response<EmergencyResponse>

  // ├ö├Â├ç├ö├Â├ç Activity (attivitÔö£├í libere senza sessione di gruppo) ├ö├Â├ç├ö├Â├ç

  @POST("api/v1/activities")
  suspend fun createActivity(@Body body: CreateActivityRequest): Response<ActivityResponse>

  @GET("api/v1/activities")
  suspend fun getMyActivities(): Response<List<ActivityResponse>>

  @GET("api/v1/activities/{id}")
  suspend fun getActivityById(@Path("id") id: String): Response<ActivityResponse>

  /**
   * Statistiche aggregate annuali/mensili per l'utente loggato (sessioni completate
   * + attivitÔö£├í libere). L'endpoint Ôö£┬┐ esposto da hikeSessionRoutes (non activityRoutes)
   * perchÔö£┬« aggrega entrambe le sorgenti: vedi backend/src/services/hikeSessionService.js.
   */
  @GET("api/v1/sessions/stats")
  suspend fun getActivityStats(@Query("year") year: Int): Response<ActivityStatsResponse>


  @DELETE("api/v1/activities/{id}")
  suspend fun deleteActivity(@Path("id") id: String): Response<ApiMessageBody>

  // ── Sentieri SAT (modalità "Scegli percorso sulla mappa") ──

  /**
   * GET /api/v1/sentieri → tutti i sentieri (senza percorsoCoordinate, escluse per performance).
   * Sorgente unica per la modalità "Scegli tra i percorsi suggeriti": destinazioni, conteggi,
   * filtri (difficoltà/dislivello/distanza/tempo) e ricerca sono calcolati client-side da questa lista.
   * @param limit numero massimo di sentieri da restituire (default backend 100): usare un valore alto.
   */
  @GET("api/v1/sentieri")
  suspend fun getAllSentieri(
    @Query("limit") limit: Int = 100000,
  ): Response<ApiListResponse<SentieroListItemDto>>

  /** GET /api/v1/sentieri/{codice} → dettaglio sentiero con percorsoCoordinate. */
  @GET("api/v1/sentieri/{codice}")
  suspend fun getSentieroByCodice(
    @Path("codice") codice: String,
  ): Response<ApiItemResponse<SentieroDettaglioDto>>

  // ── Weather (implementazione di Marco via meteo.report / TINIA) ──

  @GET("weather/locations/nearby")
  suspend fun getWeatherLocationsNearby(
    @Query("lon") lon: Double,
    @Query("lat") lat: Double,
    @Query("maxDistance") maxDistance: Int? = null,
    @Query("type") type: String? = null,
    @Query("limit") limit: Int? = null,
  ): Response<WeatherLocationsResponse>

  @GET("weather/locations/search")
  suspend fun searchWeatherLocations(
    @Query("q") query: String,
    @Query("type") type: String? = null,
    @Query("limit") limit: Int? = null,
  ): Response<WeatherLocationsResponse>

  @GET("weather/forecast/{externalId}")
  suspend fun getWeatherForecast(
    @Path("externalId") externalId: String,
    @Query("forceRefresh") forceRefresh: Boolean? = null,
  ): Response<WeatherForecastResponse>

  // ├ö├Â├ç├ö├Â├ç Credits & Level ├ö├Â├ç├ö├Â├ç

  @GET("api/v1/users/me/credits")
  suspend fun getMyCredits(): Response<CreditsResponse>

  @GET("api/v1/users/me/credits/history")
  suspend fun getCreditHistory(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
    @Query("source") source: String? = null,
  ): Response<CreditHistoryResponse>

  // ├ö├Â├ç├ö├Â├ç Quiz ├ö├Â├ç├ö├Â├ç

  @GET("api/v1/quiz/categories")
  suspend fun getQuizCategories(): Response<List<QuizCategoryProgressResponse>>

  @GET("api/v1/quiz/categories/{slug}/quizzes")
  suspend fun getQuizzesByCategory(@Path("slug") slug: String): Response<List<QuizListItemResponse>>

  /** Risolve "Continua ├ö├Ñ├å" della FormazioneScreen al primo quiz non superato. */
  @GET("api/v1/quiz/categories/{slug}/next")
  suspend fun getNextQuizForCategory(@Path("slug") slug: String): Response<NextQuizResponse>

  @GET("api/v1/quiz/{id}")
  suspend fun getQuizDetail(@Path("id") id: String): Response<QuizDetailResponse>

  @POST("api/v1/quiz/{id}/submit")
  suspend fun submitQuiz(
    @Path("id") id: String,
    @Body body: QuizSubmissionRequest,
  ): Response<QuizSubmissionResponse>

  // ├ö├Â├ç├ö├Â├ç NFC ├ö├Â├ç├ö├Â├ç

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

  // ├ö├Â├ç├ö├Â├ç Account management ├ö├Â├ç├ö├Â├ç

  @PATCH("api/v1/users/me")
  suspend fun updateAccount(@Body body: AccountUpdateRequest): Response<AccountUpdateResponse>

  @POST("api/v1/users/change-password")
  suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiMessageBody>

  @POST("api/v1/users/me/verify-password")
  suspend fun verifyPassword(@Body body: VerifyPasswordRequest): Response<ApiMessageBody>

  @retrofit2.http.HTTP(method = "DELETE", path = "api/v1/users/me", hasBody = true)
  @DELETE("api/v1/users/me")
  suspend fun deleteAccount(@Body body: DeleteAccountRequest): Response<ApiMessageBody>

  @PATCH("api/v1/users/me/goals")
  suspend fun updateGoals(@Body body: GoalsUpdateRequest): Response<GoalsResponse>

  // ├ö├Â├ç├ö├Â├ç Profilo v2 ├ö├Â├ç├ö├Â├ç

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

  // ├ö├Â├ç├ö├Â├ç Challenges ├ö├Â├ç├ö├Â├ç

  @GET("api/v1/challenges")
  suspend fun listChallenges(): Response<List<Challenge>>

  @POST("api/v1/challenges")
  suspend fun createChallenge(@Body body: CreateChallengeRequest): Response<Challenge>

  @GET("api/v1/challenges/{id}")
  suspend fun getChallengeDetail(@Path("id") id: String): Response<ChallengeDetailResponse>

  @POST("api/v1/challenges/{id}/respond")
  suspend fun respondToChallenge(
    @Path("id") id: String,
    @Body body: ChallengeRespondRequest,
  ): Response<Challenge>


  @DELETE("api/v1/challenges/{id}")
  suspend fun cancelChallenge(@Path("id") id: String): Response<ApiMessageBody>

  // ├ö├Â├ç├ö├Â├ç Badges + Certificates ├ö├Â├ç├ö├Â├ç

  @GET("api/v1/users/me/badges")
  suspend fun getMyBadges(): Response<List<BadgeItem>>

  @GET("api/v1/users/me/certificates")
  suspend fun getMyCertificates(): Response<List<CertificateItem>>

  // ├ö├Â├ç├ö├Â├ç Social ├ö├ç├Â Sprint 2 (feed, share, like, follow) ├ö├Â├ç├ö├Â├ç

  /**
   * Feed sociale paginato (Activity + HikeSession condivise di chi seguo + me).
   * Server cap: limit max 50, default 20. Vedi `socialService.getFeedForUser`.
   */
  @GET("api/v1/users/me/feed")
  suspend fun getFeed(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<FeedResponse>

  // ├ö├Â├ç├ö├Â├ç Share / Unshare attivitÔö£├í ├ö├Â├ç├ö├Â├ç

  @POST("api/v1/activities/{id}/share")
  suspend fun shareActivity(
    @Path("id") id: String,
    @Body body: ShareRequest,
  ): Response<ShareResponse>

  @DELETE("api/v1/activities/{id}/share")
  suspend fun unshareActivity(@Path("id") id: String): Response<ApiMessageBody>

  @POST("api/v1/sessions/{id}/share")
  suspend fun shareSession(
    @Path("id") id: String,
    @Body body: ShareRequest,
  ): Response<ShareResponse>

  @DELETE("api/v1/sessions/{id}/share")
  suspend fun unshareSession(@Path("id") id: String): Response<ApiMessageBody>

  // ├ö├Â├ç├ö├Â├ç Like / Unlike ├ö├Â├ç├ö├Â├ç

  @POST("api/v1/activities/{id}/like")
  suspend fun likeActivity(@Path("id") id: String): Response<LikeResponse>

  @DELETE("api/v1/activities/{id}/like")
  suspend fun unlikeActivity(@Path("id") id: String): Response<LikeResponse>

  @POST("api/v1/sessions/{id}/like")
  suspend fun likeSession(@Path("id") id: String): Response<LikeResponse>

  @DELETE("api/v1/sessions/{id}/like")
  suspend fun unlikeSession(@Path("id") id: String): Response<LikeResponse>

  // ├ö├Â├ç├ö├Â├ç Follow / Unfollow + stats ├ö├Â├ç├ö├Â├ç

  @POST("api/v1/users/{id}/follow")
  suspend fun followUser(@Path("id") id: String): Response<ApiMessageBody>

  @DELETE("api/v1/users/{id}/follow")
  suspend fun unfollowUser(@Path("id") id: String): Response<ApiMessageBody>

  @GET("api/v1/users/{id}/follow-stats")
  suspend fun getFollowStats(@Path("id") id: String): Response<FollowStatsResponse>

  /** Totali escursionistici ALL-TIME (km/dislivello/uscite/punti) per il profilo. */
  @GET("api/v1/users/{id}/hiking-stats")
  suspend fun getUserHikingStats(@Path("id") id: String): Response<HikingStatsResponse>

  /** Classifica settimanale (rolling 7gg) tra l'utente e i suoi seguiti. */
  @GET("api/v1/users/me/weekly-leaderboard")
  suspend fun getWeeklyLeaderboard(): Response<WeeklyLeaderboardResponse>

  // ── Notifiche social ──
  @GET("api/v1/users/me/notifications")
  suspend fun getNotifications(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<NotificationsResponse>

  /** Conteggio non-letti per il badge sulla campanella (polling leggero). */
  @GET("api/v1/users/me/notifications/unread-count")
  suspend fun getUnreadNotificationsCount(): Response<UnreadCountResponse>

  /** Segna tutte le notifiche come lette (all'apertura del centro notifiche). */
  @POST("api/v1/users/me/notifications/read")
  suspend fun markNotificationsRead(): Response<MarkReadResponse>

  /** Elimina una singola notifica (swipe-to-delete). */
  @DELETE("api/v1/users/me/notifications/{id}")
  suspend fun deleteNotification(@Path("id") id: String): Response<Unit>

  /** Elimina TUTTE le notifiche dell'utente ("Elimina tutte"). */
  @DELETE("api/v1/users/me/notifications")
  suspend fun deleteAllNotifications(): Response<Unit>

  /**
   * Ricerca escursionisti per username (match parziale, case-insensitive).
   * Cuore del flusso "aggiungi amici": ritorna utenti + `isFollowedByMe`.
   * Termine < 2 caratteri → lista vuota (gestito lato server).
   */
  @GET("api/v1/users/search")
  suspend fun searchUsers(
    @Query("q") query: String,
    @Query("limit") limit: Int = 20,
  ): Response<UserSearchResponse>

  /** Follower di un utente qualsiasi (navigazione del grafo sociale dal profilo). */
  @GET("api/v1/users/{id}/followers")
  suspend fun getUserFollowers(
    @Path("id") id: String,
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<FollowListResponse>

  /** Utenti seguiti da un utente qualsiasi. */
  @GET("api/v1/users/{id}/following")
  suspend fun getUserFollowing(
    @Path("id") id: String,
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<FollowListResponse>

  @GET("api/v1/users/me/following")
  suspend fun getMyFollowing(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<FollowListResponse>

  @GET("api/v1/users/me/followers")
  suspend fun getMyFollowers(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<FollowListResponse>

  /**
   * Bacheca pubblica di un utente (post condivisi).
   * Per il viewer == author, ritorna anche i post non condivisi.
   */
  @GET("api/v1/users/{id}/posts")
  suspend fun getUserPosts(
    @Path("id") id: String,
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<FeedResponse>

  /**
   * Profilo pubblico di un hiker (post-privacy gate). Filtrato lato server
   * via `utils/userPrivacy.js`: per viewer "other" rimuove sex/birthDate/peso
   * ma preserva username e `personalInfo.avatarUrl`.
   */
  @GET("hikers/{id}")
  suspend fun getPublicHiker(@Path("id") id: String): Response<PublicUserProfile>

  /**
   * Avatar Row del feed Social: per ogni utente seguito ritorna uno
   * status (live/story/goal/neutral) e i dati derivati. Refresh tipico
   * 30s mentre la tab Social Ôö£┬┐ attiva (lo stato live deve essere fresco).
   */
  @GET("api/v1/users/me/social-row")
  suspend fun getSocialRow(): Response<SocialRowResponse>

  // ├ö├Â├ç├ö├Â├ç Commenti su attivitÔö£├í libere ├ö├Â├ç├ö├Â├ç

  @POST("api/v1/activities/{id}/comments")
  suspend fun addActivityComment(
    @Path("id") id: String,
    @Body body: CreateCommentRequest,
  ): Response<CreateCommentResponse>

  @GET("api/v1/activities/{id}/comments")
  suspend fun getActivityComments(
    @Path("id") id: String,
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<CommentListResponse>

  @DELETE("api/v1/activities/{id}/comments/{cid}")
  suspend fun deleteActivityComment(
    @Path("id") id: String,
    @Path("cid") cid: String,
  ): Response<ApiMessageBody>

  // ├ö├Â├ç├ö├Â├ç Commenti su sessioni di gruppo ├ö├Â├ç├ö├Â├ç

  @POST("api/v1/sessions/{id}/comments")
  suspend fun addSessionComment(
    @Path("id") id: String,
    @Body body: CreateCommentRequest,
  ): Response<CreateCommentResponse>

  @GET("api/v1/sessions/{id}/comments")
  suspend fun getSessionComments(
    @Path("id") id: String,
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
  ): Response<CommentListResponse>

  @DELETE("api/v1/sessions/{id}/comments/{cid}")
  suspend fun deleteSessionComment(
    @Path("id") id: String,
    @Path("cid") cid: String,
  ): Response<ApiMessageBody>
}
