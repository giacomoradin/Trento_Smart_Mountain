package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AccountUpdateRequest(
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
)

data class ChangePasswordRequest(
    @SerializedName("oldPassword") val oldPassword: String,
    @SerializedName("newPassword") val newPassword: String,
)

data class DeleteAccountRequest(
    @SerializedName("password") val password: String,
)

data class VerifyPasswordRequest(
    @SerializedName("password") val password: String,
)

data class WeeklyGoals(
    @SerializedName("km") val km: Int,
    @SerializedName("elevM") val elevM: Int,
    @SerializedName("count") val count: Int,
)

data class GoalsUpdateRequest(
    @SerializedName("km") val km: Int? = null,
    @SerializedName("elevM") val elevM: Int? = null,
    @SerializedName("count") val count: Int? = null,
)

data class GoalsResponse(
    @SerializedName("weeklyGoals") val weeklyGoals: WeeklyGoals,
)

data class AccountUpdateResponse(
    @SerializedName("requiresEmailVerification") val requiresEmailVerification: Boolean,
)

/** Statistiche della settimana ISO corrente — ritorno di GET /users/me/weekly-stats. */
data class WeeklyStatsResponse(
    @SerializedName("weekStart") val weekStart: String,
    @SerializedName("weekEnd") val weekEnd: String,
    @SerializedName("km") val km: Double,
    @SerializedName("elevM") val elevM: Int,
    @SerializedName("count") val count: Int,
)
