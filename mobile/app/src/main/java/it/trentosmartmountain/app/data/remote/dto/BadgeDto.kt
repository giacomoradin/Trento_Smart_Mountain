package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Risposta di GET /users/me/badges — elenco completo del catalogo con flag
 * `earned`. I non-earned vengono comunque mostrati (greyed out) per dare
 * all'utente visibilità di cosa può sbloccare.
 */
data class BadgeItem(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("emoji") val emoji: String,
    @SerializedName("tier") val tier: String, // bronze/silver/gold/platinum
    @SerializedName("earned") val earned: Boolean,
    @SerializedName("earnedAt") val earnedAt: String?,
    @SerializedName("contextValue") val contextValue: Int?,
)

/** Certificato: una category quiz interamente superata. */
data class CertificateItem(
    @SerializedName("categorySlug") val categorySlug: String,
    @SerializedName("categoryName") val categoryName: String,
    @SerializedName("issuedAt") val issuedAt: String,
    @SerializedName("totalQuizzes") val totalQuizzes: Int,
)
