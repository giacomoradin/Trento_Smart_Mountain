/**
 * Mappa centralizzata dei codici errore "business" del Service Layer agli
 * status HTTP corrispondenti + messaggio user-friendly.
 *
 * I service lanciano `throw new Error("CODE")` (es. `SESSION_NOT_FOUND`).
 * Prima del fix audit 2026-05, ogni route ripeteva blocchi
 * `if (err.message === "SESSION_NOT_FOUND") return res.status(404)...`.
 * Ora basta passare a `next(err)` — il global handler riconosce il code.
 *
 * Convenzione: i CODE sono `UPPER_SNAKE_CASE`. Per errori parametrici
 * (es. anti-cheat `FIELD_LOCKED:birthDate`), supportiamo anche il prefisso
 * con `:`.
 */
const BUSINESS_ERROR_MAP = {
  // 401 — credenziali
  WRONG_PASSWORD: { status: 401, message: "Password errata." },
  WRONG_OLD_PASSWORD: { status: 401, message: "Password attuale errata." },
  INVALID_CREDENTIALS: { status: 401, message: "Credenziali non valide." },
  // 403 — autorizzazione
  FORBIDDEN: { status: 403, message: "Non autorizzato." },
  NOT_IN_SESSION: { status: 403, message: "Non fai parte di questa sessione." },
  ONLY_CREATOR: { status: 403, message: "Solo il Capogruppo può eseguire questa operazione." },
  LIVE_TRACKING_SUSPENDED: {
    status: 403,
    message: "Live tracking sospeso per questo utente.",
  },
  USER_NOT_PARTICIPANT: {
    status: 400,
    message: "L'utente indicato non è un partecipante della sessione.",
  },
  NOT_INVITED: { status: 403, message: "Non invitato a questa sfida." },
  CREATOR_CANNOT_LEAVE: {
    status: 403,
    message:
      "Il Capogruppo non può abbandonare la sessione. Eliminala se vuoi rimuoverla.",
  },
  ONLY_CREATOR_CAN_UPDATE_SESSION: {
    status: 403,
    message: "Solo il Capogruppo può modificare la sessione.",
  },
  ONLY_CREATOR_CAN_DELETE_SESSION: {
    status: 403,
    message: "Solo il Capogruppo può eliminare la sessione.",
  },
  ONLY_CREATOR_CAN_COMPLETE_SESSION: {
    status: 403,
    message: "Non sei autorizzato a completare questa sessione.",
  },
  ONLY_CREATOR_CAN_CANCEL_CHALLENGE: {
    status: 403,
    message: "Solo il creator può cancellare.",
  },
  FORBIDDEN_NOT_OWNER: {
    status: 403,
    message: "Solo il proprietario può eseguire questa azione.",
  },
  FORBIDDEN_NOT_CREATOR: {
    status: 403,
    message: "Solo il creator della sessione può eseguire questa azione.",
  },
  NOT_SHARED: {
    status: 403,
    message: "Questa attività non è condivisa sul feed.",
  },
  FORBIDDEN_NOT_AUTHOR: {
    status: 403,
    message: "Puoi cancellare solo i tuoi commenti.",
  },
  BOARD_FORBIDDEN_ROLE: {
    status: 403,
    message: "Solo gli account rifugio possono pubblicare in bacheca.",
  },
  // 403/404/409 — gestione partecipanti sessione (approvazione/rimozione)
  FORBIDDEN_NOT_LEADER: {
    status: 403,
    message: "Solo il Capogruppo può eseguire questa operazione.",
  },
  FORBIDDEN_NOT_MEMBER: {
    status: 403,
    message: "Devi essere un partecipante accettato per gestire le richieste.",
  },
  PARTICIPANT_NOT_FOUND: {
    status: 404,
    message: "Partecipante non trovato nella sessione.",
  },
  PARTICIPANT_NOT_PENDING: {
    status: 409,
    message: "La richiesta non è più in attesa di approvazione.",
  },
  PARTICIPANT_BANNED: {
    status: 409,
    message: "Sei stato rimosso da questa sessione e non puoi più unirti.",
  },
  CANNOT_REMOVE_CREATOR: {
    status: 400,
    message: "Non puoi rimuovere il Capogruppo dalla sessione.",
  },
  // Stories
  STORY_NOT_FOUND: { status: 404, message: "Storia non trovata o scaduta." },
  STORY_MEDIA_TOO_LARGE: {
    status: 413,
    message: "Media troppo grande. Usa una foto più leggera o un video più breve.",
  },
  STORY_FORBIDDEN_REF: {
    status: 403,
    message: "Non puoi creare una storia per questa attività o sessione.",
  },
  // 400 — input semanticamente invalido
  SELF_FOLLOW: { status: 400, message: "Non puoi seguire te stesso." },
  // 404 — risorsa non trovata
  USER_NOT_FOUND: { status: 404, message: "Utente non trovato." },
  SESSION_NOT_FOUND: { status: 404, message: "Sessione non trovata." },
  INVITE_CODE_INVALID: { status: 404, message: "Codice invito non valido." },
  ACTIVITY_NOT_FOUND: { status: 404, message: "Attività non trovata." },
  QUIZ_NOT_FOUND: { status: 404, message: "Quiz non trovato." },
  CATEGORY_NOT_FOUND: { status: 404, message: "Categoria non trovata." },
  TOTEM_NOT_FOUND: { status: 404, message: "Totem non trovato." },
  CHALLENGE_NOT_FOUND: { status: 404, message: "Sfida non trovata." },
  COMMENT_NOT_FOUND: { status: 404, message: "Commento non trovato." },
  POST_NOT_FOUND: { status: 404, message: "Post non trovato." },
  // 400
  COMMENT_EMPTY: { status: 400, message: "Il commento non può essere vuoto." },
  POST_EMPTY: { status: 400, message: "Titolo e testo sono obbligatori." },
  // 409 — conflitti
  EMAIL_TAKEN: { status: 409, message: "Email già in uso." },
  USERNAME_TAKEN: { status: 409, message: "Username già in uso." },
  USER_ALREADY_IN_SESSION: {
    status: 409,
    message:
      "Hai una sessione attualmente in corso. Concludila prima di crearne / unirti a un'altra.",
  },
  ALREADY_IN_SESSION: { status: 409, message: "Sei già in questa sessione." },
  JOIN_REQUEST_PENDING: {
    status: 409,
    message: "Richiesta già inviata: attendi la conferma del Capogruppo.",
  },
  SESSION_NOT_JOINABLE: {
    status: 409,
    message: "La sessione non è più aperta.",
  },
  SESSION_NOT_ACTIVE: {
    status: 409,
    message: "La sessione non è attiva.",
  },
  ALREADY_RESPONDED: {
    status: 409,
    message: "Hai già risposto a questo invito.",
  },
  CANNOT_CANCEL_RUNNING: {
    status: 409,
    message: "Impossibile cancellare una sfida attiva o completata.",
  },
  TOTEM_TAG_DUPLICATE: { status: 409, message: "tagId già esistente." },
  ALREADY_FOLLOWING: {
    status: 409,
    message: "Stai già seguendo questo utente.",
  },
  NOT_FOLLOWING: {
    status: 404,
    message: "Non stai seguendo questo utente.",
  },
  // 500 — server-side ma con messaggio utile
  INVITE_CODE_GENERATION_FAILED: {
    status: 500,
    message: "Impossibile generare un codice invito univoco. Riprova.",
  },
};

