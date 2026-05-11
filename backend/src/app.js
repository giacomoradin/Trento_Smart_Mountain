import express from "express";
import userRoutes from "./route/userRoutes.js";
import authRoutes from "./route/authRoutes.js";
import hikeSessionRoutes from "./route/hikeSessionRoutes.js";

const app = express();
app.use(express.json());

app.use("/users", userRoutes);
app.use("/auth", authRoutes);
app.use("/api/v1/sessions", hikeSessionRoutes);

export default app;
