package it.trentosmartmountain.app.util

/**
 * Punti massimi del profilo altimetrico inviato al backend per la "banda
 * altimetrica" del feed ([it.trentosmartmountain.app.ui.screens.home.ElevationSparkline]).
 *
 * ~50 punti bastano per riconoscere la silhouette del dislivello in una card e
 * restano allineati al cap delle sessioni GPX (`gpxStatsSchema.elevationProfile`
 * lato server). Sotto al limite `createActivitySchema` (max 200), così entrambi
 * i path di upload — immediato e differito — restano validi.
 */
const val ELEVATION_PROFILE_MAX_POINTS = 50

/** Cap punti route signature inviati al backend (allineato a geoPolyline.js). */
const val ROUTE_SIGNATURE_MAX_POINTS = 80

/**
 * Riduce [points] ad al massimo [maxPoints] elementi campionando in modo
 * uniforme per indice. Primo e ultimo elemento sono sempre preservati.
 *
 * Mirror lato client di `backend/src/utils/geoPolyline.js`: il server ricampiona
 * comunque come hard cap, ma riduciamo qui per non sforare i limiti di
 * validazione (route signature, profilo altimetrico) e contenere il payload.
 *
 * Campionamento uniforme (NON Douglas-Peucker): per una thumbnail di feed conta
 * la forma complessiva, non la fedeltà geometrica fine. È O(n) e deterministico.
 *
 * @return lista campionata, oppure `null` se [points] ha meno di 2 elementi
 *         (1 punto non disegna né una linea né una sparkline).
 */
fun <T> downsampleByIndex(points: List<T>, maxPoints: Int): List<T>? {
  if (points.size < 2) return null
  if (points.size <= maxPoints) return points
  val step = (points.size - 1).toDouble() / (maxPoints - 1)
  val out = ArrayList<T>(maxPoints)
  for (i in 0 until maxPoints) {
    out.add(points[Math.round(i * step).toInt()])
  }
  // Math.round può non centrare l'ultimo indice per step frazionari: forzalo.
  out[out.size - 1] = points[points.size - 1]
  return out
}
