package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NfcTotemResponse(
    @SerializedName("tagId") val tagId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("kind") val kind: String,
    @SerializedName("creditsReward") val creditsReward: Int,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("radius") val radius: Int,
)

data class NfcScanRequest(
    @SerializedName("tagId") val tagId: String,
    @SerializedName("gpsLon") val gpsLon: Double,
    @SerializedName("gpsLat") val gpsLat: Double,
)

data class NfcScanResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("creditsAwarded") val creditsAwarded: Int,
    @SerializedName("alreadyScannedToday") val alreadyScannedToday: Boolean?,
    @SerializedName("distance") val distance: Int,
    @SerializedName("reason") val reason: String?,
    @SerializedName("totem") val totem: NfcTotemResponse?,
    @SerializedName("newTotalCredits") val newTotalCredits: Int?,
)
