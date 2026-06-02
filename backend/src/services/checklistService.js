/**
 * Logica pura di generazione della checklist per un'escursione.
 *
 * La funzione principale è `generateChecklist(sentiero, forecastResult?)`.
 * È una funzione pura e idempotente: stessi input → stesso output.
 * Viene chiamata sia da createChecklist che da updateChecklist nel router.
 *
 * Dipendenze esterne:
 *   - sentieroService  → getSentieroByCode  (o passa direttamente il doc)
 *   - weatherService   → getLocationForecast
 */

// ─── Costanti di soglia ───────────────────────────────────────────────────────

// Quota in metri sopra la quale si considera terreno alpino con rischio ghiaccio/neve
const QUOTA_ALPINA = 2000;
const QUOTA_GHIACCIAIO = 2800;

// Difficoltà CAI ordinate per gravità
const DIFFICOLTA_ORDER = { T: 0, E: 1, EE: 2, EEA: 3 };

// Soglie meteo
const SOGLIA_PIOGGIA_PROB   = 40;  // % — sopra questa soglia si considera probabile pioggia
const SOGLIA_VENTO_FORTE    = 50;  // km/h
const SOGLIA_TEMP_FREDDA    = 5;   // °C — sotto questa si aggiunge strato isolante
const SOGLIA_TEMP_MOLTO_FREDDA = -5; // °C

// Acqua: litri/ora base per difficoltà (stima prudente, non sovradimensionata)
const ACQUA_LPH = { T: 0.30, E: 0.35, EE: 0.42, EEA: 0.50 };

// Calorie: kcal/ora base per difficoltà (approssimazione media adulto 70kg)
const KCAL_PH = { T: 250, E: 350, EE: 450, EEA: 550 };

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Converte una stringa "HH:MM" in ore decimali.
 * Es. "03:30" → 3.5
 */
function parseOre(tempoStr) {
  if (!tempoStr || typeof tempoStr !== 'string') return 0;
  const [hh, mm] = tempoStr.split(':').map(Number);
  return (hh || 0) + (mm || 0) / 60;
}

