import { jest } from "@jest/globals";
import bcrypt from "bcrypt";
import User from "../../src/models/user.js";

/**
 * Unit test dell'authService: handler Express testati direttamente con req/res
 * finti + DB in-memory. emailService e refreshTokenService sono mockati per
 * pilotare in modo deterministico ogni ramo (verify/login/refresh/logout/
 * forgot/reset, incluse le varianti JSON vs HTML del reset e i catch 500).
 */

jest.unstable_mockModule("../../src/services/emailService.js", () => ({
  sendVerificationEmail: jest.fn(),
  sendPasswordResetEmail: jest.fn(),
}));
jest.unstable_mockModule("../../src/services/refreshTokenService.js", () => ({
  generateAccessToken: jest.fn(),
  issueRefreshToken: jest.fn(),
  rotateRefreshToken: jest.fn(),
  revokeRefreshToken: jest.fn(),
}));

const {
  verifyEmail,
  loginUser,
  refreshTokens,
  logout,
  forgotPassword,
  getResetPasswordForm,
  resetPassword,
} = await import("../../src/services/authService.js");
const { sendPasswordResetEmail } = await import(
  "../../src/services/emailService.js"
);
const {
  generateAccessToken,
  issueRefreshToken,
  rotateRefreshToken,
  revokeRefreshToken,
} = await import("../../src/services/refreshTokenService.js");

function mockRes() {
  return {
    statusCode: 200,
    body: undefined,
    redirectUrl: undefined,
    status(c) {
      this.statusCode = c;
      return this;
    },
    json(b) {
      this.body = b;
      return this;
    },
    send(b) {
      this.body = b;
      return this;
    },
    redirect(u) {
      this.redirectUrl = u;
      return this;
    },
  };
}

const HEX_TOKEN = "a".repeat(64); // 64 hex chars: forma valida del reset token

async function makeUser(overrides = {}) {
  const passwordHash = await bcrypt.hash(overrides.password || "Password123!", 10);
  return User.create({
    username: overrides.username || "u1",
    email: overrides.email || "u1@test.com",
    passwordHash,
    role: "groupLeader",
    isVerified: overrides.isVerified ?? true,
    verificationToken: overrides.verificationToken,
    passwordResetToken: overrides.passwordResetToken,
    passwordResetExpires: overrides.passwordResetExpires,
  });
}

