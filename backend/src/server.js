const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../../.env") });
const express = require("express");
const mongoose = require("mongoose");

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(express.json());

// MongoDB Connection
const connectDB = async () => {
  try {
    console.log("🔄 Tentativo di connessione a MongoDB...");

    await mongoose.connect(process.env.MONGO_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });

    console.log("✅ GRANDE! Connessione a MongoDB riuscita correttamente.");
  } catch (err) {
    console.error("❌ ERRORE DI CONNESSIONE:");
    console.error(err.message);

    if (err.message.includes("IP not whitelisted")) {
      console.log(
        "\n👉 Suggerimento: Controlla il 'Network Access' su Atlas e aggiungi 0.0.0.0/0",
      );
    }

    process.exit(1);
  }
};

// Connect to MongoDB
connectDB();

// Routes (placeholder)
app.get("/", (req, res) => {
  res.json({ message: "Trento Smart Mountain API" });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Server avviato su http://localhost:${PORT}`);
});