function formatTempoAndata(oreDecimali) {
  const h = Math.floor(oreDecimali);
  const m = Math.round((oreDecimali - h) * 60);
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/** Stima durata escursione da sessione GPX (durata GPX o formula CAI). */
export function estimateOreFromSession(session) {
  const gs = session.gpxStats || {};
  if (gs.gpxDurationSec && gs.gpxDurationSec > 0) {
    return gs.gpxDurationSec / 3600;
  }
  const dist = gs.distanceKm ?? 0;
  const elev = gs.elevationGainM ?? session.routeDetails?.elevationGain ?? 0;
  if (dist > 0) return dist / 4 + elev / 600;
  return 0;
}

/**
 * Adatta i dati di una sessione GPX/import al formato atteso da generateChecklist.
 * Permette checklist dinamica anche senza sentieroCode SAT.
 */
export function buildSentieroLikeFromSession(session) {
  const rd = session.routeDetails || {};
  const gs = session.gpxStats || {};
  const ore = estimateOreFromSession(session);
  if (ore <= 0 && !gs.distanceKm) return null;

  let quotaMassima = null;
  let quotaMinima = null;
  const profile = gs.elevationProfile;
  if (Array.isArray(profile) && profile.length > 0) {
    quotaMinima = Math.min(...profile);
    quotaMassima = Math.max(...profile);
  } else {
    const gain = gs.elevationGainM ?? rd.elevationGain ?? 0;
    if (gain > 0) {
      quotaMinima = 600;
      quotaMassima = quotaMinima + gain;
    }
  }

  return {
    difficolta: rd.difficultyLevel || 'E',
    tempoAndata: ore > 0 ? formatTempoAndata(ore) : '03:00',
    quotaMassima,
    quotaMinima,
    denominazione: rd.name || 'Percorso importato',
  };
}

function isViaFerrata(sentiero) {
  const label = `${sentiero.denominazione || ''} ${sentiero.codice || ''}`.toLowerCase();
  return /ferrata|\bvf\b|via ferrata|sentiero attrezzato|attrezzat/.test(label);
}

function isEscursioneImpegnativa(sentiero) {
  const ore = parseOre(sentiero.tempoAndata);
  const diffLevel = DIFFICOLTA_ORDER[sentiero.difficolta || 'E'] ?? 1;
  const dislivello = (sentiero.quotaMassima ?? 0) - (sentiero.quotaMinima ?? 0);
  return diffLevel >= DIFFICOLTA_ORDER.EE || ore >= 6 || dislivello >= 1200;
}

/**
 * Calcola acqua in litri per l'intera escursione.
 * Aumenta del 20% se caldo (temp media > 20°C), del 10% se dislivello elevato.
 */
function calcolaAcqua(sentiero, meteoAgg) {
  const ore        = parseOre(sentiero.tempoAndata);
  const difficolta = sentiero.difficolta || 'E';
  const lph        = ACQUA_LPH[difficolta] ?? 0.35;
  let litri        = ore * lph;

  if (meteoAgg?.temperaturaMed > 22) litri *= 1.12;
  const dislivello = (sentiero.quotaMassima ?? 0) - (sentiero.quotaMinima ?? 0);
  if (dislivello > 1500) litri *= 1.05;

  // Tetto prudente: evita stime eccessive su escursioni lunghe
  const cap = Math.max(1.5, ore * 0.55);
  litri = Math.min(litri, cap);

  return Math.round(litri * 10) / 10;
}

/**
 * Calcola fabbisogno calorico stimato (kcal) per l'escursione.
 */
function calcolaCalorie(sentiero) {
  const ore        = parseOre(sentiero.tempoAndata);
  const difficolta = sentiero.difficolta || 'E';
  const kcalPh     = KCAL_PH[difficolta] ?? 350;
  return Math.round(ore * kcalPh);
}

/**
 * Aggrega le metriche meteo dagli slot 3h coperti dalla durata del sentiero.
 * Restituisce null se non ci sono slot disponibili.
 *
 * @param {Object[]} slots3h   — array di forecastSlot (validFrom, validTo, ...)
 * @param {Date}     partenza  — ora di partenza prevista
 * @param {number}   oreStima  — durata stimata dell'escursione in ore
 */
function aggregaMeteo(slots3h, partenza, oreStima) {
  if (!slots3h || slots3h.length === 0) return null;

  const fine    = new Date(partenza.getTime() + oreStima * 3_600_000);
  const rilevanti = slots3h.filter(s => {
    const vf = new Date(s.validFrom);
    const vt = new Date(s.validTo);
    return vf < fine && vt > partenza;
  });

  if (rilevanti.length === 0) return null;

  const temps     = rilevanti.map(s => s.temperature ?? s.temperatureMin).filter(v => v != null);
  const piogge    = rilevanti.map(s => s.rainProbability).filter(v => v != null);
  const venti     = rilevanti.map(s => s.windSpeed).filter(v => v != null);
  const neve      = rilevanti.map(s => s.freshSnow ?? 0);
  const zeroTerm  = rilevanti.map(s => s.freezingLevel).filter(v => v != null);

  return {
    temperaturaMin:    temps.length   ? Math.min(...temps)        : null,
    temperaturaMed:    temps.length   ? temps.reduce((a,b)=>a+b,0)/temps.length : null,
    pioggiaProbMax:    piogge.length  ? Math.max(...piogge)       : null,
    neveFrescaTotale:  neve.reduce((a,b) => a+b, 0),
    ventoMax:          venti.length   ? Math.max(...venti)        : null,
    quotaZeroTermico:  zeroTerm.length ? Math.min(...zeroTerm)    : null,
  };
}

// ─── Builder categorie ────────────────────────────────────────────────────────

/**
 * Genera la categoria ABBIGLIAMENTO in base a difficoltà, quota e meteo.
 */
function buildAbbigliamento(sentiero, meteo) {
  const diff        = sentiero.difficolta || 'E';
  const quotaMax    = sentiero.quotaMassima ?? 0;
  const diffLevel   = DIFFICOLTA_ORDER[diff] ?? 1;
  const tempMin     = meteo?.temperaturaMin ?? 15;
  const pioggia     = meteo?.pioggiaProbMax ?? 0;
  const vento       = meteo?.ventoMax ?? 0;
  const neveCm      = meteo?.neveFrescaTotale ?? 0;
  const zeroTerm    = meteo?.quotaZeroTermico ?? 4000;

  const base = [];
  const consigliati = [];
  const opzionali = [];

  // ── Base: sempre presenti ──
  if (diffLevel >= DIFFICOLTA_ORDER['E']) {
    base.push({
      nome: 'Scarponi da trekking con suola Vibram',
      motivo: `Sentiero ${diff}: terreno irregolare che richiede appoggio stabile alla caviglia.`,
    });
  } else {
    base.push({
      nome: 'Scarpe da camminata con suola antiscivolo',
      motivo: 'Sentiero turistico: sufficiente una scarpa da trekking leggera.',
    });
  }

  base.push({
    nome: 'Calzini tecnici in lana merino o sintetico',
    motivo: 'Prevengono vesciche e gestiscono l\'umidità.',
  });

  base.push({
    nome: diffLevel >= 2 ? 'Pantaloni lunghi da trekking' : 'Pantaloni da trekking (lunghi o convertibili)',
    motivo: diffLevel >= 2
      ? 'Proteggono da arbusti, rocce e insetti su terreno impegnativo.'
      : 'I convertibili offrono flessibilità al variare della temperatura.',
  });

  base.push({
    nome: 'Maglietta tecnica a maniche lunghe traspirante',
    motivo: 'Base layer che gestisce il sudore e protegge dal sole a quota.',
  });

  base.push({
    nome: 'Cappellino con visiera o berretto tecnico',
    motivo: 'Protezione solare/termica in base alla stagione.',
  });

  const guantiConsigliati = quotaMax > QUOTA_ALPINA
    || tempMin < SOGLIA_TEMP_FREDDA
    || diffLevel >= DIFFICOLTA_ORDER.EE;
  if (guantiConsigliati) {
    consigliati.push({
      nome: 'Guanti da trekking leggeri',
      motivo: quotaMax > QUOTA_ALPINA
        ? `Quota massima ${quotaMax} m: temperature più basse in vetta.`
        : tempMin < SOGLIA_TEMP_FREDDA
          ? `Temperatura minima prevista ${tempMin}°C.`
          : `Sentiero ${diff}: terreno impegnativo, mani esposte al freddo/vento.`,
    });
  }

  // ── Condizioni fredde ──
  if (tempMin < SOGLIA_TEMP_FREDDA || quotaMax > QUOTA_ALPINA) {
    base.push({
      nome: 'Pile o felpa in pile mid-layer',
      motivo: tempMin < SOGLIA_TEMP_FREDDA
        ? `Temperatura minima prevista ${tempMin}°C: strato isolante indispensabile.`
        : `Quota ${quotaMax} m: differenziale termico rispetto al fondovalle tipicamente -6/8°C per km.`,
    });
  }

  if (tempMin < SOGLIA_TEMP_MOLTO_FREDDA) {
    base.push({
      nome: 'Giubbotto isolante (piumino o sintetico)',
      motivo: `Temperatura minima prevista ${tempMin}°C: necessario strato isolante pesante.`,
    });
    base.push({
      nome: 'Guanti invernali impermeabili',
      motivo: 'Proteggono da frostbite in condizioni di freddo intenso.',
    });
    base.push({
      nome: 'Balaclava o fascia copri-orecchie',
      motivo: 'Protezione testa e orecchie dal freddo sotto zero.',
    });
  }

  // ── Pioggia ──
  if (pioggia >= SOGLIA_PIOGGIA_PROB) {
    base.push({
      nome: 'Giacca impermeabile e traspirante (Gore-Tex o equivalente)',
      motivo: `Probabilità pioggia ${pioggia}%: impermeabile indispensabile.`,
    });
    base.push({
      nome: 'Pantaloni impermeabili o ghette impermeabili',
      motivo: 'Completano la protezione dalla pioggia nelle gambe.',
    });
  } else if (pioggia > 0 && pioggia < SOGLIA_PIOGGIA_PROB) {
    consigliati.push({
      nome: 'Giacca impermeabile leggera',
      motivo: `Probabilità pioggia ${pioggia}%: bassa ma non nulla. Vale la pena portarla nello zaino.`,
    });
  }

  // ── Vento ──
  if (vento >= SOGLIA_VENTO_FORTE) {
    consigliati.push({
      nome: 'Giacca antivento',
      motivo: `Vento previsto fino a ${vento} km/h: riduce sensibilmente la temperatura percepita.`,
    });
  }

  // ── EEA: attrezzatura specifica ──
  if (diff === 'EEA') {
    base.push({
      nome: 'Scarponi da alpinismo con suola rigida (compatibili con ramponi)',
      motivo: 'Sentiero EEA: terreno che può richiedere l\'uso di ramponi o piccozza.',
    });
  }

  return [
    ...(base.length        ? [{ nome: 'Abbigliamento', livello: 'base',        items: base }]        : []),
    ...(consigliati.length ? [{ nome: 'Abbigliamento', livello: 'consigliato', items: consigliati }] : []),
    ...(opzionali.length   ? [{ nome: 'Abbigliamento', livello: 'opzionale',   items: opzionali }]   : []),
  ];
}

/**
 * Genera la categoria ATTREZZATURA in base a difficoltà e quota.
 */
function buildAttrezzatura(sentiero, meteo) {
  const diff      = sentiero.difficolta || 'E';
  const quotaMax  = sentiero.quotaMassima ?? 0;
  const diffLevel = DIFFICOLTA_ORDER[diff] ?? 1;
  const neveCm    = meteo?.neveFrescaTotale ?? 0;
  const zeroTerm  = meteo?.quotaZeroTermico ?? 4000;

  const base = [];
  const consigliati = [];
  const opzionali = [];

  // ── Base: sempre ──
  base.push({
    nome: 'Zaino da trekking (25–35L per uscite di un giorno)',
    motivo: 'Contenitore per acqua, cibo, indumenti e kit di emergenza.',
  });

  consigliati.push({
    nome: 'Bastoncini da trekking regolabili',
    motivo: diffLevel >= DIFFICOLTA_ORDER.EE
      ? 'Riducono il carico sulle ginocchia su sentieri impegnativi.'
      : 'Migliorano stabilità e comfort; utili ma non indispensabili.',
  });

  base.push({
    nome: 'Borraccia o sistema idratazione',
    motivo: 'Contenitore per l\'acqua calcolata in base a durata e difficoltà.',
  });

  base.push({
    nome: 'Mappa cartacea del sentiero o traccia GPX scaricata offline',
    motivo: 'Il segnale GPS/dati in quota può essere assente. La mappa offline è salvavita.',
  });

  opzionali.push({
    nome: 'Powerbank per smartphone',
    motivo: 'Utile se usi GPS/navigatione a lungo; non indispensabile con batteria piena.',
  });

  const ferrata = isViaFerrata(sentiero);
  if (diff === 'EEA' || ferrata) {
    base.push({
      nome: 'Casco da via ferrata o alpinismo',
      motivo: ferrata
        ? 'Percorso attrezzato/ferrata: protezione obbligatoria da caduta massi.'
        : `Sentiero ${diff}: terreno esposto con rischio caduta.`,
    });
  } else if (diffLevel >= DIFFICOLTA_ORDER.EE) {
    opzionali.push({
      nome: 'Casco da via ferrata o alpinismo',
      motivo: 'Consigliato su tratti esposti; obbligatorio solo su vie ferrate attrezzate.',
    });
  }

  if (diff === 'EEA') {
    base.push({
      nome: 'Imbragatura da ferrata con kit assorbitori di energia',
      motivo: 'Sentiero EEA: attrezzatura di progressione su terreno attrezzato/alpinistico.',
    });
  }

  // ── Quota e neve/ghiaccio ──
  const ghiaccioRischio = diff === 'EEA' || (quotaMax > QUOTA_GHIACCIAIO) || (zeroTerm < quotaMax + 200) || neveCm > 5;

  if (ghiaccioRischio) {
    base.push({
      nome: 'Ramponi a 10/12 punte (compatibili con i tuoi scarponi)',
      motivo: [
        diff === 'EEA' ? 'Sentiero EEA.' : '',
        quotaMax > QUOTA_GHIACCIAIO ? `Quota ${quotaMax} m con possibile ghiaccio permanente.` : '',
        zeroTerm < quotaMax + 200 ? `Quota zero termico (${zeroTerm} m) vicina alla quota massima: ghiaccio probabile.` : '',
        neveCm > 5 ? `${neveCm} cm di neve fresca previsti.` : '',
      ].filter(Boolean).join(' '),
    });
    base.push({
      nome: 'Piccozza da alpinismo (50–60 cm)',
      motivo: 'Fondamentale per l\'autoarresto su pendii nevosi o glaciali.',
    });
  } else if (quotaMax > QUOTA_ALPINA && neveCm > 0) {
    consigliati.push({
      nome: 'Mini-ramponi o ramponcini antiscivolo (microspikes)',
      motivo: `Quota ${quotaMax} m con ${neveCm} cm di neve: tratti gelati possibili ma non dominanti.`,
    });
  }

  if (quotaMax > QUOTA_ALPINA) {
    opzionali.push({
      nome: 'Occhiali da sole con protezione UV cat. 3–4',
      motivo: `A ${quotaMax} m l\'irradiazione UV è sensibilmente più alta. Neve o ghiaccio amplificano l\'effetto.`,
    });
    opzionali.push({
      nome: 'Crema solare SPF 50+',
      motivo: 'La protezione solare in quota è spesso sottovalutata.',
    });
  }

  return [
    ...(base.length        ? [{ nome: 'Attrezzatura', livello: 'base',        items: base }]        : []),
    ...(consigliati.length ? [{ nome: 'Attrezzatura', livello: 'consigliato', items: consigliati }] : []),
    ...(opzionali.length   ? [{ nome: 'Attrezzatura', livello: 'opzionale',   items: opzionali }]   : []),
  ];
}

/**
 * Genera la categoria SICUREZZA.
 */
function buildSicurezza(sentiero, meteo) {
  const diff      = sentiero.difficolta || 'E';
  const diffLevel = DIFFICOLTA_ORDER[diff] ?? 1;
  const quotaMax  = sentiero.quotaMassima ?? 0;
  const pioggia   = meteo?.pioggiaProbMax ?? 0;

  const base = [];
  const consigliati = [];
  const opzionali = [];

  base.push({
    nome: 'Kit di primo soccorso compatto (cerotti, bende, disinfettante, garze)',
    motivo: 'Essenziale per qualsiasi escursione, da T ad EEA.',
  });

  opzionali.push({
    nome: 'Fischietto di emergenza',
    motivo: 'Segnale acustico leggero per richiamare attenzione in caso di necessità.',
  });

  base.push({
    nome: 'Coperta termica di emergenza (space blanket)',
    motivo: 'In caso di ipotermia o infortunio, riduce drasticamente la dispersione di calore.',
  });

  consigliati.push({
    nome: 'Torcia frontale con batterie di riserva',
    motivo: 'Utile in caso di ritardo imprevisto; consigliata se il rientro può avvenire al crepuscolo.',
  });

  if (diffLevel >= DIFFICOLTA_ORDER['EE'] || quotaMax > QUOTA_ALPINA) {
    consigliati.push({
      nome: 'Comunicatore satellitare (es. Garmin inReach Mini)',
      motivo: `${diff} a ${quotaMax} m: le reti mobili sono spesso assenti. Il segnale satellitare garantisce i soccorsi.`,
    });
  }

  if (pioggia >= SOGLIA_PIOGGIA_PROB) {
    consigliati.push({
      nome: 'Sacchetto impermeabile per lo zaino (rain cover)',
      motivo: `Pioggia ${pioggia}%: protegge contenuti critici (documenti, dispositivi elettronici).`,
    });
  }

  if (isEscursioneImpegnativa(sentiero)) {
    opzionali.push({
      nome: 'Barrette energetiche di emergenza',
      motivo: 'Scorta calorica extra consigliata su escursioni lunghe o molto impegnative.',
    });
  }

  return [
    ...(base.length        ? [{ nome: 'Sicurezza', livello: 'base',        items: base }]        : []),
    ...(consigliati.length ? [{ nome: 'Sicurezza', livello: 'consigliato', items: consigliati }] : []),
    ...(opzionali.length   ? [{ nome: 'Sicurezza', livello: 'opzionale',   items: opzionali }]   : []),
  ];
}

/**
 * Genera la categoria ALIMENTAZIONE.
 */
function buildAlimentazione(sentiero, meteoAgg) {
  const ore        = parseOre(sentiero.tempoAndata);
  const diff       = sentiero.difficolta || 'E';
  const diffLevel  = DIFFICOLTA_ORDER[diff] ?? 1;
  const kcal       = calcolaCalorie(sentiero);
  const acqua      = calcolaAcqua(sentiero, meteoAgg);
  const quotaMax   = sentiero.quotaMassima ?? 0;

  const base = [];
  const consigliati = [];
  const opzionali = [];

  base.push({
    nome: `Acqua: almeno ${acqua} litri`,
    motivo: `Calcolati su ${ore.toFixed(1)}h di percorrenza a difficoltà ${diff}${meteoAgg?.temperaturaMed > 22 ? ', leggermente aumentati per caldo' : ''}.`,
  });

  base.push({
    nome: `Cibo: circa ${kcal} kcal stimate`,
    motivo: `Fabbisogno energetico stimato per ${ore.toFixed(1)}h di escursione a difficoltà ${diff} (~70 kg adulto medio).`,
  });

  base.push({
    nome: 'Pranzo al sacco (panino, formaggio, frutta secca)',
    motivo: ore > 3 ? 'Escursione > 3h: necessario un pasto completo.' : 'Spuntino sostanzioso per sostenere il percorso.',
  });

  base.push({
    nome: 'Frutta secca o mix di noci (energetico, leggero)',
    motivo: 'Snack ad alto apporto calorico e facilmente trasportabile.',
  });

  if (isEscursioneImpegnativa(sentiero)) {
    consigliati.push({
      nome: 'Barrette energetiche o gel (2–3 pezzi)',
      motivo: `Escursione impegnativa (${ore.toFixed(1)}h, difficoltà ${diff}): utili per mantenere la glicemia costante.`,
    });
  }

  if (quotaMax > QUOTA_ALPINA) {
    consigliati.push({
      nome: 'Thermos con bevanda calda (tè, brodo)',
      motivo: `A ${quotaMax} m le temperature possono scendere rapidamente: una bevanda calda aiuta a mantenere il calore corporeo.`,
    });
  }

  consigliati.push({
    nome: 'Sali minerali o integratore elettrolitico',
    motivo: 'La sudorazione intensa disperde sodio e potassio. I sali prevengono i crampi.',
  });

  opzionali.push({
    nome: 'Frutta fresca (mele, banane)',
    motivo: 'Aggiungono zuccheri naturali e idratazione. Pesano di più, utili per escursioni più brevi.',
  });

  return [
    ...(base.length        ? [{ nome: 'Alimentazione', livello: 'base',        items: base }]        : []),
    ...(consigliati.length ? [{ nome: 'Alimentazione', livello: 'consigliato', items: consigliati }] : []),
    ...(opzionali.length   ? [{ nome: 'Alimentazione', livello: 'opzionale',   items: opzionali }]   : []),
  ];
}

// ─── Snapshot meteo ───────────────────────────────────────────────────────────

function buildMeteoSnapshot(forecastResult, meteoAgg) {
  if (!forecastResult) return null;
  return {
    locationId:              forecastResult.location?.externalId ?? null,
    locationName:            forecastResult.location?.name ?? forecastResult.town?.name ?? null,
    forecastFetchedAt:       forecastResult.meta?.fetchedAt ?? null,
    forecastValidFrom:       forecastResult.meta?.validFrom ?? null,
    forecastValidTo:         forecastResult.meta?.validTo ?? null,
    temperaturaMinPrevista:  meteoAgg?.temperaturaMin ?? null,
    temperaturaMedPrevista:  meteoAgg?.temperaturaMed ?? null,
    pioggiaProbMax:          meteoAgg?.pioggiaProbMax ?? null,
    neveFrescaPrevista:      meteoAgg?.neveFrescaTotale ?? null,
    ventoMaxPrevisto:        meteoAgg?.ventoMax ?? null,
    quotaZeroTermico:        meteoAgg?.quotaZeroTermico ?? null,
  };
}

// ─── Funzione principale ──────────────────────────────────────────────────────

/**
 * Genera la checklist completa per un'escursione.
 *
 * @param {Object}  sentiero        — documento Mongoose (o .lean()) del sentiero
 * @param {Object}  [forecastResult] — risultato di getLocationForecast() — opzionale
 * @param {Date}    [partenza]       — ora di partenza prevista (default: meetingDate a 08:00)
 * @returns {Object} oggetto compatibile con checklistSchema
 */
export function generateChecklist(sentiero, forecastResult = null, partenza = null) {
  const oraPartenza = partenza
    ? new Date(partenza)
    : new Date(); // fallback: ora corrente

  const oreStima = parseOre(sentiero.tempoAndata);

  // Aggrega metriche meteo sugli slot rilevanti per la durata del percorso
  const meteoAgg = forecastResult
    ? aggregaMeteo(forecastResult.slots3h ?? [], oraPartenza, oreStima)
    : null;

  // Build categorie
  const categorie = [
    ...buildAbbigliamento(sentiero, meteoAgg),
    ...buildAttrezzatura(sentiero, meteoAgg),
    ...buildSicurezza(sentiero, meteoAgg),
    ...buildAlimentazione(sentiero, meteoAgg),
  ];

  return {
    generatedAt:       new Date(),
    updatedAt:         new Date(),
    isFrozen:          false,
    frozenAt:          null,
    meteoSnapshot:     buildMeteoSnapshot(forecastResult, meteoAgg),
    categorie,
    acquaLitri:        calcolaAcqua(sentiero, meteoAgg),
    calorieFabbisogno: calcolaCalorie(sentiero),
  };
}

/**
 * Verifica se la checklist di una sessione è congelata.
 * Il freeze scatta alla mezzanotte del giorno prima della meetingDate.
 *
 * @param {Date|string} meetingDate — data della sessione
 * @returns {boolean}
 */
export function isChecklistFrozen(meetingDate) {
  if (!meetingDate) return false;
  const d = new Date(meetingDate);
  // Mezzanotte UTC del giorno prima
  const freezeAt = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
  return new Date() >= freezeAt;
}

/**
 * Restituisce il timestamp di freeze per una meetingDate.
 */
export function getFreezeAt(meetingDate) {
  if (!meetingDate) return null;
  const d = new Date(meetingDate);
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
}