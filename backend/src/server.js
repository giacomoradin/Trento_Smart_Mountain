import "dotenv/config";
import mongoose from "mongoose";
import app from "./app.js"; // ← importa app.js invece di ricreare Express
import { seedLocations } from './services/weatherService.js';

const PORT = process.env.PORT || 3000;
const MONGO_URI =
  process.env.MONGO_URI || "mongodb://localhost:27017/trento_smart_mountain";

mongoose
  .connect(MONGO_URI)
  .then(async() => {
    await seedLocations();
    console.log("Connected to MongoDB");
    app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
  })
  .catch((error) => {
    console.error("MongoDB connection error:", error);
    process.exit(1);
  });
