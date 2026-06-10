/**
 * Servizio Rifiuti & Logistica per rifugi (ADR-002, MVP read-only).
 *
 * Porta dentro TSM il modello del "Simulatore Gestione Rifiuti — Rifugi Alpini"
 * (https://biasio.github.io/simulatore-rifiuti-in-quota/), a sua volta derivato
 * dall'elaborato OGA "Gestione Rifiuti in Alta Quota" (gruppo ID-22):
 *   - bilancio di massa stagionale su categorie merceologiche (densità apparente);
 *   - pre-trattamento opzionale (riduzione % massa/volume per categoria);
 *   - verifica vincoli normativi di giacenza (art. 185-bis D.Lgs. 152/2006);
 *   - confronto costi tra vettori outbound: C_kg = (C_fix + c_var · t) / (P · S_t).
 *
 * Nessuna persistenza: il servizio è una funzione pura su parametri di input,
 * pensata per essere estesa in seguito con storico stagionale (WasteRecord).
 */

/** Limite volumetrico di stoccaggio temporaneo (m³) — art. 185-bis D.Lgs. 152/2006. */
export const STORAGE_LIMIT_M3 = 30;
/** Limite temporale di giacenza (giorni) — art. 185-bis D.Lgs. 152/2006. */
export const STORAGE_LIMIT_DAYS = 90;

/**
 * Categorie merceologiche del rifiuto "base" (percentuali sul totale prodotto).
 * `densityKgM3` = densità apparente; `storageCapacityM3` = capacità di stoccaggio
 * dedicata tipica al rifugio per quella frazione.
 */
export const WASTE_CATEGORIES = [
  { name: "Organico", percent: 45, densityKgM3: 700, storageCapacityM3: 1.5 },
  { name: "Plastica", percent: 12, densityKgM3: 60, storageCapacityM3: 2.0 },
  { name: "Vetro", percent: 18, densityKgM3: 1000, storageCapacityM3: 1.0 },
  { name: "Metalli", percent: 5, densityKgM3: 400, storageCapacityM3: 1.0 },
  { name: "Cartone", percent: 10, densityKgM3: 100, storageCapacityM3: 2.0 },
  { name: "Altro/indiff.", percent: 10, densityKgM3: 300, storageCapacityM3: 1.5 },
];

/** Grigliato (refluo pre-trattato): calcolato a parte, solo sugli ospiti pernottanti. */
export const SCREENING_CATEGORY = {
  name: "Grigliato",
  densityKgM3: 900,
  storageCapacityM3: 0.8,
};

/**
 * Vettori outbound disponibili con parametri di costo.
 * `fillFactor` = coefficiente di saturazione del carico (S_t).
 */
export const TRANSPORT_VECTORS = [
  { name: "Elicottero", payloadKg: 500, fixedCostEur: 450, variableCostEurMin: 30, travelTimeMin: 15, fillFactor: 0.9 },
  { name: "Drone cargo", payloadKg: 100, fixedCostEur: 0, variableCostEurMin: 0.5, travelTimeMin: 10, fillFactor: 0.95 },
  { name: "Teleferica", payloadKg: 300, fixedCostEur: 10, variableCostEurMin: 0, travelTimeMin: 0, fillFactor: 1.0 },
  { name: "Mezzo terrestre", payloadKg: 2000, fixedCostEur: 50, variableCostEurMin: 0, travelTimeMin: 0, fillFactor: 1.0 },
];

/** Configurazione esposta al client (categorie, vettori, limiti normativi). */
export function getWasteConfig() {
  return {
    categories: WASTE_CATEGORIES,
    screening: SCREENING_CATEGORY,
    vectors: TRANSPORT_VECTORS,
    limits: {
      storageLimitM3: STORAGE_LIMIT_M3,
      storageLimitDays: STORAGE_LIMIT_DAYS,
      reference: "art. 185-bis D.Lgs. 152/2006",
    },
  };
}

