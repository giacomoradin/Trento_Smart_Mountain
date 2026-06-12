package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Risposta di GET /api/v1/refuge/dashboard — dati (mock) della Dashboard IoT
 * del rifugio: sensori ambientali, edge nodes BLE-mesh, passaggi di oggi.
 */
data class RefugeDashboardResponse(
    @SerializedName("refuge") val refuge: RefugeInfoDto = RefugeInfoDto(),
    @SerializedName("live") val live: Boolean = false,
    @SerializedName("sensors") val sensors: RefugeSensorsDto? = null,
    @SerializedName("edgeNodes") val edgeNodes: List<EdgeNodeDto> = emptyList(),
    @SerializedName("edgeNodesOnline") val edgeNodesOnline: Int = 0,
    @SerializedName("edgeNodesTotal") val edgeNodesTotal: Int = 0,
    @SerializedName("passages") val passages: RefugePassagesDto = RefugePassagesDto(),
)

data class RefugeInfoDto(
    @SerializedName("name") val name: String = "Rifugio",
    @SerializedName("altitudeM") val altitudeM: Int? = null,
    @SerializedName("caiCode") val caiCode: String? = null,
    @SerializedName("posti") val posti: Int? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("verified") val verified: Boolean = false,
    /** Foto della struttura (data URI Base64), null se non impostata. */
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

/** Body per PATCH /api/v1/refuge/profile (foto struttura; "" per rimuoverla). */
data class RefugeProfileUpdateRequest(
    @SerializedName("avatarUrl") val avatarUrl: String?,
)

/** Risposta di PATCH /api/v1/refuge/profile. */
data class RefugeProfileUpdateResponse(
    @SerializedName("name") val name: String = "Rifugio",
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

data class RefugeSensorsDto(
    @SerializedName("temperature") val temperature: SensorMetricDto? = null,
    @SerializedName("humidity") val humidity: SensorMetricDto? = null,
    @SerializedName("wind") val wind: WindMetricDto? = null,
    @SerializedName("pressure") val pressure: SensorMetricDto? = null,
    @SerializedName("capturedAt") val capturedAt: String? = null,
)

data class SensorMetricDto(
    @SerializedName("value") val value: Double? = null,
    @SerializedName("trend") val trend: Double? = null,
)

data class WindMetricDto(
    @SerializedName("value") val value: Double? = null,
    @SerializedName("dir") val dir: String? = null,
    @SerializedName("gust") val gust: Double? = null,
)

data class EdgeNodeDto(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("signalPct") val signalPct: Int = 0,
    @SerializedName("online") val online: Boolean = false,
    @SerializedName("lastSeenAt") val lastSeenAt: String? = null,
)

data class RefugePassagesDto(
    @SerializedName("totalCreditsToday") val totalCreditsToday: Int = 0,
    @SerializedName("items") val items: List<PassageDto> = emptyList(),
)

data class PassageDto(
    @SerializedName("displayName") val displayName: String = "",
    @SerializedName("via") val via: String? = null,
    @SerializedName("credits") val credits: Int = 0,
    @SerializedName("passedAt") val passedAt: String? = null,
)
