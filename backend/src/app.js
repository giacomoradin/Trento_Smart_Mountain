import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Carica .env PRIMA di qualsiasi altro import
dotenv.config({ path: path.resolve(__dirname, '../.env') });

import express from "express";
import userRoutes from "./routes/userRoutes.js";
import authRoutes from "./routes/authRoutes.js";
import hikeSessionRoutes from "./routes/hikeSessionRoutes.js";
import cors from "cors";
import swaggerUI from "swagger-ui-express";
import { readFileSync } from 'fs';
import { globalErrorHandler, notFoundHandler } from "./middleware/errorMiddleware.js";
import weatherRoutes from "./routes/weatherRoutes.js";

const swaggerDocument = JSON.parse(readFileSync(new URL('../../swagger-output.json', import.meta.url)));
const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cors());
app.use("/api-docs", swaggerUI.serve, swaggerUI.setup(swaggerDocument));
app.use("/users", userRoutes);
app.use("/auth", authRoutes);
app.use("/api/v1/sessions", hikeSessionRoutes);
app.use("/weather", weatherRoutes);
app.use(notFoundHandler);
app.use(globalErrorHandler);

export default app;