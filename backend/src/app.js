import express from "express";
import userRoutes from "./routes/userRoutes.js";
import authRoutes from "./routes/authRoutes.js";
import hikeSessionRoutes from "./routes/hikeSessionRoutes.js";
import cors from "cors";
import swaggerUI from "swagger-ui-express";
import { glob, readFileSync } from 'fs';
import { globalErrorHandler, notFoundHandler } from "./middleware/errorMiddleware.js";
import weatherRoutes from "./routes/weatherRoutes.js";


const swaggerDocument = JSON.parse(readFileSync(new URL('../../swagger-output.json', import.meta.url)));
const app = express();

// Normalizza il path collassando slash multipli (es. //auth/verify → /auth/verify).
// Robustezza per i vecchi link email che potrebbero contenere doppi slash
// se BASE_URL aveva un trailing slash quando l'email è stata inviata.
app.use((req, res, next) => {
  if (req.url.match(/\/{2,}/)) {
    const cleanUrl = req.url.replace(/\/{2,}/g, "/");
    console.log(`[app] Path normalizzato: "${req.url}" → "${cleanUrl}"`);
    return res.redirect(301, cleanUrl);
  }
  next();
});

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
