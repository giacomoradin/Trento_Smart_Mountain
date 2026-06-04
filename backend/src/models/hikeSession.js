import mongoose from "mongoose";
const { Schema } = mongoose;

// Setter che accetta sia "YYYY-MM-DD" (formato legacy esposto dal mobile)
// sia Date/ISO 8601. Output JSON sempre come "YYYY-MM-DD" via toJSON.transform
// per evitare breaking change al client.
//
// Background: prima dell'audit 2026-05, meetingDate era String. Questo
// impediva $sort cronologico efficiente lato DB (sort lessicografico
// funzionava per "YYYY-MM-DD" ma non per altri formati eventualmente salvati).
// Vedi anche scripts/migrate-meeting-date.js per il backfill delle stringhe
// esistenti.
function parseMeetingDate(value) {
  if (value === null || value === undefined || value === "") return value;
  if (value instanceof Date) return value;
  if (typeof value === "string") {
    const trimmed = value.trim();
    // "YYYY-MM-DD" → mezzanotte UTC (evita drift timezone)
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(trimmed);
    if (match) {
      return new Date(Date.UTC(+match[1], +match[2] - 1, +match[3]));
    }
    const d = new Date(trimmed);
    if (!Number.isNaN(d.getTime())) return d;
  }
  return value; // lascia che la validation Mongoose segnali l'errore
}

function formatMeetingDateForJson(value) {
  if (!value) return value;
  if (value instanceof Date) {
    return value.toISOString().slice(0, 10);
  }
  return value;
}
// ── Sub-schema: singolo item della checklist ──────────────────────────────────
const checklistItemSchema = new Schema({
  // Nome leggibile dell'item — es. "Scarponi con suola rigida"
  nome: {
    type: String,
    required: true,
    trim: true,
  },
 
  // Motivazione contestuale — es. "Quota massima > 2500 m con possibile ghiaccio"
  // Rende la checklist educativa: l'utente capisce *perché* portare quell'item.
  motivo: {
    type: String,
    default: '',
    trim: true,
  },
}, { _id: false });
 
// ── Sub-schema: categoria (raggruppa item per tipo + livello) ─────────────────
const checklistCategoriaSchema = new Schema({
  // Tipo di categoria — es. "Abbigliamento", "Alimentazione", "Attrezzatura", "Sicurezza"
  nome: {
    type: String,
    required: true,
    trim: true,
  },
 
  // Livello di priorità dell'intera categoria:
  //   base       → indispensabile, senza questo non si parte
  //   consigliato → fortemente raccomandato in base al contesto
  //   opzionale  → utile ma non critico
  livello: {
    type: String,
    enum: ['base', 'consigliato', 'opzionale'],
    required: true,
  },
 
  items: {
    type: [checklistItemSchema],
    default: [],
  },
}, { _id: false });
 
// ── Sub-schema: snapshot meteo usato al momento della generazione ─────────────
// Serve per debug e per mostrare all'utente su quali previsioni si basava la checklist.
const checklistWeatherSnapshotSchema = new Schema({
  // externalId della town di riferimento (dal modello Location)
  locationId:  { type: String, default: null },
  locationName: { type: String, default: null },
 
  // Quando è stato fatto il fetch del forecast
  forecastFetchedAt: { type: Date, default: null },
 
  // Range di validità del forecast usato
  forecastValidFrom: { type: Date, default: null },
  forecastValidTo:   { type: Date, default: null },
 
  // Metriche aggregate estratte dagli slot coperti dalla durata dell'escursione.
  // Sono i valori su cui il checklistService ha preso decisioni.
  temperaturaMinPrevista: { type: Number, default: null }, // °C
  temperaturaMedPrevista: { type: Number, default: null }, // °C
  pioggiaProbMax:         { type: Number, default: null }, // % (max tra gli slot)
  neveFrescaPrevista:     { type: Number, default: null }, // cm
  ventoMaxPrevisto:       { type: Number, default: null }, // km/h
  quotaZeroTermico:       { type: Number, default: null }, // m slm (min tra gli slot)
}, { _id: false });
 
