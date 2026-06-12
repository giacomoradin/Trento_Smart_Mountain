import Refuge from "../models/refuge.js";
import EdgeNode from "../models/edgeNode.js";
import RefugeSensorReading from "../models/refugeSensorReading.js";
import RefugePassage from "../models/refugePassage.js";

/**
 * Servizio Dashboard IoT del rifugio.
 *
 * ⚠️ Sprint mockup: NON esiste ancora l'ingest MQTT reale. I dati vengono
 * generati (seed) la prima volta che un rifugio apre la dashboard, così la UI
 * ha sempre contenuto realistico. Lo schema DB è però quello definitivo: quando
 * arriverà l'ingest reale basterà fare upsert sugli stessi modelli e rimuovere
 * il seed.
 */

const MOCK_NODES = [
  { code: "EDGE-NODE-01", name: "Passo Principe", signalPct: 92, online: true },
  { code: "EDGE-NODE-02", name: "Sentiero 546", signalPct: 71, online: true },
  { code: "EDGE-NODE-03", name: "Cresta Catinaccio", signalPct: 0, online: false },
  { code: "EDGE-NODE-04", name: "Rifugio", signalPct: 100, online: true },
];

const MOCK_PASSAGES = [
  { displayName: "Marco Bianchi", h: 14, m: 22 },
  { displayName: "Anna Conti", h: 13, m: 55 },
  { displayName: "Luca Ferrari", h: 12, m: 41 },
  { displayName: "Giulia Rossi", h: 11, m: 18 },
  { displayName: "Paolo Greco", h: 10, m: 47 },
  { displayName: "Sara Moretti", h: 9, m: 33 },
];

function todayAt(h, m) {
  const d = new Date();
  d.setHours(h, m, 0, 0);
  return d;
}

function startOfToday() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}

/**
 * Seed idempotente:
 *  - se non c'è alcuna lettura sensori → crea snapshot + edge nodes;
 *  - se non ci sono passaggi OGGI → li (ri)crea per la giornata corrente,
 *    così la sezione "Passaggi oggi" è sempre popolata anche nei giorni
 *    successivi al primo seed.
 */
async function ensureSeed(refugeId) {
  const hasReading = await RefugeSensorReading.exists({ refugeId });
  if (!hasReading) {
    const now = new Date();
    await RefugeSensorReading.create({
      refugeId,
      temperatureC: -2.4,
      temperatureTrend: -1.2,
      humidityPct: 78,
      humidityTrend: 4,
      windKmh: 32,
      windDir: "NE",
      windGustKmh: 51,
      pressureHpa: 742,
      pressureTrend: -3,
      capturedAt: now,
    });
    const twoHoursAgo = new Date(now.getTime() - 2 * 3600 * 1000);
    await Promise.all(
      MOCK_NODES.map((n) =>
        EdgeNode.updateOne(
          { refugeId, code: n.code },
          {
            $set: {
              refugeId,
              name: n.name,
              signalPct: n.signalPct,
              online: n.online,
              lastSeenAt: n.online ? now : twoHoursAgo,
            },
          },
          { upsert: true },
        ),
      ),
    );
  }

  const hasPassageToday = await RefugePassage.exists({
    refugeId,
    passedAt: { $gte: startOfToday() },
  });
  if (!hasPassageToday) {
    await RefugePassage.insertMany(
      MOCK_PASSAGES.map((p) => ({
        refugeId,
        displayName: p.displayName,
        via: "mesh",
        credits: 25,
        passedAt: todayAt(p.h, p.m),
      })),
    );
  }
}

/**
 * Dashboard aggregata per il rifugio loggato: sensori (ultima lettura),
 * edge nodes (con conteggio online), passaggi di oggi (con totale crediti).
 */
export async function getRefugeDashboard(refugeId) {
  await ensureSeed(refugeId);

  const [refuge, reading, nodes, passages] = await Promise.all([
    Refuge.findById(refugeId)
      .select("rifugioName quota caiCode posti email isVerified avatarUrl")
      .lean(),
    RefugeSensorReading.findOne({ refugeId }).sort({ capturedAt: -1 }).lean(),
    EdgeNode.find({ refugeId }).sort({ code: 1 }).lean(),
    RefugePassage.find({ refugeId, passedAt: { $gte: startOfToday() } })
      .sort({ passedAt: -1 })
      .lean(),
  ]);

  const onlineCount = nodes.filter((n) => n.online).length;
  const totalCreditsToday = passages.reduce((s, p) => s + (p.credits || 0), 0);

  return {
    refuge: {
      name: refuge?.rifugioName || "Rifugio",
      altitudeM: refuge?.quota ?? null,
      caiCode: refuge?.caiCode ?? null,
      posti: refuge?.posti ?? null,
      email: refuge?.email ?? null,
      verified: refuge?.isVerified ?? false,
      avatarUrl: refuge?.avatarUrl ?? null,
    },
    live: true,
    sensors: reading
      ? {
        temperature: { value: reading.temperatureC, trend: reading.temperatureTrend },
        humidity: { value: reading.humidityPct, trend: reading.humidityTrend },
        wind: { value: reading.windKmh, dir: reading.windDir, gust: reading.windGustKmh },
        pressure: { value: reading.pressureHpa, trend: reading.pressureTrend },
        capturedAt: reading.capturedAt,
      }
      : null,
    edgeNodes: nodes.map((n) => ({
      code: n.code,
      name: n.name,
      signalPct: n.signalPct,
      online: n.online,
      lastSeenAt: n.lastSeenAt,
    })),
    edgeNodesOnline: onlineCount,
    edgeNodesTotal: nodes.length,
    passages: {
      totalCreditsToday,
      items: passages.map((p) => ({
        displayName: p.displayName,
        via: p.via,
        credits: p.credits,
        passedAt: p.passedAt,
      })),
    },
  };
}

/**
 * Aggiorna il profilo del rifugio loggato (per ora: foto della struttura).
 * Usa il modello discriminator `Refuge` (MAI `User.findByIdAndUpdate` per i
 * campi del sotto-schema: lo strict mode li scarterebbe in silenzio — vedi
 * bug S2-C1). `avatarUrl: ""` resetta la foto.
 */
export async function updateRefugeProfile(refugeId, { avatarUrl } = {}) {
  const update = {};
  if (avatarUrl !== undefined) update.avatarUrl = avatarUrl === "" ? null : avatarUrl;

  const refuge = await Refuge.findByIdAndUpdate(
    refugeId,
    { $set: update },
    { new: true, runValidators: true },
  )
    .select("rifugioName avatarUrl")
    .lean();
  if (!refuge) throw new Error("REFUGE_NOT_FOUND");
  return { name: refuge.rifugioName, avatarUrl: refuge.avatarUrl ?? null };
}