/**
 * Simulazione stagionale del bilancio rifiuti + costi di evacuazione.
 *
 * @param {object} params
 * @param {number} params.periodDays        durata stagione operativa (giorni)
 * @param {number} params.beds              posti letto totali
 * @param {number} params.bedOccupancy      coefficiente occupazione letti (0–1)
 * @param {number} params.dayVisitors       escursionisti giornalieri medi
 * @param {number} params.wastePerGuestKg   kg rifiuti/giorno per ospite pernottante
 * @param {number} params.wastePerVisitorKg kg rifiuti/giorno per escursionista
 * @param {number} params.screeningPerGuestKg kg grigliato/giorno per ospite
 * @param {boolean} params.compactorEnabled pre-trattamento attivo
 * @param {Array<{category:string, massReductionPct:number, volumeReductionPct:number}>} [params.reductions]
 *        riduzioni per categoria applicate solo se `compactorEnabled`
 */
export function simulateWaste(params) {
  const {
    periodDays,
    beds,
    bedOccupancy,
    dayVisitors,
    wastePerGuestKg,
    wastePerVisitorKg,
    screeningPerGuestKg,
    compactorEnabled = false,
    reductions = [],
  } = params;

  const overnightGuests = beds * bedOccupancy;
  const baseMassKg =
    overnightGuests * periodDays * wastePerGuestKg +
    dayVisitors * periodDays * wastePerVisitorKg;
  const screeningMassKg = overnightGuests * periodDays * screeningPerGuestKg;

  const reductionFor = (categoryName) => {
    if (!compactorEnabled) return { massPct: 0, volPct: 0 };
    const r = reductions.find((x) => x.category === categoryName);
    return {
      massPct: r?.massReductionPct ?? 0,
      volPct: r?.volumeReductionPct ?? 0,
    };
  };

  // Dettaglio per categoria (pre/post trattamento)
  const breakdown = WASTE_CATEGORIES.map((c) => {
    const preMassKg = baseMassKg * (c.percent / 100);
    const preVolumeM3 = preMassKg / c.densityKgM3;
    const { massPct, volPct } = reductionFor(c.name);
    return {
      name: c.name,
      preMassKg,
      preVolumeM3,
      postMassKg: preMassKg * (1 - massPct / 100),
      postVolumeM3: preVolumeM3 * (1 - volPct / 100),
      storageCapacityM3: c.storageCapacityM3,
    };
  });
  if (screeningMassKg > 0) {
    const vol = screeningMassKg / SCREENING_CATEGORY.densityKgM3;
    breakdown.push({
      name: SCREENING_CATEGORY.name,
      preMassKg: screeningMassKg,
      preVolumeM3: vol,
      postMassKg: screeningMassKg,
      postVolumeM3: vol,
      storageCapacityM3: SCREENING_CATEGORY.storageCapacityM3,
    });
  }

  const totalPreMassKg = baseMassKg + screeningMassKg;
  const totalPostMassKg = breakdown.reduce((s, d) => s + d.postMassKg, 0);
  const totalPreVolumeM3 = breakdown.reduce((s, d) => s + d.preVolumeM3, 0);
  const totalPostVolumeM3 = breakdown.reduce((s, d) => s + d.postVolumeM3, 0);

  // Compliance: primo giorno di superamento per categoria, limite 90 gg, limite 30 m³.
  const alerts = [];
  let criticalDay = Infinity;
  let criticalCause = null;
  for (const d of breakdown) {
    const dailyVolume = periodDays > 0 ? d.postVolumeM3 / periodDays : 0;
    if (dailyVolume > 0 && d.storageCapacityM3 > 0) {
      const overflowDay = Math.ceil(d.storageCapacityM3 / dailyVolume);
      if (overflowDay <= periodDays) {
        alerts.push({
          type: "STORAGE_CAPACITY",
          category: d.name,
          day: overflowDay,
          message: `Superamento capacità stoccaggio per "${d.name}" al giorno ${overflowDay}: necessario svuotamento.`,
        });
        if (overflowDay < criticalDay) {
          criticalDay = overflowDay;
          criticalCause = d.name;
        }
      }
    }
  }
  if (periodDays > STORAGE_LIMIT_DAYS) {
    alerts.push({
      type: "STORAGE_DAYS_LIMIT",
      day: STORAGE_LIMIT_DAYS,
      message: `Il periodo operativo supera il limite di giacenza di ${STORAGE_LIMIT_DAYS} giorni (art. 185-bis D.Lgs. 152/2006): svuotamento obbligatorio entro il limite.`,
    });
    if (STORAGE_LIMIT_DAYS < criticalDay) {
      criticalDay = STORAGE_LIMIT_DAYS;
      criticalCause = "Limite 90 giorni (art. 185-bis)";
    }
  }
  const dailyTotalVolume = periodDays > 0 ? totalPostVolumeM3 / periodDays : 0;
  if (dailyTotalVolume > 0) {
    const volumeLimitDay = Math.ceil(STORAGE_LIMIT_M3 / dailyTotalVolume);
    if (volumeLimitDay <= periodDays) {
      alerts.push({
        type: "STORAGE_VOLUME_LIMIT",
        day: volumeLimitDay,
        message: `Superamento stimato dei ${STORAGE_LIMIT_M3} m³ al giorno ${volumeLimitDay} (accumulo continuo senza evacuazione, art. 185-bis).`,
      });
      if (volumeLimitDay < criticalDay) {
        criticalDay = volumeLimitDay;
        criticalCause = "Limite 30 m³ (art. 185-bis)";
      }
    }
  }

  // Costi di evacuazione per vettore: C_kg = (C_fix + c_var·t) / (P·S_t)
  const vectors = TRANSPORT_VECTORS.map((v) => {
    const effectivePayloadKg = v.payloadKg * v.fillFactor;
    if (effectivePayloadKg <= 0 || totalPostMassKg <= 0) {
      return { name: v.name, trips: 0, totalCostEur: 0, costPerKgEur: 0, avgSaturationPct: 0 };
    }
    const trips = Math.ceil(totalPostMassKg / effectivePayloadKg);
    const tripCostEur = v.fixedCostEur + v.variableCostEurMin * v.travelTimeMin;
    const totalCostEur = trips * tripCostEur;
    return {
      name: v.name,
      effectivePayloadKg,
      trips,
      totalCostEur,
      costPerKgEur: totalCostEur / totalPostMassKg,
      avgSaturationPct: (totalPostMassKg / (trips * v.payloadKg)) * 100,
    };
  });
  const cheapest = vectors
    .filter((v) => v.trips > 0)
    .reduce((best, v) => (best === null || v.totalCostEur < best.totalCostEur ? v : best), null);

  return {
    input: { periodDays, beds, bedOccupancy, dayVisitors, overnightGuests },
    totals: {
      preMassKg: round1(totalPreMassKg),
      postMassKg: round1(totalPostMassKg),
      preVolumeM3: round3(totalPreVolumeM3),
      postVolumeM3: round3(totalPostVolumeM3),
      massReductionPct: totalPreMassKg > 0 ? round1(((totalPreMassKg - totalPostMassKg) / totalPreMassKg) * 100) : 0,
      volumeReductionPct: totalPreVolumeM3 > 0 ? round1(((totalPreVolumeM3 - totalPostVolumeM3) / totalPreVolumeM3) * 100) : 0,
    },
    breakdown: breakdown.map((d) => ({
      name: d.name,
      preMassKg: round1(d.preMassKg),
      preVolumeM3: round3(d.preVolumeM3),
      postMassKg: round1(d.postMassKg),
      postVolumeM3: round3(d.postVolumeM3),
      storageCapacityM3: d.storageCapacityM3,
    })),
    compliance: {
      alerts,
      criticalDay: criticalDay === Infinity ? null : criticalDay,
      criticalCause,
    },
    vectors: vectors.map((v) => ({
      ...v,
      totalCostEur: round1(v.totalCostEur),
      costPerKgEur: round2(v.costPerKgEur),
      avgSaturationPct: round1(v.avgSaturationPct),
    })),
    cheapestVector: cheapest?.name ?? null,
  };
}

const round1 = (n) => Math.round(n * 10) / 10;
const round2 = (n) => Math.round(n * 100) / 100;
const round3 = (n) => Math.round(n * 1000) / 1000;
