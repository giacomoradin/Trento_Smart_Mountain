import mongoose from "mongoose";
const { Schema } = mongoose;

const hikSessionSchema = new Schema({
  // Chi ha creato la sessione diventa automaticamente groupLeader
  creatorId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
  },

  // Parametri del percorso
  routeDetails: {
    name: { type: String, required: true },
    startPoint: {
      type: { type: String, enum: ["Point"], default: "Point" },
      coordinates: { type: [Number], default: [0, 0] }, // [lng, lat]
    },
    endPoint: {
      type: { type: String, enum: ["Point"], default: "Point" },
      coordinates: { type: [Number], default: [0, 0] },
    },
    difficultyLevel: {
      type: String,
      enum: ["T", "E", "EE", "EEA"],
      default: "E",
    },
    elevationGain: { type: Number },
  },

  // Metadati sessione
  meetingDate: { type: String },
  meetingTime: { type: String },
  meetingLocation: { type: String },
  maxParticipants: { type: Number },
  minExperienceLevel: { type: String, enum: ["T", "E", "EE", "EEA"] },

  // Dati tracciato GPX (opzionale, da import mobile)
  gpxFileName: { type: String },
  gpxStats: {
    distanceKm: { type: Number },
    elevationGainM: { type: Number },
    trackPoints: { type: Number },
    // Profilo altimetrico campionato (max 50 punti) per il rendering del chart
    // nella SessionDetailScreen. Calcolato dal parser GPX mobile con smoothing.
    elevationProfile: { type: [Number], default: undefined },
    // Stima punti calcolata col modello CAI in fase di pianificazione (μ = 1.0).
    // Verrà sostituita al COMPLETED con il punteggio finale che pesa l'efficienza reale.
    estimatedPoints: { type: Number },
    // Durata effettiva estratta dai tag <time> dei trkpt del GPX (se presenti).
    // Differenza fra primo e ultimo timestamp, in secondi. Usata come fallback al
    // posto della formula CAI per la durata di sessioni non ancora completate.
    gpxDurationSec: { type: Number },
  },

  // Statistiche effettive registrate dal client al termine del tracking GPS.
  // Popolate da PATCH /api/v1/sessions/:id/complete quando la sessione diventa COMPLETED.
  // Se presenti, sostituiscono completamente le stime CAI nella UI.
  actualStats: {
    movingSeconds: { type: Number },       // tempo cronometrato senza pause
    totalSeconds: { type: Number },        // tempo totale (incluso pause)
    distanceMeters: { type: Number },      // distanza percorsa effettiva
    elevationGainM: { type: Number },      // dislivello positivo cumulato reale
    finalPoints: { type: Number },         // punteggio post-completamento (μ pesato)
    estimatedCalories: { type: Number },   // kcal stimate
    currentAltitudeM: { type: Number },    // ultima altitudine registrata
  },

  // Codice invito alfanumerico univoco (generato automaticamente)
  inviteCode: {
    type: String,
    required: true,
    unique: true,
    uppercase: true,
  },

  // Lista partecipanti che hanno accettato l'invito
  participants: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true }, // riferimento al modello User
      role: { type: String, enum: ["hiker", "groupLeader"], default: "hiker" }, // ruolo del partecipante
      joinedAt: { type: Date, default: Date.now }, // timestamp di quando ha accettato l'invito
    },
  ],

  // Stato della sessione
  status: {
    type: String,
    enum: ["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"],
    default: "PLANNED",
  },

  // Failover leadership
  statoFailover: { type: Boolean, default: false }, //  true se il groupLeader è inattivo e la leadership è passata a un altro partecipante
  lastHeartbeat: { type: Date, default: Date.now }, // timestamp dell'ultimo segnale di vita ricevuto dal groupLeader

  startTime: { type: Date }, // timestamp di inizio sessione (popolato quando lo status diventa ACTIVE)
  endTime: { type: Date }, // timestamp di fine sessione (popolato quando lo status diventa COMPLETED)
  createdAt: { type: Date, default: Date.now },
});

// Indice geospaziale per query di prossimità
hikSessionSchema.index({ "routeDetails.startPoint": "2dsphere" }); // Permette di cercare sessioni vicine a una posizione geografica

const HikeSession = mongoose.model("HikeSession", hikSessionSchema);
export default HikeSession;
