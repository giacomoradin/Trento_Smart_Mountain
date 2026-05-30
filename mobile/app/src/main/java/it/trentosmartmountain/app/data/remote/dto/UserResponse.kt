package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Utente restituito da `GET /users/{id}` (senza password).
 *
 * Per self-view, il server espone anche i campi del profilo v2 (personalInfo,
 * experience, preferences, profileCompletedAt). Per other-view il privacy gate
 * server-side li omette → arrivano `null` al client (vedi utils/userPrivacy.js).
 */
data class UserResponse(
  @SerializedName("_id") val id: String? = null,
  @SerializedName("username") val username: String? = null,
  @SerializedName("email") val email: String? = null,
  @SerializedName("role") val role: String? = null,
  @SerializedName("isVerified") val isVerified: Boolean? = null,
  // Profilo v2 — popolati solo per self-view
  @SerializedName("personalInfo") val personalInfo: PersonalInfo? = null,
  @SerializedName("experience") val experience: Experience? = null,
  @SerializedName("preferences") val preferences: Preferences? = null,
  @SerializedName("profileCompletedAt") val profileCompletedAt: String? = null,
  // weeklyGoals è privato per design (vedi utils/userPrivacy.js): self-view only.
  @SerializedName("weeklyGoals") val weeklyGoals: WeeklyGoals? = null,
)
