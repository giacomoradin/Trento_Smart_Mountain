package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO sfide social. Combaciano con backend/src/models/challenge.js + service response.
 *
 * Status: PENDING (non ancora startDate) → ACTIVE → COMPLETED, oppure CANCELLED.
 * Metric: distance/elevation/count/points.
 */

data class UserSummary(
    @SerializedName("_id") val id: String?,
    @SerializedName("username") val username: String?,
)

data class ChallengeParticipant(
    @SerializedName("userId") val user: UserSummary?,
    @SerializedName("status") val status: String, // invited/accepted/declined
    @SerializedName("invitedAt") val invitedAt: String?,
    @SerializedName("respondedAt") val respondedAt: String?,
)

data class Challenge(
    @SerializedName("_id") val id: String,
    @SerializedName("creatorId") val creator: UserSummary?,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("metric") val metric: String,
    @SerializedName("targetValue") val targetValue: Double?,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("participants") val participants: List<ChallengeParticipant>,
    @SerializedName("winnerId") val winnerId: String?,
    @SerializedName("closedAt") val closedAt: String?,
)

data class ChallengeProgressItem(
    @SerializedName("userId") val userId: String,
    @SerializedName("value") val value: Double,
    @SerializedName("reachedTarget") val reachedTarget: Boolean,
)

/** Ritorno di GET /challenges/:id. */
data class ChallengeDetailResponse(
    @SerializedName("challenge") val challenge: Challenge,
    @SerializedName("progress") val progress: List<ChallengeProgressItem>,
)

data class CreateChallengeRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("metric") val metric: String,
    @SerializedName("targetValue") val targetValue: Double? = null,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("participantUserIds") val participantUserIds: List<String> = emptyList(),
)

data class ChallengeRespondRequest(
    @SerializedName("accept") val accept: Boolean,
)
