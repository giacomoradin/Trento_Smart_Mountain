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

// Normalizza il BASE_URL rimuovendo eventuali slash finali per evitare
// doppi slash nelle URL (es. "https://app.com/" + "/auth/..." → "//auth/...")
const BASE_URL = (process.env.BASE_URL || "http://localhost:3000").replace(/\/+$/, "");

/**
 * Invia una singola email tramite Brevo REST API.
 * Usa fetch nativo (Node 18+, disponibile su Render) — nessuna dipendenza extra.
 */
/**
 * Estrae l'email "pulita" da una stringa che può essere:
 *  - "user@example.com"                       → "user@example.com"
 *  - "Nome Cognome <user@example.com>"        → "user@example.com"
 *  - "  user@example.com  "                   → "user@example.com"
 * Brevo richiede SOLO l'email nel campo `sender.email`.
 */
function extractEmail(raw) {
  if (!raw) return null;
  const match = String(raw).match(/<([^>]+)>/);  // formato "Nome <email>"
  return (match ? match[1] : String(raw)).trim();
}

/**
 * Estrae il testo plain da un body HTML in modo grezzo (rimuove tag).
 * Brevo usa la versione plain come fallback per i client email che non
 * rendono HTML, e migliora il punteggio anti-spam dei filtri Gmail/Outlook.
 */
function htmlToPlainText(html) {
  return html
    .replace(/<style[\s\S]*?<\/style>/gi, "")
    .replace(/<head[\s\S]*?<\/head>/gi, "")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(p|div|h[1-6])>/gi, "\n\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/\n\s*\n\s*\n/g, "\n\n")
    .trim();
}