// ── Sub-schema: checklist completa ────────────────────────────────────────────
const checklistSchema = new Schema({
  // Timestamp di creazione (prima generate chiamata)
  generatedAt: { type: Date, default: null },
 
  // Timestamp dell'ultimo aggiornamento (chiamata update)
  updatedAt: { type: Date, default: null },
 
  // true quando è scattato il freeze (mezzanotte del giorno prima della sessione).
  // Dopo il freeze il backend rifiuta qualsiasi update.
  isFrozen: { type: Boolean, default: false },
 
  // Timestamp in cui è scattato il freeze (per trasparenza verso il client)
  frozenAt: { type: Date, default: null },
 
  // Snapshot del meteo usato per la generazione/aggiornamento
  meteoSnapshot: {
    type: checklistWeatherSnapshotSchema,
    default: null,
  },
 
  // Lista ordinata di categorie con i rispettivi item
  // Ordine convenzionale: base → consigliato → opzionale
  categorie: {
    type: [checklistCategoriaSchema],
    default: [],
  },
 
  // Idratazione consigliata in litri (calcolata da durata + difficoltà + meteo)
  acquaLitri: { type: Number, default: null },
 
  // Cibo calcolato in kcal stimate necessarie (calcolate da durata + dislivello)
  calorieFabbisogno: { type: Number, default: null },
}, { _id: false });
 
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
   checklist: {
      type: checklistSchema,
      default: null,
    },
  // Metadati sessione
  meetingDate: { type: Date, set: parseMeetingDate },
  meetingTime: { type: String },
  meetingLocation: { type: String },
  maxParticipants: { type: Number },
  minExperienceLevel: { type: String, enum: ["T", "E", "EE", "EEA"] },

  // Codice del sentiero SAT selezionato dal DB (modalità "Scegli percorso sulla mappa").
  // Null in modalità GPX. Serve alla checklist dinamica (US-7) per risalire al Sentiero
  // (Sentiero.findOne({ codice })) e generare l'equipaggiamento in base a difficoltà/quota/meteo.
  sentieroCode: { type: String, default: null },

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

  // Polyline del percorso pianificato (necessaria per controllo distanza lato client)
  plannedRoute: {
    source: { type: String, enum: ["GPX", "SAT"] },
    polylinePoints: {
      type: [
        {
          lat: { type: Number, required: true, min: -90, max: 90 },
          lon: { type: Number, required: true, min: -180, max: 180 },
        },
      ],
      default: undefined,
    },
    pointsCountOriginal: { type: Number },
    pointsCountStored: { type: Number },
    bbox: {
      minLat: { type: Number },
      minLon: { type: Number },
      maxLat: { type: Number },
      maxLon: { type: Number },
    },
    updatedAt: { type: Date },
  },

  // Live tracking (last known position per utente)
  liveLocations: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
      lat: { type: Number, required: true, min: -90, max: 90 },
      lon: { type: Number, required: true, min: -180, max: 180 },
      accuracyM: { type: Number, min: 0, max: 1000 },
      altitudeM: { type: Number, min: -500, max: 9000 },
      trackingStatus: {
        type: String,
        enum: ["MOVING", "PAUSED"],
        default: "MOVING",
      },
      updatedAt: { type: Date, default: Date.now },
    },
  ],

  // Stato live tracking per utente (ACTIVE/SUSPENDED)
  liveTracking: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
      status: { type: String, enum: ["ACTIVE", "SUSPENDED"], required: true },
      reason: {
        type: String,
        enum: ["TOO_FAR_FROM_ROUTE", "MANUAL", "OTHER"],
      },
      updatedAt: { type: Date, default: Date.now },
    },
  ],

  // Statistiche effettive registrate dal client al termine del tracking GPS.
  // Popolate da PATCH /api/v1/sessions/:id/complete quando la sessione diventa COMPLETED.
  // Se presenti, sostituiscono completamente le stime CAI nella UI.
  actualStats: {
    movingSeconds: { type: Number }, // tempo cronometrato senza pause
    totalSeconds: { type: Number }, // tempo totale (incluso pause)
    distanceMeters: { type: Number }, // distanza percorsa effettiva
    elevationGainM: { type: Number }, // dislivello positivo cumulato reale
    finalPoints: { type: Number }, // punteggio post-completamento (μ pesato)
    estimatedCalories: { type: Number }, // kcal stimate
    currentAltitudeM: { type: Number }, // ultima altitudine registrata
  },

  // Codice invito alfanumerico univoco (generato automaticamente)
  inviteCode: {
    type: String,
    required: true,
    unique: true,
    uppercase: true,
  },

  // Lista partecipanti (richieste + accettati)
  participants: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true }, // riferimento al modello User
      role: { type: String, enum: ["hiker", "groupLeader"], default: "hiker" }, // ruolo del partecipante
      // Stato di approvazione: chi entra con codice invito parte "pending" e
      // diventa "accepted" quando capogruppo o un partecipante già accettato lo
      // approva. Default "accepted" per retrocompatibilità coi documenti pre-esistenti.
      status: { type: String, enum: ["pending", "accepted"], default: "accepted" },
      // Chi ha approvato la richiesta (capogruppo o partecipante accettato).
      // Null per il creator (auto-accettato).
      approvedBy: { type: Schema.Types.ObjectId, ref: "User", default: null },
      joinedAt: { type: Date, default: Date.now }, // timestamp di quando ha inviato la richiesta
    },
  ],

  // Utenti rimossi DEFINITIVAMENTE dal capogruppo: non possono più ri-unirsi a
  // QUESTA sessione (ban locale alla sessione).
  removedUserIds: [{ type: Schema.Types.ObjectId, ref: "User" }],

  // Utenti che hanno "eliminato" questa sessione COMPLETED dalla propria lista
  // "Le mie attività" sul mobile. La sessione resta nel DB (appartiene anche
  // agli altri partecipanti) ma viene esclusa da getSessionsByUser per questi
  // utenti — così non riappare dopo logout/login con DB locale ripulito.
  hiddenForUsers: [{ type: Schema.Types.ObjectId, ref: "User" }],

  // Stato della sessione
  status: {
    type: String,
    enum: ["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"],
    default: "PLANNED",
  },

  // ── Modello "Ibrido" di completamento ──────────────────────────────────────
  // Ogni membro termina il PROPRIO tracking individualmente (registrato qui).
  // La sessione passa a COMPLETED quando TUTTI i membri accettati hanno finito,
  // oppure quando il capogruppo forza la chiusura (forceCompleteSession).
  // Per le sessioni in solitaria (solo il creator accettato) basta che il creator
  // finisca → la sessione si chiude pulita senza restare bloccata.
  finishedParticipants: [{ type: Schema.Types.ObjectId, ref: "User" }],

  // Failover leadership
  // Leader EFFETTIVO corrente: di norma è il creator, ma può passare a un
  // partecipante se il creator si disconnette (elezione). Quando il creator
  // rientra, la leadership gli viene restituita. Default = creator (impostato
  // alla creazione).
  currentLeaderId: { type: Schema.Types.ObjectId, ref: "User", default: null },
  statoFailover: { type: Boolean, default: false }, //  true se il groupLeader è inattivo e la leadership è passata a un altro partecipante
  lastHeartbeat: { type: Date, default: Date.now }, // timestamp dell'ultimo segnale di vita ricevuto dal leader EFFETTIVO corrente

  startTime: { type: Date },
  endTime: { type: Date },
  // Tracking per-utente per evitare doppi accrediti: ogni partecipante che chiama
  // /complete viene aggiunto qui. Atomic check via $ne nel findOneAndUpdate evita
  // race condition (doppio tap → singolo accredito).
  creditsAwardedTo: [{ type: Schema.Types.ObjectId, ref: "User" }],
  // Manteniamo creditsAwardedAt per documentare il primo completion; non più usato
  // come idempotency key (sostituito da creditsAwardedTo).
  creditsAwardedAt: { type: Date },

  // ── Social (Sprint 2 — schermata Social) ──────────────────────────────────
  // La sessione è privata di default sul feed. Diventa visibile ai follower
  // del creator quando viene settato `sharedAt = now` via POST /sessions/:id/share.
  // Pattern identico ad Activity (vedi models/activity.js + docs/sprint2_social.md §2).
  // Authorization: solo `creatorId` può condividere (i partecipanti non-creator
  // hanno una propria Activity post-complete se vogliono condividere).
  sharedAt: { type: Date, default: null, index: true },
  caption: { type: String, default: null, maxlength: 200 },
  likes: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
      createdAt: { type: Date, default: Date.now },
    },
  ],
  commentsCount: { type: Number, default: 0, min: 0 },

  createdAt: { type: Date, default: Date.now },
});

