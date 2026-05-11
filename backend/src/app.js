import express from "express";
import userRoutes from "./userRoutes.js";
import hikeSessionRoutes from "./hikeSessionRoutes.js";

const app = express();
app.use(express.json());

app.use("/users", userRoutes);
app.use("/api/v1/sessions", hikeSessionRoutes);

export default app;
