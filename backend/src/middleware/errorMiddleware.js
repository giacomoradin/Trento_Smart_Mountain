
export const globalErrorHandler = (err, req, res, next) => {
  // Log dell'errore per il developer
  console.error(" [GLOBAL ERROR LOG]:", err.stack);

  const statusCode = err.statusCode || 500;

  res.status(statusCode).json({
    error: "Errore interno del server",
    message: err.message,
    // Lo stack compare solo se non siamo in produzione
    stack: process.env.NODE_ENV === 'development' ? err.stack : {}
  });
};

export const notFoundHandler = (req, res, next) => {
  res.status(404).json({ error: "Endpoint non trovato" });
};