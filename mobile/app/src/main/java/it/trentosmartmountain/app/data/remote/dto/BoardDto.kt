package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Risposta lista bacheca (GET /api/v1/board e /board/mine). */
data class BoardListResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("hasMore") val hasMore: Boolean = false,
    @SerializedName("items") val items: List<BoardPost> = emptyList(),
)

/**
 * Post della bacheca rifugi. `type` ∈ {info, avviso, pericolo} → guida colore/icona.
 */
data class BoardPost(
    @SerializedName("_id") val _id: String = "",
    @SerializedName("refugeId") val refugeId: String = "",
    @SerializedName("refugeName") val refugeName: String = "Rifugio",
    @SerializedName("type") val type: String = "info",
    @SerializedName("title") val title: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("validUntil") val validUntil: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

/** Body POST /api/v1/board (creazione post lato rifugista). */
data class CreateBoardPostRequest(
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("validUntil") val validUntil: String? = null,
)
