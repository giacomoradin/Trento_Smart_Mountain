import "dotenv/config";
import express from "express";
import mongoose from "mongoose";
import swaggerUi from "swagger-ui-express";
import swaggerFile from "../swagger-output.json" with { type: "json" };
import userRoutes from "./userRoutes.js";
import authRoutes from "./authRoutes.js";

const app = express();
const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI || "mongodb://localhost:27017/trento_smart_mountain";

app.use(express.json());
app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swaggerFile));
app.use("/users", userRoutes);
app.use("/auth", authRoutes);
// Middleware — parses incoming JSON request bodies
app.use(express.json());

// Routes
app.use("/users", userRoutes);

// Connect to MongoDB, then start the server
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
