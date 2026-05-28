package it.trentosmartmountain.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Write-Ahead Log dei punti GPS durante un tracking attivo.
 *
 * Motivazione (audit 2026-05): prima di questa entity, i punti GPS vivevano
 * SOLO in memoria nel [it.trentosmartmountain.app.data.location.HikeTrackingEngine].
 * Un crash, un OOM kill o un riavvio forzato del device durante un'escursione
 * comportava la perdita TOTALE del tracciato, anche dopo ore di registrazione.
 *
 * Questa tabella riceve un INSERT per ogni snapshot GPS ricevuto, con un
 * `track_id` che identifica la sessione di tracking. Al termine del tracking
 * ([it.trentosmartmountain.app.repository.TrackingPersistenceRepository.finalize])
 * i punti vengono letti, copiati in [CompletedActivityEntity], e la WAL viene
 * cancellata per quel `track_id`.
 *
 * In caso di crash: alla riapertura dell'app, se esiste una row con
 * `track_id` salvato in SharedPrefs come "in corso", la VM offrirà recovery
 * (TODO Sprint 3 — per ora i dati sono safe ma il recovery manuale non c'è).
 */
@Entity(
  tableName = "tracking_wal",
  indices = [Index(value = ["track_id"]) ],
)
data class TrackingWalEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,

  /** UUID del tracking — chiave di raggruppamento per `getAllForTrack`. */
  @ColumnInfo(name = "track_id")
  val trackId: String,

  @ColumnInfo(name = "lat")
  val latitude: Double,

  @ColumnInfo(name = "lon")
  val longitude: Double,

  /** Altitudine in metri, nullable (non tutti i GPS la riportano). */
  @ColumnInfo(name = "alt")
  val altitudeMeters: Double?,

  /** Timestamp del fix GPS in epoch ms (locale). */
  @ColumnInfo(name = "ts")
  val timestampMs: Long,
)