beforeEach(() => {
  jest.spyOn(console, "error").mockImplementation(() => {});
  jest.spyOn(console, "log").mockImplementation(() => {});
  generateAccessToken.mockReturnValue("access-jwt");
  issueRefreshToken.mockResolvedValue({
    raw: "refresh-raw",
    expiresAt: new Date(Date.now() + 1e9),
  });
  rotateRefreshToken.mockResolvedValue({
    accessToken: "access-2",
    refreshToken: "refresh-2",
    refreshExpiresAt: new Date(Date.now() + 1e9),
  });
  revokeRefreshToken.mockResolvedValue(undefined);
  sendPasswordResetEmail.mockResolvedValue(undefined);
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe("authService.verifyEmail", () => {
  test("verifies the user and redirects to success deep link", async () => {
    await makeUser({ isVerified: false, verificationToken: "tok123" });
    const res = mockRes();
    await verifyEmail({ params: { token: "tok123" } }, res);

    expect(res.redirectUrl).toMatch(/^tsm:\/\/auth\/success\?jwt=/);
    const updated = await User.findOne({ email: "u1@test.com" });
    expect(updated.isVerified).toBe(true);
    expect(updated.verificationToken).toBeUndefined();
  });

  test("redirects to error when token is unknown", async () => {
    const res = mockRes();
    await verifyEmail({ params: { token: "nope" } }, res);
    expect(res.redirectUrl).toContain("tsm://auth/error");
  });

  test("redirects to server-error on unexpected failure", async () => {
    jest.spyOn(User, "findOne").mockRejectedValueOnce(new Error("boom"));
    const res = mockRes();
    await verifyEmail({ params: { token: "x" } }, res);
    expect(res.redirectUrl).toContain("errore_server_interno");
  });
});

describe("authService.loginUser", () => {
  const get = () => "jest-agent";

  test("200 with access + refresh tokens on valid credentials", async () => {
    await makeUser({ email: "log@test.com", password: "Secret123!" });
    const res = mockRes();
    // get() → null copre il ramo `req.get("user-agent") || null`.
    await loginUser(
      { body: { email: "log@test.com", password: "Secret123!" }, get: () => null },
      res,
    );

    expect(res.statusCode).toBe(200);
    expect(res.body.accessToken).toBe("access-jwt");
    expect(res.body.refreshToken).toBe("refresh-raw");
  });

  test("401 when user not found", async () => {
    const res = mockRes();
    await loginUser({ body: { email: "ghost@test.com", password: "x" }, get }, res);
    expect(res.statusCode).toBe(401);
  });

  test("401 when email/username is missing (falsy identifier)", async () => {
    const res = mockRes();
    await loginUser({ body: { password: "x" }, get }, res);
    expect(res.statusCode).toBe(401);
  });

  test("401 on wrong password", async () => {
    await makeUser({ email: "log@test.com", password: "Secret123!" });
    const res = mockRes();
    await loginUser({ body: { email: "log@test.com", password: "WRONG" }, get }, res);
    expect(res.statusCode).toBe(401);
  });

  test("403 when account not verified", async () => {
    await makeUser({ email: "unv@test.com", password: "Secret123!", isVerified: false });
    const res = mockRes();
    await loginUser({ body: { email: "unv@test.com", password: "Secret123!" }, get }, res);
    expect(res.statusCode).toBe(403);
  });

  test("500 on unexpected error", async () => {
    jest.spyOn(User, "findOne").mockReturnValueOnce({
      collation: () => Promise.reject(new Error("boom")),
    });
    const res = mockRes();
    await loginUser({ body: { email: "x@test.com", password: "y" }, get }, res);
    expect(res.statusCode).toBe(500);
  });
});

describe("authService.refreshTokens", () => {
  const get = () => "jest-agent";

  test("400 when refreshToken missing", async () => {
    const res = mockRes();
    await refreshTokens({ body: {}, get }, res);
    expect(res.statusCode).toBe(400);
  });

  test("200 rotates tokens", async () => {
    const res = mockRes();
    // get() → null copre il ramo `req.get("user-agent") || null`.
    await refreshTokens({ body: { refreshToken: "r" }, get: () => null }, res);
    expect(res.statusCode).toBe(200);
    expect(res.body.accessToken).toBe("access-2");
  });

  test("401 on reused refresh token", async () => {
    rotateRefreshToken.mockRejectedValueOnce(new Error("REFRESH_TOKEN_REUSED"));
    const res = mockRes();
    await refreshTokens({ body: { refreshToken: "r" }, get }, res);
    expect(res.statusCode).toBe(401);
  });

  test("401 on invalid refresh token", async () => {
    rotateRefreshToken.mockRejectedValueOnce(new Error("REFRESH_TOKEN_INVALID"));
    const res = mockRes();
    await refreshTokens({ body: { refreshToken: "r" }, get }, res);
    expect(res.statusCode).toBe(401);
  });

  test("500 on unexpected error", async () => {
    rotateRefreshToken.mockRejectedValueOnce(new Error("boom"));
    const res = mockRes();
    await refreshTokens({ body: { refreshToken: "r" }, get }, res);
    expect(res.statusCode).toBe(500);
  });
});

describe("authService.logout", () => {
  test("200 revokes the refresh token", async () => {
    const res = mockRes();
    await logout({ body: { refreshToken: "r" } }, res);
    expect(res.statusCode).toBe(200);
    expect(revokeRefreshToken).toHaveBeenCalledWith("r");
  });

  test("500 when revoke throws", async () => {
    revokeRefreshToken.mockRejectedValueOnce(new Error("boom"));
    const res = mockRes();
    await logout({ body: { refreshToken: "r" } }, res);
    expect(res.statusCode).toBe(500);
  });
});

describe("authService.forgotPassword", () => {
  test("400 when email missing", async () => {
    const res = mockRes();
    await forgotPassword({ body: {} }, res);
    expect(res.statusCode).toBe(400);
  });

  test("200 + token saved + email sent for an existing user", async () => {
    await makeUser({ email: "fp@test.com" });
    const res = mockRes();
    await forgotPassword({ body: { email: "fp@test.com" } }, res);

    expect(res.statusCode).toBe(200);
    expect(sendPasswordResetEmail).toHaveBeenCalled();
    const u = await User.findOne({ email: "fp@test.com" });
    expect(u.passwordResetToken).toBeTruthy();
  });

  test("200 generic response for a non-existent user (no email sent)", async () => {
    const res = mockRes();
    await forgotPassword({ body: { email: "ghost@test.com" } }, res);
    expect(res.statusCode).toBe(200);
    expect(sendPasswordResetEmail).not.toHaveBeenCalled();
  });

  test("still 200 if the reset email fails to send", async () => {
    await makeUser({ email: "fp2@test.com" });
    sendPasswordResetEmail.mockRejectedValueOnce(new Error("smtp down"));
    const res = mockRes();
    await forgotPassword({ body: { email: "fp2@test.com" } }, res);
    expect(res.statusCode).toBe(200);
  });

  test("500 on unexpected error", async () => {
    jest.spyOn(User, "findOne").mockRejectedValueOnce(new Error("boom"));
    const res = mockRes();
    await forgotPassword({ body: { email: "x@test.com" } }, res);
    expect(res.statusCode).toBe(500);
  });
});

describe("authService.getResetPasswordForm", () => {
  test("400 when token is not 64-hex", () => {
    const res = mockRes();
    getResetPasswordForm({ params: { token: "bad" } }, res);
    expect(res.statusCode).toBe(400);
  });

  test("400 when token is missing (falsy)", () => {
    const res = mockRes();
    getResetPasswordForm({ params: {} }, res);
    expect(res.statusCode).toBe(400);
  });

  test("200 renders the form for a valid hex token", () => {
    const res = mockRes();
    getResetPasswordForm({ params: { token: HEX_TOKEN } }, res);
    expect(res.statusCode).toBe(200);
    expect(res.body).toContain("Nuova Password");
  });
});

describe("authService.resetPassword", () => {
  const jsonReq = (token, body) => ({
    params: { token },
    body,
    is: () => "application/json",
  });
  const htmlReq = (token, body) => ({
    params: { token },
    body,
    is: () => false,
  });

  test("[JSON] 400 when password too short", async () => {
    const res = mockRes();
    await resetPassword(jsonReq("t", { password: "short" }), res);
    expect(res.statusCode).toBe(400);
  });

  test("[JSON] 400 on invalid/expired token", async () => {
    const res = mockRes();
    await resetPassword(jsonReq("missing", { password: "longenough" }), res);
    expect(res.statusCode).toBe(400);
  });

  test("[JSON] 200 updates the password with a valid token", async () => {
    await makeUser({
      email: "rp@test.com",
      passwordResetToken: "validtok",
      passwordResetExpires: new Date(Date.now() + 3600_000),
    });
    const res = mockRes();
    await resetPassword(jsonReq("validtok", { password: "brandnew123" }), res);
    expect(res.statusCode).toBe(200);
    const u = await User.findOne({ email: "rp@test.com" });
    expect(u.passwordResetToken).toBeUndefined();
    expect(await bcrypt.compare("brandnew123", u.passwordHash)).toBe(true);
  });

  test("[JSON] 500 on unexpected error", async () => {
    jest.spyOn(User, "findOne").mockRejectedValueOnce(new Error("boom"));
    const res = mockRes();
    await resetPassword(jsonReq("t", { password: "longenough" }), res);
    expect(res.statusCode).toBe(500);
  });

  test("[HTML] 400 when passwords do not match", async () => {
    const res = mockRes();
    await resetPassword(
      htmlReq("t", { password: "longenough", confirmPassword: "different1" }),
      res,
    );
    expect(res.statusCode).toBe(400);
    expect(res.body).toContain("non corrispondono");
  });

  test("[HTML] 400 when password too short", async () => {
    const res = mockRes();
    await resetPassword(
      htmlReq("t", { password: "short", confirmPassword: "short" }),
      res,
    );
    expect(res.statusCode).toBe(400);
  });

  test("[HTML] 400 on invalid/expired token", async () => {
    const res = mockRes();
    await resetPassword(
      htmlReq("missing", { password: "longenough", confirmPassword: "longenough" }),
      res,
    );
    expect(res.statusCode).toBe(400);
  });

  test("[HTML] 200 renders success page on valid reset", async () => {
    await makeUser({
      email: "rp2@test.com",
      passwordResetToken: "htmltok",
      passwordResetExpires: new Date(Date.now() + 3600_000),
    });
    const res = mockRes();
    await resetPassword(
      htmlReq("htmltok", { password: "brandnew123", confirmPassword: "brandnew123" }),
      res,
    );
    expect(res.statusCode).toBe(200);
    expect(res.body).toContain("Password aggiornata");
  });

  test("[HTML] 500 on unexpected error", async () => {
    jest.spyOn(User, "findOne").mockRejectedValueOnce(new Error("boom"));
    const res = mockRes();
    await resetPassword(
      htmlReq("t", { password: "longenough", confirmPassword: "longenough" }),
      res,
    );
    expect(res.statusCode).toBe(500);
  });
});
