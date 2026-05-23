
export const globalErrorHandler = (err, req, res, next) => {
  // Log completo per il developer (sia in dev che prod).
  console.error(" [GLOBAL ERROR LOG]:", err.stack);

  const statusCode = err.statusCode || 500;
  const isProd = process.env.NODE_ENV === "production";

  // In produzione mascheriamo i 5xx: il messaggio originale può rivelare
  // dettagli implementativi (path file, query Mongo, ecc.). Per i 4xx
  // teniamo il messaggio: di solito è un'indicazione utile al client.
  const exposeMessage = !isProd || statusCode < 500;

  const payload = {
    error: "Errore interno del server",
    message: exposeMessage ? err.message : "Errore imprevisto. Riprova più tardi.",
  };
  if (!isProd) payload.stack = err.stack;

  res.status(statusCode).json(payload);
};

export const notFoundHandler = (req, res, next) => {
  res.status(404).json({ error: "Endpoint non trovato" });
};