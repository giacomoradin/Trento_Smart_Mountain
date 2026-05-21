// dotenv viene già caricato globalmente da server.js (`import "dotenv/config"`).
// Su Render le env var sono iniettate direttamente nel process: nessun file .env.
// Su locale, il file backend/.env viene caricato all'avvio.
import nodemailer from "nodemailer";

// Trim difensivo: protegge da spazi/newline incollati per errore nel dashboard Render
const trim = (v) => (typeof v === "string" ? v.trim() : v);

// Render Free tier non ha routing IPv6 in egress: smtp.gmail.com risolve sia
// AAAA (IPv6) che A (IPv4) e nodemailer di default preferisce IPv6 → ENETUNREACH.
// Forziamo IPv4 con `family: 4` per evitare il problema.
const transporter = nodemailer.createTransport({
  host: trim(process.env.SMTP_HOST),
  port: Number(trim(process.env.SMTP_PORT)) || 587,
  secure: trim(process.env.SMTP_SECURE) === "true",
  auth: {
    user: trim(process.env.SMTP_USER),
    pass: trim(process.env.SMTP_PASS),
  },
  tls: { rejectUnauthorized: false },
  family: 4,             // forza IPv4 (fix ENETUNREACH su Render)
  connectionTimeout: 20_000,
  greetingTimeout: 20_000,
  socketTimeout: 30_000,
});

// Verifica all'avvio che il transporter sia configurato correttamente.
// Esegui in background — non blocca il server, ma logga eventuali problemi di auth.
transporter.verify((err) => {
  if (err) {
    console.error("[emailService] SMTP verify FAILED:", err.message);
    console.error("  → controlla SMTP_USER/SMTP_PASS su Render (no newline/spazi)");
    console.error("  → SMTP_USER attuale:", JSON.stringify(process.env.SMTP_USER));
  } else {
    console.log("[emailService] SMTP transporter OK, ready to send emails");
  }
});

async function sendMailWithRetry(mailOptions, maxRetries = 3) {
  let lastError;
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const info = await transporter.sendMail(mailOptions);
      console.log(`[emailService] Email sent to ${mailOptions.to} (messageId: ${info.messageId})`);
      return;
    } catch (err) {
      lastError = err;
      console.warn(`[emailService] SMTP attempt ${attempt}/${maxRetries} failed: ${err.message}`);
      console.warn(`  → to: ${mailOptions.to}, subject: ${mailOptions.subject}`);
      if (attempt < maxRetries) {
        await new Promise(r => setTimeout(r, Math.pow(2, attempt) * 1000));
      }
    }
  }
  console.error(`[emailService] Email definitively failed after ${maxRetries} attempts:`, lastError.message);
  throw lastError;
}

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';

export const sendVerificationEmail = async (targetEmail, token) => {
  const verificationUrl = `${BASE_URL}/auth/verify/${token}`;
  await sendMailWithRetry({
    from: `"Trento Smart Mountain" <${process.env.SMTP_USER}>`,
    to: targetEmail,
    subject: "Inizializzazione Account - Verifica Richiesta",
    html: `
      <h3>Inizializzazione Nodo Operativa</h3>
      <p>Il tuo account è stato allocato nel cluster Trento Smart Mountain.</p>
      <p>Per completare l'handshake e sbloccare l'accesso, clicca sul link:</p>
      <a href="${verificationUrl}" style="background:#2E5A27;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;">
        Verifica Identità
      </a>
      <p><small>Ignora questa email se non hai richiesto l'accesso.</small></p>
    `,
  });
};

export const sendPasswordResetEmail = async (targetEmail, token) => {
  const resetUrl = `${BASE_URL}/auth/reset-password/${token}`;
  await sendMailWithRetry({
    from: `"Trento Smart Mountain" <${process.env.SMTP_USER}>`,
    to: targetEmail,
    subject: "Reset Password - Trento Smart Mountain",
    html: `
      <h3>Reset Password</h3>
      <p>Hai richiesto il reset della password per il tuo account TSM.</p>
      <p>Il link scade tra 1 ora.</p>
      <a href="${resetUrl}" style="background:#2E5A27;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;">
        Reimposta Password
      </a>
      <p><small>Se non hai richiesto questo reset, ignora questa email.</small></p>
    `,
  });
};
