import { MongoMemoryServer } from "mongodb-memory-server";
import mongoose from "mongoose";

let mongoServer;

// Set JWT_SECRET for tests
process.env.JWT_SECRET =
  process.env.JWT_SECRET || "tsm-local-2026-x9Qm2pL7vN4kR8f83js055hfj2na17f";
// Forza NODE_ENV=test: i rate limiter usano questa flag per essere bypassati
// (test deterministici devono poter eseguire molte richieste consecutive).
process.env.NODE_ENV = "test";

/**
 * Setup per i test Jest con MongoDB Memory Server.
 *
 * - beforeAll: crea un'istanza MongoDB in-memory e connette Mongoose
 * - afterAll: disconnette e ferma il server
 * - afterEach: pulisce tutte le collezioni dopo ogni test
 *
 * IMPORTANTE: Questo file viene eseguito automaticamente prima dei test
 * grazie alla configurazione setupFilesAfterEnv in jest.config.
 */

// Eseguito una volta prima di tutti i test
beforeAll(async () => {
  // Chiudi eventuali connessioni esistenti
  if (mongoose.connection.readyState !== 0) {
    await mongoose.disconnect();
  }

  // Crea MongoDB in-memory (massimo 512MB come da tuo requisito)
  mongoServer = await MongoMemoryServer.create({
    instance: {
      storageEngine: "wiredTiger",
    },
  });

  const mongoUri = mongoServer.getUri();

  // Connetti Mongoose al database di test
  await mongoose.connect(mongoUri);

  console.log("✓ Test database connected (MongoDB Memory Server)");
}, 30000); // Timeout 30 secondi per il setup iniziale

// Eseguito una volta dopo tutti i test
afterAll(async () => {
  // Disconnetti Mongoose
  if (mongoose.connection.readyState !== 0) {
    await mongoose.disconnect();
  }

  // Ferma il server MongoDB in-memory
  if (mongoServer) {
    await mongoServer.stop();
  }

  console.log("✓ Test database disconnected");
}, 30000);

// Eseguito dopo ogni singolo test per pulire i dati
afterEach(async () => {
  if (mongoose.connection.readyState !== 0) {
    const collections = mongoose.connection.collections;

    // Svuota tutte le collezioni
    for (const key in collections) {
      await collections[key].deleteMany({});
    }
  }
});
