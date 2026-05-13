import express from "express";
import userRoutes from "./routes/userRoutes.js";
import authRoutes from "./routes/authRoutes.js";
import hikeSessionRoutes from "./routes/hikeSessionRoutes.js";
import cors from "cors";
import swaggerUI from "swagger-ui-express";
import { readFileSync } from 'fs';

const swaggerDocument = JSON.parse(readFileSync(new URL('../../swagger-output.json', import.meta.url)));
const app = express();

app.use(express.json());
app.use(cors());
app.use("/api-docs", swaggerUI.serve, swaggerUI.setup(swaggerDocument));
app.use("/users", userRoutes);
app.use("/auth", authRoutes);
app.use("/api/v1/sessions", hikeSessionRoutes);

export default app;
