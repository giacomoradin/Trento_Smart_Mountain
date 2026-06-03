import mongoose from "mongoose";
const { Schema } = mongoose;

// Le storie vivono 24h poi spariscono (come Instagram). Usiamo un TTL index su
// `expiresAt`: MongoDB rimuove automaticamente i documenti scaduti.
export const STORY_TTL_MS = 24 * 3600 * 1000;

// Singolo media della storia. I media sono **Base64 data URI** (niente object
// storage nello stack): immagini compresse e video brevi/bassa risoluzione.
// I cap di dimensione sono imposti in fase di validazione (validationMiddleware)
// e nel service (storyService) per restare sotto il limite body di 5mb.
const storyMediaSchema = new Schema(
  {
    kind: { type: String, enum: ["image", "video"], required: true },
    dataUri: { type: String, required: true }, // "data:image/jpeg;base64,..." | "data:video/mp4;base64,..."
    durationSec: { type: Number, default: null }, // solo per i video
  },
  { _id: false },
);

// Punto della polyline campionata mostrata come "traccia" in overlay sul media.
const overlayPointSchema = new Schema(
  { lat: { type: Number }, lon: { type: Number } },
  { _id: false },
);

// Snapshot dei dati di tracciamento da disegnare in overlay sul media (stile
// Strava: distanza/dislivello/tempo + traccia). Salvato alla creazione così il
// viewer non deve rifare fetch dell'attività/sessione referenziata.
const storyOverlaySchema = new Schema(
  {
    title: { type: String, default: null },
    activityType: { type: String, default: null },
    difficultyLevel: { type: String, default: null },
    distanceMeters: { type: Number, default: null },
    elevationGainM: { type: Number, default: null },
    movingSeconds: { type: Number, default: null },
    routePolyline: { type: [overlayPointSchema], default: undefined },
  },
  { _id: false },
);

const storySchema = new Schema({
  authorId: { type: Schema.Types.ObjectId, ref: "User", required: true, index: true },

  // "planned_session" → condivisione pre-hike di una sessione pianificata con
  //                      link/codice per unirsi.
  // "activity"        → preview post-hike di un'attività completata.
  type: { type: String, enum: ["planned_session", "activity"], required: true },

  // Riferimenti (uno dei due in base al type) per deep-link / join.
  sessionId: { type: Schema.Types.ObjectId, ref: "HikeSession", default: null },
  activityId: { type: Schema.Types.ObjectId, ref: "Activity", default: null },

  // Snapshot del codice invito (solo planned_session) per il bottone "Unisciti".
  inviteCode: { type: String, default: null },

  caption: { type: String, default: null, maxlength: 200 },
  media: { type: [storyMediaSchema], default: [] },
  overlay: { type: storyOverlaySchema, default: null },

  // Chi ha visto la storia (per anello "non vista" + privacy author-side).
  viewers: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
      viewedAt: { type: Date, default: Date.now },
    },
  ],

  createdAt: { type: Date, default: Date.now },
  expiresAt: { type: Date, required: true },
});

// TTL: i documenti vengono rimossi automaticamente quando `expiresAt` è passato.
storySchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });
// Lookup "storie non scadute di un autore, più recenti prima".
storySchema.index({ authorId: 1, createdAt: -1 });

const Story = mongoose.model("Story", storySchema);
export default Story;
