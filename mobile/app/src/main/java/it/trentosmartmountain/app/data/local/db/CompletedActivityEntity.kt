package it.trentosmartmountain.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Attività escursionistica completata, salvata localmente al termine del tracking GPS.
 *
 * Persistita da due sorgenti:
 * 1. [RegistraViewModel.confirmStopTracking] → attività registrate localmente con tracciato GPS
 * 2. [ActivityListViewModel.syncCompletedSessionsToRoom] → sessioni COMPLETED importate dal backend
 *    (senza tracciato locale, [trackLatLng] = "[]", [isSynced] = true)
 *
 * [trackLatLng]: array JSON di triplet [lat, lon, alt]. Serializzato come stringa per Room.
 *   - Attività locali: array popolato con max 200 punti campionati dal tracking GPS
 *   - Attività da backend: "[]" (no tracciato locale disponibile)
 *
 * [isSynced]: true quando i dati sono stati verificati/importati dal backend.
 *
 * NOTA ROOM: i campi nullable (String?, Int?) vengono mappati a colonne NULL in SQLite.
 * I campi con default (isSynced, completedAt) hanno @ColumnInfo(defaultValue) per evitare
 * warning KSP in migrazioni future.
 */
@Entity(tableName = "completed_activities")
data class CompletedActivityEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String?,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "activity_type", defaultValue = "hiking")
    val activityType: String = "hiking",

    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,

    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long,

    @ColumnInfo(name = "moving_seconds")
    val movingSeconds: Long,

    @ColumnInfo(name = "total_seconds")
    val totalSeconds: Long,

    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double,

    @ColumnInfo(name = "elevation_gain_meters")
    val elevationGainMeters: Int,

    @ColumnInfo(name = "current_altitude_meters")
    val currentAltitudeMeters: Int?,

    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: String?,

    @ColumnInfo(name = "track_lat_lng")
    val trackLatLng: String,

    @ColumnInfo(name = "estimated_calories")
    val estimatedCalories: Int?,

    @ColumnInfo(name = "points")
    val points: Int?,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "completed_at", defaultValue = "0")
    val completedAt: Long = 0L,
)
