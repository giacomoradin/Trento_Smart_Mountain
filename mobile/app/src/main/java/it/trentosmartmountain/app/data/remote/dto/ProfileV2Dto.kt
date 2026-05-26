package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO del profilo v2 (dati personali, esperienza, preferenze).
 *
 * Tutti i campi sono nullable: il server accetta update parziali e l'utente
 * può saltare l'onboarding lasciandoli a null. La gestione UI usa `?:` /
 * `isNullOrBlank()` per fornire placeholder ragionevoli.
 *
 * NOTA: la scheda medica (gruppo sanguigno, allergie, patologie, contatto
 * emergenza) NON è qui — categoria particolare GDPR, iterazione dedicata.
 */

data class PersonalInfo(
    @SerializedName("sex") val sex: String? = null, // "M" | "F" | "X" | "N"
    @SerializedName("birthDate") val birthDate: String? = null, // ISO 8601
    @SerializedName("heightCm") val heightCm: Int? = null,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

data class Experience(
    @SerializedName("caiLevel") val caiLevel: String? = null, // T/E/EE/EEA
    @SerializedName("baselineFitness") val baselineFitness: String? = null, // sedentary/active/sport/athlete
    @SerializedName("weeklyTrainingFreq") val weeklyTrainingFreq: String? = null, // "0-1"/"2-3"/"4+"
)

data class NotificationPreferences(
    @SerializedName("pushEnabled") val pushEnabled: Boolean? = null,
    @SerializedName("emailDigest") val emailDigest: Boolean? = null,
    @SerializedName("fcmToken") val fcmToken: String? = null,
)

data class PrivacyPreferences(
    @SerializedName("profileVisibility") val profileVisibility: String? = null, // public/friends/private
)

data class Preferences(
    @SerializedName("units") val units: String? = null, // "metric"/"imperial"
    @SerializedName("language") val language: String? = null, // "it"/"en"/...
    @SerializedName("notifications") val notifications: NotificationPreferences? = null,
    @SerializedName("privacy") val privacy: PrivacyPreferences? = null,
)

// ── Wrapper response usati dalle PATCH (il server ritorna { personalInfo: {...} }) ──

data class PersonalInfoResponse(@SerializedName("personalInfo") val personalInfo: PersonalInfo?)
data class ExperienceResponse(@SerializedName("experience") val experience: Experience?)
data class PreferencesResponse(@SerializedName("preferences") val preferences: Preferences?)
data class ProfileCompleteResponse(@SerializedName("profileCompletedAt") val profileCompletedAt: String?)
