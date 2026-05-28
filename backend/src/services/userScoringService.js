/**
 * Coefficiente baseline utente (μ_user_baseline) per il calcolo crediti.
 *
 * Premia la "sfida personale": un utente sedentario di livello CAI T che
 * completa una hike EE prende un boost maggiore rispetto a un atleta EEA
 * che fa lo stesso percorso. Disincentiva il farming di crediti facili
 * per profili esperti e incoraggia l'utente neofita a continuare.
 *
 * Range bounded: il moltiplicatore finale è clippato in [0.85, 1.30] per
 * evitare derive estreme (es. utente con dati incoerenti che riceve 3×).
 *
 * Tabella: μ = base_fitness_factor × cai_alignment_factor
 *   - base_fitness_factor: chi è più sedentario riceve più boost per ogni
 *     hike completata (premia il primo passo).
 *   - cai_alignment_factor: percorsi più difficili rispetto al livello
 *     dichiarato dell'utente valgono di più (challenge bonus).
 *
 * Profili mancanti → 1.0 (neutro). Non penalizziamo chi non ha completato
 * l'onboarding — il banner in app è già lì per spingerlo a farlo.
 */

// Modello "inversamente proporzionale all'esperienza": più sei esperto, più
// basso il moltiplicatore. Premiamo l'utente neofita che fa lo sforzo di iniziare.
//
// Entrambi i factor sono inversamente proporzionali al livello dichiarato.
// Il risultato finale μ = fitnessFactor × caiLevelFactor (poi clipping difensivo).

const FITNESS_FACTOR = {
  sedentary: 1.20, // chi parte da fermo: ogni hike è una grande conquista
  active:    1.10,
  sport:     1.00,
  athlete:   0.90, // hike "facili" rispetto al loro standard fisico
};

const CAI_LEVEL_FACTOR = {
  T:   1.20, // turistico — neofita dei sentieri
  E:   1.10, // escursionistico — esperienza intermedia
  EE:  1.00, // esperti
  EEA: 0.90, // esperti con attrezzatura — top livello tecnico
};

/**
 * Calcola il moltiplicatore baseline. `user` è il documento Hiker (anche lean).
 *
 * `sessionDifficulty` resta nel signature per backward compatibility con i call
 * site ma non è più usato — la formula è puramente "inversamente proporzionale
 * all'esperienza" come preferito dal product. Lascia il parametro nel caso si
 * volesse re-introdurre un bonus di sfida personale in futuro.
 *
 * Range teorico: 0.81 (athlete EEA) → 1.44 (sedentary T)
 * Range effettivo dopo clipping: [0.85, 1.30]
 */
export function getBaselineMultiplier(user, _sessionDifficulty = null) {
  const fitness = user?.experience?.baselineFitness;
  const caiLevel = user?.experience?.caiLevel;
  const fitnessFactor = (fitness && FITNESS_FACTOR[fitness]) || 1.0;
  const caiLevelFactor = (caiLevel && CAI_LEVEL_FACTOR[caiLevel]) || 1.0;
  const mu = fitnessFactor * caiLevelFactor;
  // Clipping difensivo: combinazioni estreme (sedentary T = 1.44) vengono
  // limitate a 1.30 per evitare bonus troppo forti.
  return Math.min(1.30, Math.max(0.85, mu));
}

/**
 * Applica il moltiplicatore baseline a un punteggio "base" client-calcolato.
 * Restituisce un intero (i crediti sono sempre interi).
 *
 * NOTA: il client manda già `finalPoints` con la μ_efficiency CAI applicata.
 * Questa funzione aggiunge un secondo layer (μ_user_baseline) → il totale
 * effettivo è basePoints × μ_efficiency × μ_user_baseline.
 */
export function applyBaselineMultiplier(basePoints, user, sessionDifficulty = null) {
  if (!basePoints || basePoints <= 0) return 0;
  const mu = getBaselineMultiplier(user, sessionDifficulty);
  return Math.round(basePoints * mu);
}
