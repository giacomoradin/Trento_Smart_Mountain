import HikeSession from "../models/hikeSession.js";
import User from "../models/user.js";
import crypto from "crypto";

// Genera un codice invito alfanumerico univoco di 8 caratteri
function generateInviteCode() {
  return crypto.randomBytes(4).toString("hex").toUpperCase(); // es. "A3F7C12B"
}

// Crea una nuova sessione — il creator diventa automaticamente Capogruppo
export async function createSession(creatorId, routeDetails) {
  let inviteCode;
  let isUnique = false;

  // Rigenera il codice finché non è univoco nel DB
  while (!isUnique) {
    inviteCode = generateInviteCode();
    const existing = await HikeSession.findOne({ inviteCode });
    if (!existing) isUnique = true;
  }

  const session = new HikeSession({
    creatorId,
    routeDetails,
    inviteCode,
    // Il creatore è già nella lista partecipanti
    participants: [{ userId: creatorId }],
    status: "PLANNED",
  });

  await session.save();



// Diamo i ruoli di "groupLeader" a chi ha creato la sessione.
  // Usiamo $push per non rimuovere i ruoli che l'utente ha già in altre sessioni.
  await User.findByIdAndUpdate(creatorId, {
    $push: {
      sessionRoles: {
        groupId: session._id,
        role: "groupLeader",
        createdBy: creatorId, 
      },
    },
  });

  return session;
}

// Logica per l'ingresso in sessione tramite inviteCode
export async function joinSession(userId, inviteCode) {
  // Recuperiamo la sessione dal codice; se il codice è farlocco, usciamo subito.
  const session = await HikeSession.findOne({ inviteCode });
  if (!session) {
    throw new Error("SESSION_NOT_FOUND");
  }

  // Si può entrare solo se la camminata è ancora in fase di pianificazione.
  if (session.status !== "PLANNED") {
    throw new Error("SESSION_NOT_JOINABLE");
  }

  // Evitiamo duplicati: controlliamo se l'utente è già dei nostri.
  const alreadyIn = session.participants.some(
    (p) => p.userId.toString() === userId.toString()
  );
  if (alreadyIn) {
    throw new Error("ALREADY_IN_SESSION");
  }

  // Tutto ok, aggiungiamo l'utente ai partecipanti e salviamo su DB.
  session.participants.push({ userId });
  await session.save();

  // Infine, aggiorniamo il profilo dell'utente segnando che ora partecipa come "hiker".
  // Teniamo traccia del creatorId come riferimento per chi ha generato l'invito.
  await User.findByIdAndUpdate(userId, {
    $push: {
      sessionRoles: {
        groupId: session._id,
        role: "hiker",
        createdBy: session.creatorId,
      },
    },
  });

  return session;
}
// Recupera una sessione per ID
export async function getSessionById(sessionId) {
  return HikeSession.findById(sessionId)
    .populate("creatorId", "username email")
    .populate("participants.userId", "username email");
}

// Recupera tutte le sessioni di un utente (come creator o partecipante)
export async function getSessionsByUser(userId) {
  return HikeSession.find({
    $or: [{ creatorId: userId }, { "participants.userId": userId }],
  }).populate("creatorId", "username email");
}

// Aggiorna lo stato della sessione (es. PLANNED → ACTIVE)
export async function updateSessionStatus(sessionId, creatorId, newStatus) {
  const session = await HikeSession.findById(sessionId);

  if (!session) throw new Error("SESSION_NOT_FOUND");

  // Solo il Capogruppo (creator) può cambiare lo stato
  if (session.creatorId.toString() !== creatorId) {
    throw new Error("FORBIDDEN");
  }

  session.status = newStatus;
  if (newStatus === "ACTIVE") session.startTime = new Date();
  if (newStatus === "COMPLETED") session.endTime = new Date();

  await session.save();
  return session;
}

// Elimina una sessione (solo il creator può farlo)
export async function deleteSession(sessionId, creatorId) {
  const session = await HikeSession.findById(sessionId);

  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.creatorId.toString() !== creatorId) {
    throw new Error("FORBIDDEN");
  }

  await session.deleteOne();
}
