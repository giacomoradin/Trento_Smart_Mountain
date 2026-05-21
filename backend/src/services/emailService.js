import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import nodemailer from "nodemailer";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

//for debugging purposes only
dotenv.config({ path: path.resolve(__dirname, '../../.env') });
dotenv.config(); // Carica anche le variabili d'ambiente standard

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: process.env.SMTP_PORT,
  secure: process.env.SMTP_SECURE === "true",
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
  tls: { rejectUnauthorized: false },
});

async function sendMailWithRetry(mailOptions, maxRetries = 3) {
  let lastError;
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      await transporter.sendMail(mailOptions);
      return;
    } catch (err) {
      lastError = err;
      console.warn(`SMTP attempt ${attempt}/${maxRetries} failed: ${err.message}`);
      if (attempt < maxRetries) {
        await new Promise(r => setTimeout(r, Math.pow(2, attempt) * 1000));
      }
    }
  }
  throw lastError;
}

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';
//for debugging purposes only
console.log('🔗 BASE_URL configurato:', BASE_URL);
export const sendVerificationEmail = async (targetEmail, token) => {
  const verificationUrl = `${BASE_URL}/auth/verify/${token}`;
  //for debugging purposes only
  console.log(`📧 Inizio invio email a: ${targetEmail}`);
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
  //for debugging purposes only
  console.log(`✅ Email verification completata per: ${targetEmail}`);
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
