// CRUD attività libere (collezione separata da HikeSession).
// Authorization a livello service: ogni operazione verifica che userId === owner.
import Activity from "../models/activity.js";
import User from "../models/user.js";
import { addCredits } from "./creditService.js";
import { applyBaselineMultiplier } from "./userScoringService.js";
import { evaluateAllBadges } from "./badgeService.js";
import { downsamplePolyline } from "../utils/geoPolyline.js";

export async function createActivity(userId, payload) {
  const sourceSessionId = payload.sourceSessionId || null;

  // Copia personale di una sessione di gruppo (ADR-001): idempotente per
  // coppia (userId, sourceSessionId). Il client può ritentare l'upload
  // (stop → retry SyncManager → re-login) senza creare duplicati: il
  // secondo invio aggiorna la copia esistente e ritorna lo stesso _id.
  if (sourceSessionId) {
    const existing = await Activity.findOne({ userId, sourceSessionId });
    if (existing) {
      existing.name = payload.name ?? existing.name;
      existing.actualStats = payload.actualStats ?? existing.actualStats;
      if (payload.elevationProfile?.length) {
        existing.elevationProfile = payload.elevationProfile;
      }
      if (payload.routePolyline?.length) {
        existing.routePolyline = downsamplePolyline(payload.routePolyline, 80);
      }
      await existing.save();
      return existing;
    }
  }

  const activity = new Activity({
    userId,
    name: payload.name,
    activityType: payload.activityType || "hiking",
    difficultyLevel: payload.difficultyLevel,
    startTimeMs: payload.startTimeMs,
    endTimeMs: payload.endTimeMs,
    completedAt: new Date(payload.endTimeMs),
    actualStats: payload.actualStats,
    elevationProfile: payload.elevationProfile,
    sourceSessionId,
    // Persiste la traccia GPS ricampionata (max 80 punti) per la route
    // signature del feed. Il client invia già downsampled, ma ricampioniamo
    // lato server come hard cap difensivo (storage + payload feed bounded).
    routePolyline: downsamplePolyline(payload.routePolyline, 80),
  });
  await activity.save();

  // Accredito crediti per attività libere: stesso modello delle sessioni di gruppo,
  // con μ_user_baseline applicato. Idempotency via refId+source unique combination
  // (vedi creditTransaction): un secondo POST con stesso payload non genera doppio
  // accredito perché crea una nuova Activity con _id diverso → comportamento atteso
  // (l'utente può creare tante attività quante ne vuole).
  // ECCEZIONE: le copie personali di sessione NON accreditano nulla — i crediti
  // della sessione sono già stati accreditati da completeSession (per-utente).
  const basePoints = sourceSessionId ? 0 : (activity.actualStats?.finalPoints ?? 0);
  if (basePoints > 0) {
    const user = await User.findById(userId).select("experience").lean();
    const credits = applyBaselineMultiplier(basePoints, user, activity.difficultyLevel);
    if (credits > 0) {
      await addCredits({
        userId,
        amount: credits,
        source: "free_activity",
        refId: activity._id,
        refKind: "Activity",
        note: credits !== basePoints ? `baseline μ applicato (base=${basePoints})` : undefined,
      });
    }
  }

  // Badge evaluation post-create — fire-and-forget.
  evaluateAllBadges(userId).catch((err) => {
    console.error("[activityService] badge eval fallita:", err.message);
  });

  return activity;
}

export async function getActivitiesByUser(userId) {
  return Activity.find({ userId }).sort({ completedAt: -1 }).lean();
}

export async function getActivityById(activityId, userId) {
  const activity = await Activity.findById(activityId).lean();
  if (!activity) throw new Error("ACTIVITY_NOT_FOUND");
  if (activity.userId.toString() !== userId.toString()) throw new Error("FORBIDDEN");
  return activity;
}

export async function deleteActivity(activityId, userId) {
  const activity = await Activity.findById(activityId);
  if (!activity) throw new Error("ACTIVITY_NOT_FOUND");
  if (activity.userId.toString() !== userId.toString()) throw new Error("FORBIDDEN");
  await activity.deleteOne();
}

// Unifica HikeSession.COMPLETED + Activity per le aggregate annuali della HOME.
// Riceve già le stats delle sessioni e ci somma le attività libere dell'anno.
export async function getCombinedActivityStats(userId, year, hikeSessionStats) {
  // Esclude le copie personali di sessione (sourceSessionId valorizzato): la
  // HikeSession di origine è già contata in hikeSessionStats — includerle
  // raddoppierebbe km/dislivello/punti per ogni uscita di gruppo.
  const activities = await Activity.find({
    userId,
    sourceSessionId: null,
  }).lean();

  const diffScore = { T: 0.25, E: 0.5, EE: 0.75, EEA: 1.0 };
  const monthlyCount = [...hikeSessionStats.monthlyActivityCount];
  const monthlyDiffSum = monthlyCount.map((_, i) => hikeSessionStats.monthlyAvgDifficulty[i] || 0);
  const monthlyDiffN = monthlyCount.map((c) => (c > 0 ? 1 : 0));

  let totalDist = hikeSessionStats.totalDistanceKm;
  let totalElev = hikeSessionStats.totalElevationGainM;
  let totalPoints = hikeSessionStats.totalPoints;
  let yearCount = hikeSessionStats.totalActivities;

  activities.forEach((a) => {
    const ref = a.completedAt;
    if (!ref) return;
    const d = new Date(ref);
    if (d.getFullYear() !== year) return;
    const m = d.getMonth();
    monthlyCount[m]++;
    yearCount++;
    const distKm = (a.actualStats?.distanceMeters || 0) / 1000.0;
    totalDist += distKm;
    totalElev += a.actualStats?.elevationGainM || 0;
    totalPoints += a.actualStats?.finalPoints || 0;
    const score = diffScore[a.difficultyLevel] ?? 0.5;
    monthlyDiffSum[m] += score;
    monthlyDiffN[m]++;
  });

  return {
    year,
    totalActivities: yearCount,
    totalDistanceKm: Math.round(totalDist * 10) / 10,
    totalElevationGainM: totalElev,
    totalPoints,
    monthlyActivityCount: monthlyCount,
    monthlyAvgDifficulty: monthlyCount.map((_, i) =>
      monthlyDiffN[i] > 0 ? monthlyDiffSum[i] / monthlyDiffN[i] : 0,
    ),
  };
}
