import express from "express";
import userRoutes from "./routes/userRoutes.js";
import authRoutes from "./routes/authRoutes.js";
import hikeSessionRoutes from "./routes/hikeSessionRoutes.js";



const app = express();
app.use(express.json());
const cors = require('cors');
app.use(cors());
app.use("/users", userRoutes);
app.use("/auth", authRoutes);
app.use("/api/v1/sessions", hikeSessionRoutes);

export default app;
