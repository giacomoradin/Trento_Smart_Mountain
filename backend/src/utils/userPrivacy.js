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
  "personalInfo",
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
 * Restituisce una copia dell'oggetto utente con i campi self-only rimossi
 * se `viewerIsSelfOrAdmin` è false. Lavora indifferentemente su documenti
 * Mongoose (passa per `.toObject()`) o su risultati `.lean()`.
 */
export function stripPrivateFields(user, viewerIsSelfOrAdmin) {
  if (!user) return user;
  const plain = typeof user.toObject === "function" ? user.toObject() : { ...user };
  if (viewerIsSelfOrAdmin) return plain;
  for (const field of SELF_ONLY_FIELDS) {
    delete plain[field];
  }
  return plain;
}

/** Helper: il viewer è il proprietario del profilo o un admin? */
export function isSelfOrAdmin(viewerUser, targetUserId) {
  if (!viewerUser) return false;
  if (viewerUser.role === "admin") return true;
  return viewerUser.userId?.toString() === targetUserId?.toString();
}
