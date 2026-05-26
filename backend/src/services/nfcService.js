import NfcTotem from "../models/nfcTotem.js";
import NfcScan from "../models/nfcScan.js";
import Hiker from "../models/hiker.js";
import { addCredits } from "./creditService.js";
import { evaluateAllBadges } from "./badgeService.js";

function haversineMeters(lon1, lat1, lon2, lat2) {
  const R = 6371000;
  const toRad = (d) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export async function listTotems({ lon, lat, maxDistance } = {}) {
  const filter = { active: true };
  if (lon != null && lat != null) {
    filter.location = {
      $near: {
        $geometry: { type: "Point", coordinates: [lon, lat] },
        ...(maxDistance ? { $maxDistance: maxDistance } : {}),
      },
    };
  }
  return NfcTotem.find(filter)
    .select("tagId name location kind creditsReward altitude radius description")
    .lean();
}

/** Bucket giornaliero "YYYY-MM-DD" in UTC. Usato come chiave dell'unique
 *  partial index su NfcScan per la rate-limit atomic 1 scan/totem/user/day. */
function getScanDayUTC(date = new Date()) {
  return date.toISOString().slice(0, 10);
}

export async function scanTotem(userId, { tagId, gpsLon, gpsLat }) {
  const totem = await NfcTotem.findOne({ tagId, active: true }).lean();
  if (!totem) throw new Error("TOTEM_NOT_FOUND");

  const [tLon, tLat] = totem.location.coordinates;
  const distance = Math.round(haversineMeters(gpsLon, gpsLat, tLon, tLat));

  if (distance > totem.radius) {
    await NfcScan.create({
      userId,
      totemId: totem._id,
      tagId,
      gpsLocation: { type: "Point", coordinates: [gpsLon, gpsLat] },
      distanceFromTotem: distance,
      creditsAwarded: 0,
      rejectionReason: "OUT_OF_RANGE",
    });
    return { ok: false, reason: "OUT_OF_RANGE", distance, totem };
  }

  // ── Atomic claim del reward giornaliero (fix audit 2026-05) ──────────────
  // PRIMA: findOne + create non era atomico → doppio tap rapido poteva creare
  //   due NfcScan con crediti, accreditando doppio bonus.
  // ORA: insert con scanDay + unique partial index (creditsAwarded > 0).
  //   Solo la prima scan riesce; le concorrenti falliscono con E11000 → catch.
  const scanDay = getScanDayUTC();
  const creditsAwarded = totem.creditsReward;
  try {
    await NfcScan.create({
      userId,
      totemId: totem._id,
      tagId,
      scanDay,
      gpsLocation: { type: "Point", coordinates: [gpsLon, gpsLat] },
      distanceFromTotem: distance,
      creditsAwarded,
    });
  } catch (err) {
    if (err.code === 11000) {
      // Già scansionato oggi (anche da una request concorrente). Registriamo
      // comunque l'attempt per analytics ma senza crediti.
      await NfcScan.create({
        userId,
        totemId: totem._id,
        tagId,
        gpsLocation: { type: "Point", coordinates: [gpsLon, gpsLat] },
        distanceFromTotem: distance,
        creditsAwarded: 0,
        rejectionReason: "RATE_LIMIT",
      });
      return { ok: true, alreadyScannedToday: true, creditsAwarded: 0, distance, totem };
    }
    throw err;
  }

  await addCredits({ userId, amount: creditsAwarded, source: "nfc", refId: totem._id, refKind: "NfcTotem" });
  // nfcStats è sotto-documento del discriminator Hiker → usa Hiker (vedi nota in creditService).
  await Hiker.findByIdAndUpdate(userId, {
    $inc: { "nfcStats.scansCount": 1, "nfcStats.scansCredits": creditsAwarded },
  });

  const user = await Hiker.findById(userId).select("socialCredits").lean();

  // Badge evaluation post-scan: potrebbe sbloccare checkpoint_collector / credit_*.
  evaluateAllBadges(userId).catch((err) => {
    console.error("[nfcService] badge eval fallita:", err.message);
  });

  return { ok: true, alreadyScannedToday: false, creditsAwarded, distance, totem, newTotalCredits: user?.socialCredits };
}

export async function getNfcHistory(userId, page = 1) {
  const limit = 20;
  const skip = (page - 1) * limit;
  return NfcScan.find({ userId, creditsAwarded: { $gt: 0 } })
    .sort({ scannedAt: -1 })
    .skip(skip)
    .limit(limit)
    .populate("totemId", "name kind location")
    .lean();
}

export async function createTotem(data) {
  return NfcTotem.create(data);
}
