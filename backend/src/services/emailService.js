import nodemailer from "nodemailer";

let transporter = null;

function getTransporter() {
  if (!transporter) {
    transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: process.env.SMTP_PORT,
      secure: process.env.SMTP_SECURE === "true",
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS,
      },
      tls: { rejectUnauthorized: false },
    });
  }
  return transporter;
}

async function sendMailWithRetry(mailOptions, maxRetries = 3) {
  const transporter = getTransporter();
  
  let lastError;
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      await transporter.sendMail(mailOptions);
      console.log(`✅ Email inviata con successo al tentativo ${attempt}`);
      return;
    } catch (err) {
      lastError = err;
      console.warn(`❌ SMTP attempt ${attempt}/${maxRetries} failed: ${err.message}`);
      if (attempt < maxRetries) {
        const delay = Math.pow(2, attempt) * 1000;
        await new Promise(r => setTimeout(r, delay));
      }
    }
  }
  throw lastError;
}

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';

export const sendVerificationEmail = async (targetEmail, token) => {
  const verificationUrl = `${BASE_URL}/auth/verify/${token}`;
  
  await sendMailWithRetry({
    from: `"Trento Smart Mountain" <${process.env.EMAIL_FROM}`,
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
    from: `"Trento Smart Mountain" <${process.env.EMAIL_FROM}`,
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