import { jest } from "@jest/globals";
import {
  sendVerificationEmail,
  sendPasswordResetEmail,
} from "../../src/services/emailService.js";

/**
 * Unit test dell'emailService (integrazione Brevo via fetch).
 * In NODE_ENV=test sendEmail è un no-op: per coprire il path reale forziamo
 * NODE_ENV diverso e mockiamo global.fetch, pilotando tutti i rami (successo,
 * chiave/mittente mancanti, risposta non-ok, errore di rete + retry).
 */

describe("emailService — no-op in test env", () => {
  test("does not call fetch when NODE_ENV=test", async () => {
    const fetchSpy = jest.spyOn(global, "fetch").mockResolvedValue({ ok: true });
    await sendVerificationEmail("to@test.com", "tok");
    expect(fetchSpy).not.toHaveBeenCalled();
    fetchSpy.mockRestore();
  });
});

describe("emailService — real Brevo path", () => {
  const ORIGINAL_ENV = { ...process.env };
  let fetchMock;

  beforeEach(() => {
    process.env.NODE_ENV = "production";
    process.env.BREVO_API_KEY = "xkeysib-test";
    process.env.EMAIL_FROM_ADDRESS = "Mittente <from@test.com>";
    fetchMock = jest.fn();
    global.fetch = fetchMock;
    jest.spyOn(console, "log").mockImplementation(() => {});
    jest.spyOn(console, "warn").mockImplementation(() => {});
    jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    process.env.NODE_ENV = ORIGINAL_ENV.NODE_ENV;
    process.env.BREVO_API_KEY = ORIGINAL_ENV.BREVO_API_KEY;
    process.env.EMAIL_FROM_ADDRESS = ORIGINAL_ENV.EMAIL_FROM_ADDRESS;
    jest.restoreAllMocks();
  });

  test("sends a verification email (sender given as 'Name <email>')", async () => {
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({ messageId: "m1" }) });

    await sendVerificationEmail("to@test.com", "verifytok");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, opts] = fetchMock.mock.calls[0];
    expect(url).toBe("https://api.brevo.com/v3/smtp/email");
    const payload = JSON.parse(opts.body);
    expect(payload.sender.email).toBe("from@test.com"); // estratto dal formato "Name <email>"
    expect(payload.to[0].email).toBe("to@test.com");
    expect(payload.textContent).toContain("Conferma"); // htmlToPlainText ha prodotto il plain
  });

  test("sends a password-reset email (sender as a plain address)", async () => {
    process.env.EMAIL_FROM_ADDRESS = "plain@test.com";
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({ messageId: "m2" }) });

    await sendPasswordResetEmail("to@test.com", "resettok");

    const payload = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(payload.sender.email).toBe("plain@test.com");
    expect(payload.subject).toContain("Reimpostazione password");
  });

  test("throws when BREVO_API_KEY is not configured", async () => {
    delete process.env.BREVO_API_KEY;
    await expect(sendVerificationEmail("to@test.com", "t")).rejects.toThrow(
      /BREVO_API_KEY/,
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  test("throws when EMAIL_FROM_ADDRESS is not configured", async () => {
    delete process.env.EMAIL_FROM_ADDRESS;
    await expect(sendVerificationEmail("to@test.com", "t")).rejects.toThrow(
      /EMAIL_FROM_ADDRESS/,
    );
  });

  test("throws when Brevo responds non-ok", async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 400,
      text: async () => "Bad Request",
    });
    await expect(sendVerificationEmail("to@test.com", "t")).rejects.toThrow(
      /Brevo API error 400/,
    );
  });

  test("retries once on a network error and succeeds", async () => {
    fetchMock
      .mockRejectedValueOnce(new Error("ENETUNREACH"))
      .mockResolvedValueOnce({ ok: true, json: async () => ({ messageId: "m3" }) });

    await expect(sendVerificationEmail("to@test.com", "t")).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  test("throws when both the initial request and the retry fail", async () => {
    fetchMock.mockRejectedValue(new Error("ENETUNREACH"));
    await expect(sendVerificationEmail("to@test.com", "t")).rejects.toThrow(
      /ENETUNREACH/,
    );
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
