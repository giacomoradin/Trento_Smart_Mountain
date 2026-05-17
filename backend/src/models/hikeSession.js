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
      // GeoJSON Point — popolato solo se il GPX viene importato.
      // NESSUN default: i documenti senza GPX non entrano nell'indice 2dsphere
      // (grazie a sparse:true sull'indice, vedi sotto) ed evitano il rumore a Null Island.
      type: { type: String, enum: ["Point"] },
      coordinates: { type: [Number] }, // [lng, lat]
    },
    endPoint: {
      type: { type: String, enum: ["Point"] },
      coordinates: { type: [Number] },
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

// Indice geospaziale per query di prossimità.
// sparse:true → i documenti senza startPoint (es. sessioni senza GPX) non vengono
// indicizzati, evitando coordinate [0,0] a Null Island e errori 16755 di MongoDB.
hikSessionSchema.index({ "routeDetails.startPoint": "2dsphere" }, { sparse: true });

const HikeSession = mongoose.model("HikeSession", hikSessionSchema);
export default HikeSession;
