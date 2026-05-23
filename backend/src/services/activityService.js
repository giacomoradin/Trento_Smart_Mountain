// CRUD attività libere (collezione separata da HikeSession).
// Authorization a livello service: ogni operazione verifica che userId === owner.
import Activity from "../models/activity.js";

export async function createActivity(userId, payload) {
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
  });
  await activity.save();
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
  const activities = await Activity.find({ userId }).lean();

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
