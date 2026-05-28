const LEVELS = [
  { lv: 1,  name: "Sentiero",         min: 0,     max: 249 },
  { lv: 2,  name: "Rifugio",          min: 250,   max: 499 },
  { lv: 3,  name: "Bivacco",          min: 500,   max: 999 },
  { lv: 4,  name: "Alpinista",        min: 1000,  max: 1499 },
  { lv: 5,  name: "Cima",             min: 1500,  max: 2499 },
  { lv: 6,  name: "Esploratore",      min: 2500,  max: 3999 },
  { lv: 7,  name: "Veterano",         min: 4000,  max: 5999 },
  { lv: 8,  name: "Guida Alpina",     min: 6000,  max: 8999 },
  { lv: 9,  name: "Maestro",          min: 9000,  max: 12999 },
  { lv: 10, name: "Leggenda Alpina",  min: 13000, max: Infinity },
];

export function computeLevel(credits) {
  const current = LEVELS.findLast((l) => credits >= l.min) ?? LEVELS[0];
  const next = LEVELS.find((l) => l.lv === current.lv + 1) ?? null;
  const range = current.max === Infinity ? 1 : current.max - current.min + 1;
  const progress = current.max === Infinity ? 1 : (credits - current.min) / range;
  return {
    lv: current.lv,
    name: current.name,
    min: current.min,
    max: current.max === Infinity ? null : current.max,
    next: next ? { lv: next.lv, name: next.name, min: next.min } : null,
    progressPct: Math.min(1, Math.max(0, progress)),
    creditsToNext: next ? Math.max(0, next.min - credits) : 0,
  };
}
