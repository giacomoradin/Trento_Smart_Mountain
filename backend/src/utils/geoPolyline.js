/**
 * Utility per il campionamento (downsampling) di polyline GPS.
 *
 * Usata in due punti:
 *   - activityService.createActivity → riduce la traccia ricevuta dal client a
 *     un massimo gestibile prima di persisterla (storage + payload feed).
 *   - socialService.toFeedItem → riduce ulteriormente la polyline alla
 *     risoluzione che serve a disegnare la "route signature" in una card
 *     (~50 punti bastano per riconoscere la forma del percorso).
 *
 * Strategia: campionamento uniforme per indice (NON Douglas-Peucker). Per una
 * thumbnail di feed la fedeltà geometrica fine è irrilevante: conta la forma
 * complessiva. Il campionamento uniforme è O(n), deterministico e mantiene
 * sempre primo e ultimo punto (start/end marker corretti).
 */

/**
 * Riduce `points` ad al massimo `maxPoints` elementi campionando in modo
 * uniforme per indice. Primo e ultimo punto sono sempre preservati.
 *
 * @param {Array<{lat:number, lon:number}>} points  polyline originale
 * @param {number} maxPoints  numero massimo di punti in output (>= 2)
 * @returns {Array<{lat:number, lon:number}>|undefined} polyline campionata,
 *          oppure `undefined` se l'input non è una polyline valida (< 2 punti).
 */
export function downsamplePolyline(points, maxPoints = 60) {
  if (!Array.isArray(points) || points.length < 2) return undefined;
  // Normalizza al solo {lat, lon} (scarta eventuali _id dei subdocument Mongoose).
  const clean = points
    .map((p) => ({ lat: p.lat, lon: p.lon }))
    .filter((p) => typeof p.lat === "number" && typeof p.lon === "number");
  if (clean.length < 2) return undefined;
  if (clean.length <= maxPoints) return clean;

  const step = (clean.length - 1) / (maxPoints - 1);
  const out = [];
  for (let i = 0; i < maxPoints; i++) {
    out.push(clean[Math.round(i * step)]);
  }
  // Garantisce che l'ultimo punto sia esattamente l'ultimo originale
  // (Math.round potrebbe non centrarlo per certi step frazionari).
  out[out.length - 1] = clean[clean.length - 1];
  return out;
}
