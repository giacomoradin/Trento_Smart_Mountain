import { createTestHiker } from "../helpers/authHelper.js";
import { addCredits, getCreditsWithLevel } from "../../src/services/creditService.js";
import Hiker from "../../src/models/hiker.js";
import User from "../../src/models/user.js";

/**
 * Test che verifica la persistenza dei campi del discriminator Hiker.
 *
 * Storico del bug:
 * Prima del fix, `User.findByIdAndUpdate(id, { $inc: { socialCredits: N } })`
 * scartava silenziosamente l'$inc perché `socialCredits` non è nello schema
 * base User (solo nel sub-schema Hiker). Risultato: NFC scan / quiz /
 * sessioni mostravano "+N crediti" lato client ma il DB restava a 0.
 *
 * Questo test fissa il contratto: addCredits DEVE incrementare socialCredits
 * nel documento Hiker. Se qualcuno regredisce a User.findByIdAndUpdate, il
 * test fallisce immediatamente.
 */

describe("Discriminator fields persistence", () => {
  test("addCredits incrementa socialCredits sul documento Hiker", async () => {
    const { user } = await createTestHiker({
      email: "disc1@test.com",
      username: "disc1",
    });

    // Stato iniziale: 0 crediti
    const initial = await Hiker.findById(user._id).select("socialCredits").lean();
    expect(initial.socialCredits ?? 0).toBe(0);

    // addCredits chiama internamente findByIdAndUpdate con $inc
    await addCredits({
      userId: user._id,
      amount: 50,
      source: "admin_adjust",
      refKind: "Test",
    });

    // Verifica via Hiker (sa di socialCredits)
    const afterHiker = await Hiker.findById(user._id).select("socialCredits").lean();
    expect(afterHiker.socialCredits).toBe(50);

    // Verifica via User (deve restituire lo stesso valore — è la stessa collection)
    const afterUser = await User.findById(user._id).select("socialCredits").lean();
    expect(afterUser.socialCredits).toBe(50);
  });

  test("getCreditsWithLevel restituisce il totale corretto post-addCredits", async () => {
    const { user } = await createTestHiker({
      email: "disc2@test.com",
      username: "disc2",
    });

    await addCredits({ userId: user._id, amount: 100, source: "nfc", refKind: "NfcTotem" });
    await addCredits({ userId: user._id, amount: 75, source: "quiz", refKind: "Quiz" });

    const result = await getCreditsWithLevel(user._id);
    expect(result.total).toBe(175);
    expect(result.level).toBeDefined();
  });

  test("nfcStats si incrementa correttamente (campo Hiker-only)", async () => {
    // Simula il pattern di nfcService: $inc su nfcStats.scansCount.
    // Se usato con User.findByIdAndUpdate, il valore non sale.
    const { user } = await createTestHiker({
      email: "disc3@test.com",
      username: "disc3",
    });

    await Hiker.findByIdAndUpdate(user._id, {
      $inc: { "nfcStats.scansCount": 1, "nfcStats.scansCredits": 25 },
    });

    const after = await Hiker.findById(user._id).select("nfcStats").lean();
    expect(after.nfcStats?.scansCount).toBe(1);
    expect(after.nfcStats?.scansCredits).toBe(25);
  });

  test("User.findById restituisce campi discriminator se selezionati (read OK)", async () => {
    // Verifica che il pattern READ con User base funzioni: il select esplicito
    // proietta il campo discriminator a livello MongoDB anche se non è nello
    // schema base. Questo conferma che i call site di hikeSessionService che
    // leggono `experience` via User non sono buggy (solo le WRITE lo sono).
    const { user } = await createTestHiker({
      email: "disc4@test.com",
      username: "disc4",
    });
    await Hiker.findByIdAndUpdate(user._id, {
      $set: { "experience.caiLevel": "EE", "experience.baselineFitness": "athlete" },
    });

    // Lettura con User base e select esplicito
    const userBase = await User.findById(user._id).select("experience").lean();
    expect(userBase.experience).toBeDefined();
    expect(userBase.experience.caiLevel).toBe("EE");
    expect(userBase.experience.baselineFitness).toBe("athlete");
  });
});
