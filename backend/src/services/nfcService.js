import NfcTotem from "../models/nfcTotem.js";
import NfcScan from "../models/nfcScan.js";
import User from "../models/user.js";
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

  const cutoff = new Date(Date.now() - 24 * 60 * 60 * 1000);
  const existing = await NfcScan.findOne({
    userId,
    totemId: totem._id,
    scannedAt: { $gte: cutoff },
    creditsAwarded: { $gt: 0 },
  }).lean();

  if (existing) {
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

  const creditsAwarded = totem.creditsReward;
  await NfcScan.create({
    userId,
    totemId: totem._id,
    tagId,
    gpsLocation: { type: "Point", coordinates: [gpsLon, gpsLat] },
    distanceFromTotem: distance,
    creditsAwarded,
  });

  await addCredits({ userId, amount: creditsAwarded, source: "nfc", refId: totem._id, refKind: "NfcTotem" });
  await User.findByIdAndUpdate(userId, {
    $inc: { "nfcStats.scansCount": 1, "nfcStats.scansCredits": creditsAwarded },
  });

  const user = await User.findById(userId).select("socialCredits").lean();

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
