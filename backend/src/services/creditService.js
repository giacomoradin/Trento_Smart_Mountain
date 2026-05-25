import User from "../models/user.js";
import CreditTransaction from "../models/creditTransaction.js";
import { computeLevel } from "./levelService.js";

export async function addCredits({ userId, amount, source, refId, refKind, note }) {
  // Create the transaction record first — if the $inc fails the record exists for reconciliation.
  await CreditTransaction.create({ userId, amount, source, refId, refKind, note });
  await User.findByIdAndUpdate(userId, { $inc: { socialCredits: amount } });
}

export async function getCreditsWithLevel(userId) {
  const user = await User.findById(userId).select("socialCredits");
  if (!user) throw new Error("USER_NOT_FOUND");
  const total = user.socialCredits ?? 0;
  return { total, level: computeLevel(total) };
}

export async function getCreditHistory(userId, { page = 1, limit = 20, source } = {}) {
  const filter = { userId };
  if (source) filter.source = source;
  const skip = (page - 1) * limit;
  const [items, total] = await Promise.all([
    CreditTransaction.find(filter).sort({ createdAt: -1 }).skip(skip).limit(limit).lean(),
    CreditTransaction.countDocuments(filter),
  ]);
  return { items, hasMore: skip + items.length < total };
}
