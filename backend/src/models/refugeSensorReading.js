import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Lettura sensori ambientali di un rifugio (snapshot time-series).
 *
 * Ogni documento è una "fotografia" dei 4 sensori della Dashboard IoT
 * (temperatura esterna, umidità, vento, pressione) con il relativo trend
 * rispetto alla lettura precedente. La dashboard usa la lettura più recente
 * (`capturedAt` desc).
 *
 * I `*Trend` sono già pre-calcolati (delta vs lettura precedente) per
 * semplicità del mock: l'ingest reale potrà calcolarli o lasciarli a 0.
 */
const refugeSensorReadingSchema = new Schema({
  refugeId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  temperatureC: { type: Number },
  temperatureTrend: { type: Number, default: 0 }, // °C vs lettura precedente
  humidityPct: { type: Number },
  humidityTrend: { type: Number, default: 0 }, // % vs precedente
  windKmh: { type: Number },
  windDir: { type: String }, // "NE", "S", ...
  windGustKmh: { type: Number }, // raffica
  pressureHpa: { type: Number },
  pressureTrend: { type: Number, default: 0 }, // hPa vs precedente
  capturedAt: { type: Date, default: Date.now, index: true },
});

refugeSensorReadingSchema.index({ refugeId: 1, capturedAt: -1 });

const RefugeSensorReading = mongoose.model(
  "RefugeSensorReading",
  refugeSensorReadingSchema,
);
export default RefugeSensorReading;
