import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Nodo edge della rete BLE-mesh di un rifugio (es. "EDGE-NODE-01").
 *
 * Base dati per la Dashboard IoT del rifugista. NB: in questo sprint NON c'è
 * ancora il layer MQTT/ingest reale — i nodi vengono popolati con dati mock
 * (vedi refugeIotService.seedMockDashboard). Lo schema è però quello definitivo,
 * così l'ingest reale dovrà solo fare upsert su questi documenti.
 */
const edgeNodeSchema = new Schema({
  refugeId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  code: { type: String, required: true }, // "EDGE-NODE-01"
  name: { type: String, required: true }, // "Passo Principe"
  signalPct: { type: Number, min: 0, max: 100, default: 0 },
  online: { type: Boolean, default: false },
  lastSeenAt: { type: Date, default: Date.now },
});

// Un nodo è identificato univocamente da (refugio, codice).
edgeNodeSchema.index({ refugeId: 1, code: 1 }, { unique: true });

const EdgeNode = mongoose.model("EdgeNode", edgeNodeSchema);
export default EdgeNode;
