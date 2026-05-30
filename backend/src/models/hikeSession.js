import mongoose from "mongoose";
const { Schema } = mongoose;

// Punto della polyline pianificata. _id disabilitato: sono migliaia di punti
// e non serve un identificatore per ciascuno (riduce dimensione documento).
const plannedRoutePointSchema = new Schema(
  {
    lat: { type: Number, required: true },
    lon: { type: Number, required: true },
  },
  { _id: false },
);

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

  // Codice del sentiero SAT selezionato dal DB (modalità "Scegli percorso sulla mappa").
  // Null in modalità GPX. Serve alla checklist dinamica (US-7) per risalire al Sentiero
  // (Sentiero.findOne({ codice })) e generare l'equipaggiamento in base a difficoltà/quota/meteo.
  sentieroCode: { type: String, default: null },

  // Tracciato pianificato (origine + polyline) usato per il controllo distanza dal
  // percorso durante il tracking. source = "GPX" (da file importato) | "SAT" (da DB sentieri).
  // polylinePoints in formato GeoJSON-like { lat, lon } campionato (downsampling lato client).
  plannedRoute: {
    source: { type: String, enum: ["GPX", "SAT"] },
    polylinePoints: { type: [plannedRoutePointSchema], default: undefined },
    bbox: { type: [Number], default: undefined }, // [minLon, minLat, maxLon, maxLat]
  },

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

// Indice geospaziale per query di prossimità.
// sparse:true → i documenti senza startPoint (es. sessioni senza GPX) non vengono
// indicizzati, evitando coordinate [0,0] a Null Island e errori 16755 di MongoDB.
hikSessionSchema.index({ "routeDetails.startPoint": "2dsphere" }, { sparse: true });

const HikeSession = mongoose.model("HikeSession", hikSessionSchema);
export default HikeSession;
