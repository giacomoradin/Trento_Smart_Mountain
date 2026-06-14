import { jest } from "@jest/globals";
import Refuge from "../../src/models/refuge.js";

/**
 * Unit test del refugeService (handler Express testati direttamente con req/res
 * finti + DB in-memory). Copre tutti i rami: validazioni, rollback su email
 * fallita, duplicati (11000), CastError e i catch generici (500) — questi ultimi
 * forzati con spyOn sul model. L'emailService è mockato per controllare il ramo
 * di fallimento invio (in NODE_ENV=test l'invio reale è un no-op).
 */

jest.unstable_mockModule("../../src/services/emailService.js", () => ({
  sendVerificationEmail: jest.fn(),
  sendPasswordResetEmail: jest.fn(),
}));

const { createRefuge, getRefugeById, listRefuges, updateRefuge } = await import(
  "../../src/services/refugeService.js"
);
const { sendVerificationEmail } = await import(
  "../../src/services/emailService.js"
);

function mockRes() {
  return {
    statusCode: undefined,
    body: undefined,
    status(code) {
      this.statusCode = code;
      return this;
    },
    json(payload) {
      this.body = payload;
      return this;
    },
  };
}

const VALID_BODY = {
  username: "rifugio_test",
  email: "rifugio@test.com",
  password: "Password123!",
  rifugioName: "Rifugio Brentei",
  caiCode: "CAI-001",
  quota: 2182,
  posti: 60,
};

afterEach(() => {
  jest.restoreAllMocks();
});

describe("refugeService.createRefuge", () => {
  test("creates a refuge (201) and strips secret fields", async () => {
    const req = { body: { ...VALID_BODY } };
    const res = mockRes();

    await createRefuge(req, res);

    expect(res.statusCode).toBe(201);
    expect(res.body.user.rifugioName).toBe("Rifugio Brentei");
    expect(res.body.user.passwordHash).toBeUndefined();
    expect(res.body.user.verificationToken).toBeUndefined();
    expect(sendVerificationEmail).toHaveBeenCalledWith(
      "rifugio@test.com",
      expect.any(String),
    );
    expect(await Refuge.countDocuments()).toBe(1);
  });

  test("400 when username missing", async () => {
    const res = mockRes();
    await createRefuge({ body: { ...VALID_BODY, username: undefined } }, res);
    expect(res.statusCode).toBe(400);
  });

  test("400 when email missing", async () => {
    const res = mockRes();
    await createRefuge({ body: { ...VALID_BODY, email: undefined } }, res);
    expect(res.statusCode).toBe(400);
  });

  test("400 when password missing", async () => {
    const res = mockRes();
    await createRefuge({ body: { ...VALID_BODY, password: undefined } }, res);
    expect(res.statusCode).toBe(400);
  });

  test("400 when password shorter than 8 chars", async () => {
    const res = mockRes();
    await createRefuge({ body: { ...VALID_BODY, password: "short" } }, res);
    expect(res.statusCode).toBe(400);
  });

  test("400 when rifugioName missing", async () => {
    const res = mockRes();
    await createRefuge({ body: { ...VALID_BODY, rifugioName: undefined } }, res);
    expect(res.statusCode).toBe(400);
  });

  test("409 on duplicate email/username", async () => {
    await createRefuge({ body: { ...VALID_BODY } }, mockRes());
    const res = mockRes();
    await createRefuge({ body: { ...VALID_BODY } }, res);
    expect(res.statusCode).toBe(409);
  });

  test("rolls back and returns 500 if verification email fails", async () => {
    sendVerificationEmail.mockRejectedValueOnce(new Error("smtp down"));
    const res = mockRes();

    await createRefuge({ body: { ...VALID_BODY } }, res);

    expect(res.statusCode).toBe(500);
    // Rollback: l'utente NON deve restare nel DB.
    expect(await Refuge.countDocuments()).toBe(0);
  });

  test("500 on unexpected (non-duplicate) save error", async () => {
    jest
      .spyOn(Refuge.prototype, "save")
      .mockRejectedValueOnce(new Error("db exploded"));
    const res = mockRes();

    await createRefuge({ body: { ...VALID_BODY } }, res);

    expect(res.statusCode).toBe(500);
    expect(res.body.message).toBe("db exploded");
  });
});

