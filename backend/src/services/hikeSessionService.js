import HikeSession from "../models/hikeSession.js";
import User from "../models/user.js";
import crypto from "crypto";

// Genera un codice invito alfanumerico univoco di 8 caratteri
function generateInviteCode() {
  return crypto.randomBytes(4).toString("hex").toUpperCase(); // es. "A3F7C12B"
}

// Controlla se l'utente è già in una sessione attiva o pianificata
async function checkUserAlreadyInActiveSession(userId) {
  const conflict = await HikeSession.findOne({
    "participants.userId": userId,
    status: { $in: ["PLANNED", "ACTIVE"] },
  });

  if (conflict) {
    throw new Error("USER_ALREADY_IN_SESSION");
  }
}

// Crea una nuova sessione — il creator diventa automaticamente Capogruppo
export async function createSession(creatorId, routeDetails) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Crea una nuova sessione di escursione. Il creatore diventa automaticamente Group Leader.'
  */
  await checkUserAlreadyInActiveSession(creatorId);
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
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Permette a un utente di unirsi a una sessione esistente tramite codice invito.'
  */
  await checkUserAlreadyInActiveSession(userId);
  const session = await HikeSession.findOne({ inviteCode });
  if (!session) {
    throw new Error("SESSION_NOT_FOUND");
  }

  if (session.status !== "PLANNED") {
    throw new Error("SESSION_NOT_JOINABLE");
  }

  const alreadyIn = session.participants.some(
    (p) => p.userId.toString() === userId.toString()
  );
  if (alreadyIn) {
    throw new Error("ALREADY_IN_SESSION");
  }

  session.participants.push({ userId });
  await session.save();

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
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Recupera i dettagli completi di una sessione, inclusi i dati di partecipanti e creatore.'
  */
  return HikeSession.findById(sessionId)
    .populate("creatorId", "username email")
    .populate("participants.userId", "username email");
}

// Recupera tutte le sessioni di un utente (come creator o partecipante)
export async function getSessionsByUser(userId) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Ottiene la lista di tutte le sessioni a cui l'utente partecipa o che ha creato.'
  */
  return HikeSession.find({
    $or: [{ creatorId: userId }, { "participants.userId": userId }],
  }).populate("creatorId", "username email");
}

// Aggiorna lo stato della sessione (es. PLANNED → ACTIVE)
export async function updateSessionStatus(sessionId, creatorId, newStatus) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Aggiorna lo stato della sessione. Solo il creatore (Group Leader) può eseguire questa operazione.'
  */
  const session = await HikeSession.findById(sessionId);

  if (!session) throw new Error("SESSION_NOT_FOUND");

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
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Elimina definitivamente una sessione. Operazione riservata al creatore.'
  */
  const session = await HikeSession.findById(sessionId);

  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.creatorId.toString() !== creatorId) {
    throw new Error("FORBIDDEN");
  }

  await session.deleteOne();
}