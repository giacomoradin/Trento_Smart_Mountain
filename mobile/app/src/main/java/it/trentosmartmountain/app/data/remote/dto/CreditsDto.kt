package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LevelInfo(
    @SerializedName("lv") val lv: Int,
    @SerializedName("name") val name: String,
    @SerializedName("min") val min: Int,
    @SerializedName("max") val max: Int?,
    @SerializedName("progressPct") val progressPct: Double,
    @SerializedName("creditsToNext") val creditsToNext: Int,
    @SerializedName("next") val next: NextLevelInfo?,
)

data class NextLevelInfo(
    @SerializedName("lv") val lv: Int,
    @SerializedName("name") val name: String,
    @SerializedName("min") val min: Int,
)

data class CreditsResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("level") val level: LevelInfo,
)

data class CreditTransactionResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("source") val source: String,
    @SerializedName("note") val note: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreditHistoryResponse(
    @SerializedName("items") val items: List<CreditTransactionResponse>,
    @SerializedName("hasMore") val hasMore: Boolean,
)