async function sendEmail(toEmail, subject, htmlContent) {
  const apiKey = process.env.BREVO_API_KEY;
  const rawFrom = process.env.EMAIL_FROM_ADDRESS;
  const fromAddress = extractEmail(rawFrom);

  // Fail fast con messaggi diagnostici chiari nei log Render
  if (!apiKey) {
    throw new Error("[emailService] BREVO_API_KEY non configurata. Aggiungila su Render > Environment.");
  }
  if (!fromAddress) {
    throw new Error("[emailService] EMAIL_FROM_ADDRESS non configurata. Aggiungila su Render > Environment.");
  }

  // Log diagnostico — utile se l'utente ha incollato un formato sbagliato nella env var
  if (rawFrom !== fromAddress) {
    console.log(`[emailService] EMAIL_FROM_ADDRESS contiene metadata, estratta email: "${fromAddress}" (originale: "${rawFrom}")`);
  }
  console.log(`[emailService] Invio email → from: "${fromAddress}" | to: "${toEmail}"`);

  const payload = {
    sender: { name: "Trento Smart Mountain", email: fromAddress },
    // replyTo aiuta i filtri anti-spam: i messaggi con reply-to valido
    // hanno un punteggio migliore in Gmail/Outlook
    replyTo: { name: "Trento Smart Mountain", email: fromAddress },
    to: [{ email: toEmail }],
    subject,
    htmlContent,
    // textContent migliora drasticamente il punteggio anti-spam:
    // i client che non rendono HTML lo mostrano, e i filtri lo valutano positivamente
    textContent: htmlToPlainText(htmlContent),
    // Headers RFC 8058 per l'unsubscribe — riduce ulteriormente lo spam score
    headers: {
      "X-Mailer": "TSM-Backend/1.0",
    },
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
    "Conferma il tuo indirizzo email - Trento Smart Mountain",
    `<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Conferma indirizzo email</title>
</head>
<body style="margin:0;padding:24px;background:#f4f4f4;font-family:Arial,Helvetica,sans-serif;color:#333">
  <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:8px">
    <tr>
      <td style="padding:32px 32px 16px 32px;border-bottom:1px solid #eeeeee">
        <h1 style="margin:0;color:#2E5A27;font-size:22px">Trento Smart Mountain</h1>
        <p style="margin:4px 0 0 0;color:#888888;font-size:13px">Progetto universitario di Ingegneria del Software - Università di Trento</p>
      </td>
    </tr>
    <tr>
      <td style="padding:32px">
        <h2 style="margin:0 0 16px 0;color:#222222;font-size:18px">Ciao,</h2>
        <p style="margin:0 0 16px 0;line-height:1.6;color:#444444">
          Grazie per esserti registrato a Trento Smart Mountain. Per completare la registrazione
          e accedere alla piattaforma, conferma il tuo indirizzo email cliccando sul pulsante qui sotto.
        </p>
        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:24px 0">
          <tr>
            <td>
              <a href="${verificationUrl}"
                 style="display:inline-block;background:#2E5A27;color:#ffffff;padding:12px 24px;
                        text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px">
                Conferma indirizzo email
              </a>
            </td>
          </tr>
        </table>
        <p style="margin:0 0 8px 0;color:#666666;font-size:13px">
          Se il pulsante non funziona, copia e incolla questo link nel browser:
        </p>
        <p style="margin:0 0 24px 0;color:#2E5A27;font-size:13px;word-break:break-all">
          ${verificationUrl}
        </p>
        <p style="margin:0;color:#888888;font-size:13px;line-height:1.6">
          Il link è valido per 24 ore. Se non hai effettuato tu la registrazione, puoi
          ignorare questa email — il tuo indirizzo non verrà aggiunto al servizio.
        </p>
      </td>
    </tr>
    <tr>
      <td style="padding:16px 32px 24px 32px;border-top:1px solid #eeeeee;color:#999999;font-size:12px">
        Trento Smart Mountain - Università di Trento<br>
        Per assistenza: rispondi a questa email.
      </td>
    </tr>
  </table>
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
    "Reimpostazione password - Trento Smart Mountain",
    `<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Reimpostazione password</title>
</head>
<body style="margin:0;padding:24px;background:#f4f4f4;font-family:Arial,Helvetica,sans-serif;color:#333">
  <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:8px">
    <tr>
      <td style="padding:32px 32px 16px 32px;border-bottom:1px solid #eeeeee">
        <h1 style="margin:0;color:#2E5A27;font-size:22px">Trento Smart Mountain</h1>
        <p style="margin:4px 0 0 0;color:#888888;font-size:13px">Progetto universitario di Ingegneria del Software - Università di Trento</p>
      </td>
    </tr>
    <tr>
      <td style="padding:32px">
        <h2 style="margin:0 0 16px 0;color:#222222;font-size:18px">Reimpostazione password</h2>
        <p style="margin:0 0 16px 0;line-height:1.6;color:#444444">
          Abbiamo ricevuto una richiesta di reset password per il tuo account.
          Clicca sul pulsante qui sotto per impostare una nuova password.
        </p>
        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:24px 0">
          <tr>
            <td>
              <a href="${resetUrl}"
                 style="display:inline-block;background:#2E5A27;color:#ffffff;padding:12px 24px;
                        text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px">
                Reimposta password
              </a>
            </td>
          </tr>
        </table>
        <p style="margin:0 0 8px 0;color:#666666;font-size:13px">
          Se il pulsante non funziona, copia e incolla questo link nel browser:
        </p>
        <p style="margin:0 0 24px 0;color:#2E5A27;font-size:13px;word-break:break-all">
          ${resetUrl}
        </p>
        <p style="margin:0;color:#888888;font-size:13px;line-height:1.6">
          <strong>Importante:</strong> il link è valido per <strong>1 ora</strong>.
          Se non hai richiesto tu il reset della password, ignora questa email — la tua password
          rimarrà invariata e l'account resterà sicuro.
        </p>
      </td>
    </tr>
    <tr>
      <td style="padding:16px 32px 24px 32px;border-top:1px solid #eeeeee;color:#999999;font-size:12px">
        Trento Smart Mountain - Università di Trento<br>
        Per assistenza: rispondi a questa email.
      </td>
    </tr>
  </table>
</body>
</html>`,
  );
};
