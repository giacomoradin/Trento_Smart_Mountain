package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GeoPointDto(
    @SerializedName("type") val type: String = "Point",
    @SerializedName("coordinates") val coordinates: List<Double>,
)

data class CreateEmergencyRequest(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("emergencyType") val emergencyType: String,
    @SerializedName("coordinates") val coordinates: GeoPointDto,
    @SerializedName("beaconInstanceId") val beaconInstanceId: String,
    @SerializedName("idempotencyKey") val idempotencyKey: String,
    @SerializedName("signature") val signature: String? = null,
)

data class EmergencyProfileSnapshot(
    @SerializedName("displayName") val displayName: String,
    @SerializedName("personalInfo") val personalInfo: PersonalInfo? = null,
    @SerializedName("experience") val experience: Experience? = null,
)

data class EmergencyResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("senderUserId") val senderUserId: UserRefDto? = null,
    @SerializedName("emergencyType") val emergencyType: String,
    @SerializedName("coordinates") val coordinates: GeoPointDto,
    @SerializedName("profileSnapshot") val profileSnapshot: EmergencyProfileSnapshot,
    @SerializedName("status") val status: String,
    @SerializedName("beaconInstanceId") val beaconInstanceId: String,
    @SerializedName("idempotencyKey") val idempotencyKey: String,
    @SerializedName("leaderAckAt") val leaderAckAt: String? = null,
    @SerializedName("sharedAt") val sharedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class SessionEmergenciesResponse(
    @SerializedName("emergencies") val emergencies: List<EmergencyResponse>,
    @SerializedName("isGroupLeader") val isGroupLeader: Boolean,
    @SerializedName("hasUnacked") val hasUnacked: Boolean,
)

data class PatchEmergencyRequest(
    @SerializedName("action") val action: String,
    @SerializedName("reason") val reason: String? = null,
)

/** Riferimento utente popolato dal backend. */
data class UserRefDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
)
