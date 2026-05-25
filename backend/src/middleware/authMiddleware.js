import jwt from "jsonwebtoken";

export const authenticate = (req, res, next) => {
  const token = req.headers.authorization?.split(" ")[1]; // "Bearer <token>"

  if (!token) {
    return res.status(401).json({ message: "No token provided." });
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    // Convenzione: il JWT contiene { userId, role }. Esponiamo anche `_id` come
    // alias per evitare regressioni in handler che usano l'abitudine Mongoose
    // (req.user._id). Il valore è lo stesso: nessuna ambiguità.
    req.user = { ...decoded, _id: decoded.userId };
    next();
  } catch (error) {
    res.status(401).json({ message: "Invalid or expired token." });
  }
};
