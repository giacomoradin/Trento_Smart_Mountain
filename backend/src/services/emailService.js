import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import nodemailer from "nodemailer";
// Get the current file's directory
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// //Look for .env in /backend (two levels up from /backend/src/services/)
 dotenv.config({ path: path.resolve(__dirname, './.env') });



// //Debug SMTP configuration
console.log('SMTP Check:', {
  host: process.env.SMTP_HOST,
  port: process.env.SMTP_PORT,
  user: process.env.SMTP_USER,
  secure: process.env.SMTP_SECURE,
path: path.resolve(__dirname, '../../.env') // Shows where it's looking
});

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: process.env.SMTP_PORT,
  secure: process.env.SMTP_SECURE === "true",
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
  // --- INIEZIONE PATCH TLS ---
  tls: {
    // Bypassa il controllo della Certificate Authority (Solo per Dev/Test)
    rejectUnauthorized: false,
  },
  // ---------------------------
});


export const sendVerificationEmail = async (targetEmail, token) => {
  // Hardcoded per sviluppo locale. In produzione usare variabile d'ambiente
  const verificationUrl = `http://localhost:3000/auth/verify/${token}`;

  const mailOptions = {
    from: `"Trento Smart Mountain" <${process.env.SMTP_USER}>`,
    to: targetEmail,
    subject: "Inizializzazione Account - Verifica Richiesta",
    html: `
      <h3>Inizializzazione Nodo Operativa</h3>
      <p>Il tuo account è stato allocato nel cluster Trento Smart Mountain.</p>
      <p>Per completare l'handshake e sbloccare l'accesso, conferma il tuo vettore di comunicazione cliccando sul link sottostante:</p>
      <a href="${verificationUrl}" style="background-color: #0f8513; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
        Verifica Identità
      </a>
      <p><small>Seleziona questo link solo se hai esplicitamente richiesto l'accesso.</small></p>
    `,
  };

  await transporter.sendMail(mailOptions);
};
