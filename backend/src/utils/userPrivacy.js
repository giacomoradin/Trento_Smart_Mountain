/**
 * Privacy gate per i dati utente esposti via GET /users/:id e /hikers/:id.
 *
 * Quando un utente A richiede il profilo di un utente B (con A ≠ B), il body
 * di risposta deve nascondere i campi sensibili (peso, sesso, data nascita,
 * scheda medica, FCM token, preferenze privacy). Questi campi sono solo per
 * self-view o per amministrazione.
 *
 * Per now: l'admin viene trattato come "self" (vede tutto). In futuro, quando
 * arriva la sezione Social, qui aggiungeremo la logica friends/private.
 */

const SELF_ONLY_FIELDS = [
  // NB: `personalInfo` è gestito a parte (vedi `PERSONAL_INFO_PUBLIC_FIELDS`):
  //     l'avatarUrl resta pubblico per la UI (chip partecipanti, autori post,
  //     feed sociale), tutti gli altri campi (peso, sesso, data nascita) restano
  //     privati e vengono cancellati per i viewer "other".
  "experience",
  "preferences",
  "profileCompletedAt",
  // Token di verifica/reset non devono MAI uscire dal server — già escluso
  // dalla query select(-passwordHash), ma triple defense.
  "verificationToken",
  "passwordResetToken",
  "passwordResetExpires",
  "weeklyGoals", // gli obiettivi sono privati per design
];

/**
 * Campi di `personalInfo` visibili anche per viewer "other" (non self/admin).
 *
 * `avatarUrl` è esplicitamente pubblico: la UI ne ha bisogno per mostrare la
 * foto profilo degli altri utenti nelle liste di partecipanti, nei card delle
 * sessioni, e nel feed sociale. Tutti gli altri campi di `personalInfo`
 * (sex, birthDate, heightCm, weightKg) restano privati.
 */
const PERSONAL_INFO_PUBLIC_FIELDS = ["avatarUrl"];

/**
 * Restituisce una copia dell'oggetto utente con i campi self-only rimossi
 * se `viewerIsSelfOrAdmin` è false. Lavora indifferentemente su documenti
 * Mongoose (passa per `.toObject()`) o su risultati `.lean()`.
 *
 * Per `personalInfo`: se viewer è "other", mantiene solo i campi pubblici
 * (avatarUrl) e cancella il resto. Se viewer è self/admin, lo passa intero.
 */
export function stripPrivateFields(user, viewerIsSelfOrAdmin) {
  if (!user) return user;
  const plain = typeof user.toObject === "function" ? user.toObject() : { ...user };
  if (viewerIsSelfOrAdmin) return plain;
  for (const field of SELF_ONLY_FIELDS) {
    delete plain[field];
  }
  // Filtra personalInfo mantenendo solo i campi pubblici (avatarUrl).
  // Se non c'è nessun campo pubblico valorizzato, rimuoviamo del tutto la chiave
  // per non sporcare la response con un oggetto vuoto.
  if (plain.personalInfo && typeof plain.personalInfo === "object") {
    const publicSlice = {};
    for (const key of PERSONAL_INFO_PUBLIC_FIELDS) {
      if (plain.personalInfo[key] !== undefined && plain.personalInfo[key] !== null) {
        publicSlice[key] = plain.personalInfo[key];
      }
    }
    if (Object.keys(publicSlice).length > 0) {
      plain.personalInfo = publicSlice;
    } else {
      delete plain.personalInfo;
    }
  }
  return plain;
}

/** Helper: il viewer è il proprietario del profilo o un admin? */
export function isSelfOrAdmin(viewerUser, targetUserId) {
  if (!viewerUser) return false;
  if (viewerUser.role === "admin") return true;
  return viewerUser.userId?.toString() === targetUserId?.toString();
}