describe("refugeService.getRefugeById", () => {
  async function seedRefuge() {
    await createRefuge({ body: { ...VALID_BODY } }, mockRes());
    return Refuge.findOne({ email: VALID_BODY.email });
  }

  test("200 returns the refuge without secrets", async () => {
    const refuge = await seedRefuge();
    const res = mockRes();
    await getRefugeById({ params: { id: refuge._id.toString() } }, res);
    expect(res.statusCode).toBe(200);
    expect(res.body.passwordHash).toBeUndefined();
  });

  test("404 when refuge not found", async () => {
    const res = mockRes();
    await getRefugeById({ params: { id: "507f1f77bcf86cd799439011" } }, res);
    expect(res.statusCode).toBe(404);
  });

  test("400 on invalid (cast) id", async () => {
    const res = mockRes();
    await getRefugeById({ params: { id: "not-an-objectid" } }, res);
    expect(res.statusCode).toBe(400);
  });

  test("500 on unexpected error", async () => {
    jest.spyOn(Refuge, "findById").mockReturnValueOnce({
      select: () => Promise.reject(new Error("boom")),
    });
    const res = mockRes();
    await getRefugeById({ params: { id: "507f1f77bcf86cd799439011" } }, res);
    expect(res.statusCode).toBe(500);
  });
});

describe("refugeService.listRefuges", () => {
  test("200 returns public refuge list", async () => {
    await createRefuge({ body: { ...VALID_BODY } }, mockRes());
    const res = mockRes();
    await listRefuges({}, res);
    expect(res.statusCode).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body).toHaveLength(1);
  });

  test("500 on unexpected error", async () => {
    jest.spyOn(Refuge, "find").mockReturnValueOnce({
      select: () => ({ sort: () => Promise.reject(new Error("boom")) }),
    });
    const res = mockRes();
    await listRefuges({}, res);
    expect(res.statusCode).toBe(500);
  });
});

describe("refugeService.updateRefuge", () => {
  async function seedRefuge(overrides = {}) {
    await createRefuge({ body: { ...VALID_BODY, ...overrides } }, mockRes());
    return Refuge.findOne({ email: overrides.email || VALID_BODY.email });
  }

  test("403 when caller is neither owner nor admin", async () => {
    const refuge = await seedRefuge();
    const res = mockRes();
    await updateRefuge(
      { params: { id: refuge._id.toString() }, user: undefined, body: {} },
      res,
    );
    expect(res.statusCode).toBe(403);
  });

  test("200 when owner updates own profile", async () => {
    const refuge = await seedRefuge();
    const id = refuge._id.toString();
    const res = mockRes();
    await updateRefuge(
      {
        params: { id },
        user: { userId: id, role: "rifugio" },
        body: { rifugioName: "Rifugio Aggiornato", caiCode: undefined },
      },
      res,
    );
    expect(res.statusCode).toBe(200);
    expect(res.body.rifugioName).toBe("Rifugio Aggiornato");
  });

  test("200 when an admin updates any profile", async () => {
    const refuge = await seedRefuge();
    const res = mockRes();
    await updateRefuge(
      {
        params: { id: refuge._id.toString() },
        user: { userId: "507f1f77bcf86cd799439011", role: "admin" },
        body: { posti: 80 },
      },
      res,
    );
    expect(res.statusCode).toBe(200);
    expect(res.body.posti).toBe(80);
  });

  test("404 when admin updates a non-existent refuge", async () => {
    const res = mockRes();
    await updateRefuge(
      {
        params: { id: "507f1f77bcf86cd799439011" },
        user: { userId: "507f1f77bcf86cd799439099", role: "admin" },
        body: { posti: 10 },
      },
      res,
    );
    expect(res.statusCode).toBe(404);
  });

  test("400 on invalid (cast) id", async () => {
    const res = mockRes();
    await updateRefuge(
      {
        params: { id: "not-an-objectid" },
        user: { userId: "507f1f77bcf86cd799439099", role: "admin" },
        body: { posti: 10 },
      },
      res,
    );
    expect(res.statusCode).toBe(400);
  });

  test("409 when update collides with an existing email", async () => {
    const a = await seedRefuge();
    await seedRefuge({ username: "rifugio_b", email: "b@test.com" });
    const res = mockRes();
    await updateRefuge(
      {
        params: { id: a._id.toString() },
        user: { userId: a._id.toString(), role: "rifugio" },
        body: { email: "b@test.com" },
      },
      res,
    );
    expect(res.statusCode).toBe(409);
  });

  test("500 on unexpected error", async () => {
    const refuge = await seedRefuge();
    jest.spyOn(Refuge, "findByIdAndUpdate").mockReturnValueOnce({
      select: () => Promise.reject(new Error("boom")),
    });
    const res = mockRes();
    await updateRefuge(
      {
        params: { id: refuge._id.toString() },
        user: { userId: refuge._id.toString(), role: "rifugio" },
        body: { posti: 5 },
      },
      res,
    );
    expect(res.statusCode).toBe(500);
  });
});
