// Attività personale ("libera") registrata da un escursionista.
// Differenze rispetto a HikeSession: owner singolo, no lifecycle (sempre
// completata al momento del create), no inviteCode/participants.
// Le stats sono sempre reali, non c'è una fase GPX pianificata.
import mongoose from "mongoose";
const { Schema } = mongoose;

const activitySchema = new Schema({
  userId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },

  name: { type: String, required: true, maxlength: 120 },
  activityType: {
    type: String,
    enum: ["hiking", "trail", "skitouring", "trekking"],
    default: "hiking",
  },

  // Sessione di gruppo di origine: valorizzato quando questa Activity è la
  // copia PERSONALE dell'uscita di un membro di una HikeSession (ognuno
  // condivide la propria registrazione sul feed, ADR-001). Le copie sono:
  //  - idempotenti per coppia (userId, sourceSessionId) — vedi createActivity;
  //  - escluse dalle statistiche aggregate (la sessione conta già una volta);
  //  - senza accredito crediti (già accreditati da completeSession).
  sourceSessionId: {
    type: Schema.Types.ObjectId,
    ref: "HikeSession",
    default: null,
    index: true,
  },
  difficultyLevel: { type: String, enum: ["T", "E", "EE", "EEA"] },

  // epoch ms — stesso formato del client mobile per coerenza
  startTimeMs: { type: Number, required: true },
  endTimeMs: { type: Number, required: true },
  completedAt: { type: Date, default: Date.now, index: true },

  startPoint: {
    type: { type: String, enum: ["Point"] },
    coordinates: { type: [Number] }, // [lon, lat]
  },
  endPoint: {
    type: { type: String, enum: ["Point"] },
    coordinates: { type: [Number] },
  },

  actualStats: {
    movingSeconds: { type: Number, required: true },
    totalSeconds: { type: Number, required: true },
    distanceMeters: { type: Number, required: true },
    elevationGainM: { type: Number, required: true },
    finalPoints: { type: Number },
    estimatedCalories: { type: Number },
    currentAltitudeM: { type: Number },
  },

  // profilo altimetrico campionato (max 200 punti, metri assoluti)
  elevationProfile: { type: [Number], default: undefined },

  // Traccia GPS campionata (downsampled) del percorso effettivamente registrato.
  // Serve a disegnare la "route signature" Strava-style nella card del feed e
  // nel dettaglio attività, SENZA dover ricaricare l'intera traccia da Room.
  // Salvata solo per attività registrate online; le attività vecchie o create
  // offline non l'hanno → la UI degrada elegantemente (hero = profilo altimetrico).
  // Formato [{lat, lon}] coerente con HikeSession.plannedRoute.polylinePoints.
  routePolyline: {
    type: [
      {
        lat: { type: Number, required: true, min: -90, max: 90 },
        lon: { type: Number, required: true, min: -180, max: 180 },
      },
    ],
    default: undefined,
  },

  // ── Social (Sprint 2 — schermata Social) ──────────────────────────────────
  // L'attività è privata di default (visibile solo al proprietario nell'app).
  // Diventa "pubblica sul feed" quando il proprietario preme + Condividi: viene
  // settato `sharedAt = now` e l'attività appare nel feed dei follower.
  // Pattern: NO entity Post separata — riutilizziamo l'attività stessa come
  // "post" + caption opzionale. Vedi docs/sprint2_social.md §2.
  sharedAt: { type: Date, default: null, index: true },
  caption: { type: String, default: null, maxlength: 200 },

  // Likes come sub-document per evitare $lookup nelle card del feed: ogni feed
  // item può così calcolare `likedByMe` con $in/array check sul documento già
  // proiettato. Trade-off: array unbounded → in pratica tetto naturale ~10k
  // per attività universitarie (no scaling Twitter). Per scalare oltre,
  // estrarre in collection separata `Like` con indice {targetId, userId}.
  likes: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
      createdAt: { type: Date, default: Date.now },
    },
  ],

  // Denormalizzato per evitare aggregation nel feed: ogni POST/DELETE commento
  // fa $inc. Verità è la count effettiva su Comment collection — eventuale
  // drift sanabile con job nightly. Per ora: max 50/giorno per anti-spam.
  commentsCount: { type: Number, default: 0, min: 0 },
});

activitySchema.index({ userId: 1, completedAt: -1 });
activitySchema.index({ startPoint: "2dsphere" }, { sparse: true });
// Indice composto per il feed query "tutte le activity dei seguiti, ordinate
// per data di condivisione discendente". Usato in socialService.getFeed.
activitySchema.index({ userId: 1, sharedAt: -1 }, { sparse: true });

const Activity = mongoose.model("Activity", activitySchema);
export default Activity;
