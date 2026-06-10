import HikeSession from "../../src/models/hikeSession.js";
import { createTestHiker } from "../helpers/authHelper.js";
import Sentiero from "../../src/models/sentiero.js";
import crypto from "crypto";

/**
 * Helper functions per i test di sessioni e sentieri.
 *
 * Fornisce utility per:
 * - Creare sentieri SAT minimali nel database
 * - Creare sessioni di test in stato PLANNED
 * - Creare sessioni con sentieroCode già collegato
 */

// ─── Sentiero ─────────────────────────────────────────────────────────────────

/**
 * Crea un sentiero SAT minimale nel DB pronto per i test.
 *
 * @param {Object} data - Dati opzionali per personalizzare il sentiero
 * @returns {Promise<Object>} Sentiero creato (.lean())
 *
 * @example
 * const sentiero = await createTestSentiero({ difficolta: 'EE', codice: 'EE001' });
 */
export async function createTestSentiero(data = {}) {
  // Codice univoco per evitare conflitti tra test paralleli
  const codice = data.codice ?? `T${crypto.randomBytes(3).toString("hex").toUpperCase()}`;

  const sentiero = await Sentiero.create({
    codice,
    denominazione: data.denominazione ?? "Sentiero di test",
    difficolta:    data.difficolta   ?? "E",

    puntoInizio: {
      nome:       data.puntoInizio?.nome      ?? "Partenza test",
      quota:      data.puntoInizio?.quota     ?? 800,
      coordinate: {
        lat: data.puntoInizio?.lat ?? 46.07,
        lon: data.puntoInizio?.lon ?? 11.12,
      },
    },

    puntoFine: {
      nome:       data.puntoFine?.nome   ?? "Arrivo test",
      quota:      data.puntoFine?.quota  ?? 1400,
      coordinate: {
        lat: data.puntoFine?.lat ?? 46.12,
        lon: data.puntoFine?.lon ?? 11.18,
      },
    },

    quotaMinima:           data.quotaMinima           ?? 800,
    quotaMassima:          data.quotaMassima          ?? 1400,
    lunghezzaPlanimetrica: data.lunghezzaPlanimetrica ?? 5000,
    lunghezzaInclinata:    data.lunghezzaInclinata    ?? 5300,
    tempoAndata:           data.tempoAndata           ?? "02:30",
    tempoRitorno:          data.tempoRitorno          ?? "02:00",

    // Coordinata minima obbligatoria per il campo required
    percorsoCoordinate: data.percorsoCoordinate ?? "11.12,46.07 11.15,46.09 11.18,46.12",
  });

  return sentiero.toObject();
}

// ─── HikeSession ──────────────────────────────────────────────────────────────

/**
 * Genera un inviteCode univoco nel formato "TSM-XXXX".
 */
function generateInviteCode() {
  return "TSM-" + crypto.randomBytes(2).toString("hex").toUpperCase();
}

/**
 * Crea una sessione PLANNED nel DB pronta per i test.
 * Il creator viene creato automaticamente tramite createTestHiker
 * se non viene passato un userId esistente.
 *
 * @param {Object} options
 * @param {string}  [options.creatorId]    — userId esistente; se assente ne crea uno nuovo
 * @param {string}  [options.sentieroCode] — codice SAT da collegare alla sessione
 * @param {string}  [options.status]       — stato sessione (default: 'PLANNED')
 * @param {Object}  [options.overrides]    — campi extra da sovrascrivere
 * @returns {Promise<{ session: Object, creator: Object, token: string }>}
 *
 * @example
 * const { session, token } = await createTestSession({ sentieroCode: 'E001' });
 */
export async function createTestSession({
  creatorId,
  sentieroCode,
  status = "PLANNED",
  overrides = {},
} = {}) {
  // Crea creator se non fornito
  let creator, token;
  if (creatorId) {
    creator = { _id: creatorId };
    token = null; // chiamante gestisce il token
  } else {
    const hikerData = await createTestHiker({
      username: `creator_${crypto.randomBytes(2).toString("hex")}`,
      email:    `creator_${crypto.randomBytes(2).toString("hex")}@test.com`,
    });
    creator = hikerData.user;
    token   = hikerData.token;
  }

  const inviteCode = generateInviteCode();

  const session = await HikeSession.create({
    creatorId: creator._id,
    inviteCode,
    status,
    routeDetails: {
      name:            overrides.routeDetails?.name            ?? "Percorso di test",
      difficultyLevel: overrides.routeDetails?.difficultyLevel ?? "E",
    },
    participants: [{ userId: creator._id, role: "groupLeader" }],
    meetingDate:  overrides.meetingDate  ?? new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // +7gg
    meetingTime:  overrides.meetingTime  ?? "08:00",
    ...(sentieroCode && { sentieroCode }),
    ...overrides,
  });

  return {
    session: session.toObject(),
    creator,
    token,
  };
}

/**
 * Aggiunge un partecipante a una sessione esistente.
 * Utile per testare che GET /checklist sia accessibile anche ai non-creator.
 *
 * @param {string} sessionId  — _id della sessione
 * @param {string} userId     — _id dell'utente da aggiungere
 * @returns {Promise<Object>} Sessione aggiornata
 */
export async function addParticipantToSession(sessionId, userId) {
  return HikeSession.findByIdAndUpdate(
    sessionId,
    { $push: { participants: { userId, role: "hiker" } } },
    { new: true },
  ).lean();
}