/**
 * Risolve un Error a `{ status, message }` consultando la mappa.
 * Supporta sia codici esatti (`SESSION_NOT_FOUND`) sia parametrici
 * (`FIELD_LOCKED:birthDate`).
 */
function resolveBusinessError(err) {
  if (!err?.message) return null;
  // Codice parametrico tipo "FIELD_LOCKED:caiLevel" → prendi prima del ":"
  const code = err.message.split(":")[0];

  // FIELD_LOCKED è parametrico (es. "FIELD_LOCKED:birthDate") e va gestito
  // PRIMA del lookup nella map, perché il map non lo contiene direttamente.
  // L'errore deve avere `err.field` popolato (vedi LockedFieldError nel service).
  if (code === "FIELD_LOCKED" && err.field) {
    return {
      status: 409,
      message: `Il campo "${err.field}" non è modificabile dopo la prima impostazione.`,
      field: err.field,
    };
  }

  const mapped = BUSINESS_ERROR_MAP[code];
  if (!mapped) return null;
  return mapped;
}

export const globalErrorHandler = (err, req, res, next) => {
  // Prova prima a riconoscere un errore business → status mirato.
  const business = resolveBusinessError(err);
  if (business) {
    return res.status(business.status).json({
      message: business.message,
      ...(business.field ? { field: business.field } : {}),
      ...(err?.reason ? { reason: err.reason } : {}),
    });
  }

  // Log completo per il developer (sia in dev che prod) — solo errori "veri",
  // gli errori business non sporcano i log.
  console.error(" [GLOBAL ERROR LOG]:", err.stack);

  const statusCode = err.statusCode || 500;
  const isProd = process.env.NODE_ENV === "production";

  // In produzione mascheriamo i 5xx: il messaggio originale può rivelare
  // dettagli implementativi (path file, query Mongo, ecc.). Per i 4xx
  // teniamo il messaggio: di solito è un'indicazione utile al client.
  const exposeMessage = !isProd || statusCode < 500;

  const payload = {
    error: "Errore interno del server",
    message: exposeMessage
      ? err.message
      : "Errore imprevisto. Riprova più tardi.",
  };
  if (!isProd) payload.stack = err.stack;

  res.status(statusCode).json(payload);
};

export const notFoundHandler = (req, res, next) => {
  res.status(404).json({ error: "Endpoint non trovato" });
};