// Indice geospaziale per query di prossimità
hikSessionSchema.index({ "routeDetails.startPoint": "2dsphere" }); // Permette di cercare sessioni vicine a una posizione geografica

// Indice composto per la query frequente "sessioni dell'utente ordinate per
// data crescente" (vedi getSessionsByUser).
hikSessionSchema.index({ status: 1, meetingDate: 1 });

// Indice per la query feed: "sessioni dei creator/partecipanti che ho seguito,
// ordinate per data di condivisione discendente". Sparse perché la maggior
// parte dei documenti ha sharedAt=null (privati).
hikSessionSchema.index({ creatorId: 1, sharedAt: -1 }, { sparse: true });
hikSessionSchema.index(
  { "participants.userId": 1, sharedAt: -1 },
  { sparse: true },
);

// Indici per lookup live tracking per utente.
hikSessionSchema.index({ "liveLocations.userId": 1 });
hikSessionSchema.index({ "liveTracking.userId": 1 });

// Trasforma `meetingDate` (Date) in "YYYY-MM-DD" nei JSON di risposta API.
// Mantiene la backward compatibility col mobile che si aspetta una stringa.
// Applicato a entrambi toJSON e toObject perché Mongoose chiama il primo per
// `res.json(doc)` e il secondo per `doc.toObject()` / `.lean()` (NB: .lean()
// NON applica i transform — vedi serviceLayerNote nei service per i casi
// dove serve conversione manuale).
const meetingDateTransform = function (doc, ret) {
  if (ret.meetingDate instanceof Date) {
    ret.meetingDate = formatMeetingDateForJson(ret.meetingDate);
  }
  return ret;
};
hikSessionSchema.set("toJSON", { transform: meetingDateTransform });
hikSessionSchema.set("toObject", { transform: meetingDateTransform });

const HikeSession = mongoose.model("HikeSession", hikSessionSchema);
export default HikeSession;
