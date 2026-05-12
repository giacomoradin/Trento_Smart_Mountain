import "dotenv/config";
import mongoose from "mongoose";
import swaggerUi from "swagger-ui-express";
import swaggerFile from "./apiTracker/swagger-output.json" with { type: "json" };
import app from "./app.js"; // ← importa app.js invece di ricreare Express

const PORT = process.env.PORT || 3000;
const MONGO_URI =
  process.env.MONGO_URI || "mongodb://localhost:27017/trento_smart_mountain";

// Swagger (aggiunto qui perché serve swaggerFile che è un import statico)
app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swaggerFile));

mongoose
  .connect(MONGO_URI)
  .then(() => {
    console.log("Connected to MongoDB");
    app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
  })
  .catch((error) => {
    console.error("MongoDB connection error:", error);
    process.exit(1);
  });
