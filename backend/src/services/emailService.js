/**
 * emailService.js
 *
 * Invia email transazionali tramite Brevo (ex Sendinblue) HTTP API.
 *
 * Vantaggi rispetto a SMTP Gmail su Render:
 *  - Usa fetch HTTPS (porta 443) → nessun blocco firewall PaaS
 *  - Nessun problema IPv6 (era la causa del ENETUNREACH)
 *  - Free tier: 300 email/giorno, 9.000/mese — più che sufficiente
 *  - NON richiede un dominio personalizzato: basta verificare l'email mittente
 *
 * Setup Render (aggiungi queste 2 env var nel dashboard):
 *   BREVO_API_KEY      → API key da https://app.brevo.com/settings/keys/api
 *   EMAIL_FROM_ADDRESS → indirizzo verificato (es. SmartMountain.FMG@gmail.com)
 *
 * Setup locale (aggiungi in backend/.env):
 *   BREVO_API_KEY=xkeysib-...
 *   EMAIL_FROM_ADDRESS=SmartMountain.FMG@gmail.com
 */

const BASE_URL = process.env.BASE_URL || "http://localhost:3000";

/**
 * Invia una singola email tramite Brevo REST API.
 * Usa fetch nativo (Node 18+, disponibile su Render) — nessuna dipendenza extra.
 */
async function sendEmail(toEmail, subject, htmlContent) {
  const apiKey = process.env.BREVO_API_KEY;
  const fromAddress = process.env.EMAIL_FROM_ADDRESS;

  // Fail fast con messaggi diagnostici chiari nei log Render
  if (!apiKey) {
    throw new Error("[emailService] BREVO_API_KEY non configurata. Aggiungila su Render > Environment.");
  }
  if (!fromAddress) {
    throw new Error("[emailService] EMAIL_FROM_ADDRESS non configurata. Aggiungila su Render > Environment.");
  }

  // Log diagnostico — visibile nei log Render per verificare i valori
  console.log(`[emailService] Invio email → from: "${fromAddress}" | to: "${toEmail}"`);

  const payload = {
    sender: { name: "Trento Smart Mountain", email: fromAddress },
    to: [{ email: toEmail }],
    subject,
    htmlContent,
  };

  let response;
  try {
    response = await fetch("https://api.brevo.com/v3/smtp/email", {
      method: "POST",
      headers: {
        "accept": "application/json",
        "api-key": apiKey,
        "content-type": "application/json",
      },
      body: JSON.stringify(payload),
    });
  } catch (networkErr) {
    console.error("[emailService] Errore di rete verso Brevo API:", networkErr.message);
    throw networkErr;
  }

  if (!response.ok) {
    const body = await response.text();
    console.error(`[emailService] Brevo API ${response.status}: ${body}`);
    throw new Error(`Brevo API error ${response.status}: ${body}`);
  }

  const data = await response.json();
  console.log(`[emailService] Email inviata ✓ → to: ${toEmail} | messageId: ${data.messageId}`);
}

/**
 * Invia l'email di verifica account.
 * Il link porta a GET /auth/verify/:token → redirect a tsm://auth/success?jwt=...
 */
export const sendVerificationEmail = async (targetEmail, token) => {
  const verificationUrl = `${BASE_URL}/auth/verify/${token}`;

  await sendEmail(
    targetEmail,
    "Inizializzazione Account - Verifica Richiesta",
    `<!DOCTYPE html>
<html lang="it">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:24px;background:#121212;font-family:sans-serif">
  <div style="max-width:480px;margin:0 auto;background:#1e1e1e;padding:32px;border-radius:12px;border-left:4px solid #2E5A27">
    <h2 style="color:#4FC3F7;margin-top:0">Trento Smart Mountain 🏔️</h2>
    <h3 style="color:#ffffff;margin-top:0">Verifica il tuo account</h3>
    <p style="color:#aaaaaa;line-height:1.6">
      Il tuo account è stato creato. Clicca il pulsante qui sotto per verificare la tua
      identità e sbloccare l'accesso alla piattaforma.
    </p>
    <a href="${verificationUrl}"
       style="display:inline-block;background:#2E5A27;color:#ffffff;padding:14px 28px;
              text-decoration:none;border-radius:8px;font-weight:bold;font-size:15px;margin:16px 0">
      ✓ Verifica Account
    </a>
    <p style="color:#555555;font-size:12px;margin-top:28px;border-top:1px solid #333;padding-top:16px">
      Se non hai creato un account su Trento Smart Mountain, ignora questa email.<br>
      Il link è valido per 24 ore.
    </p>
  </div>
</body>
</html>`,
  );
};

/**
 * Invia l'email di reset password con link one-time (scadenza 1 ora).
 */
export const sendPasswordResetEmail = async (targetEmail, token) => {
  const resetUrl = `${BASE_URL}/auth/reset-password/${token}`;

  await sendEmail(
    targetEmail,
    "Reset Password - Trento Smart Mountain",
    `<!DOCTYPE html>
<html lang="it">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:24px;background:#121212;font-family:sans-serif">
  <div style="max-width:480px;margin:0 auto;background:#1e1e1e;padding:32px;border-radius:12px;border-left:4px solid #2E5A27">
    <h2 style="color:#4FC3F7;margin-top:0">Trento Smart Mountain 🏔️</h2>
    <h3 style="color:#ffffff;margin-top:0">Reset Password</h3>
    <p style="color:#aaaaaa;line-height:1.6">
      Hai richiesto il reset della password. Clicca il pulsante per impostare una nuova password.
    </p>
    <a href="${resetUrl}"
       style="display:inline-block;background:#2E5A27;color:#ffffff;padding:14px 28px;
              text-decoration:none;border-radius:8px;font-weight:bold;font-size:15px;margin:16px 0">
      🔑 Reimposta Password
    </a>
    <p style="color:#555555;font-size:12px;margin-top:28px;border-top:1px solid #333;padding-top:16px">
      ⚠️ Il link scade tra <strong>1 ora</strong>.<br>
      Se non hai richiesto questo reset, ignora l'email — la tua password rimane invariata.
    </p>
  </div>
</body>
</html>`,
  );
};
