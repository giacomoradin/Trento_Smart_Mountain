
import mongoose from "mongoose";

// Un singolo slot di previsione (3h o 24h)
const forecastSlotSchema = new mongoose.Schema({
  timeLayoutKey: { type: String, required: true }, // es. "18000000", "14400000"
  intervalMinutes: { type: Number, enum: [180, 1440], required: true },
  
  // Il timestamp reale calcolato dalla start + offset
  validFrom: { type: Date, required: true },
  validTo:   { type: Date, required: true },

  // Tutti i campi meteo dal JSON originale
  temperature:      Number,        // °C
  rainFall:         Number,        // mm
  rainProbability:  Number,        // %
  freshSnow:        Number,        // cm
  snowLevel:        Number,        // m slm
  windSpeed:        Number,        // km/h
  windGust:         Number,        // km/h
  windDirection:    Number,        // gradi 0-360
  freezingLevel:    Number,        // m slm
  skyCondition:     String,        // "A","B","C"... codice iconografico
  sunshineDuration: Number,        // ore
}, { _id: false }); // _id: false per non sprecare spazio su subdoc

const locationSchema = new mongoose.Schema({
  externalId: { type: String, required: true, unique: true, index: true },
  
  type: { type: String, enum: ['town', 'poi'], required: true },
  
  name: { type: String, required: true },
  elevation: Number,
  
  location: {
    type: { type: String, default: 'Point' },
    coordinates: [Number] // [lon, lat]
  },

  // Per i town: regionId è la regione geografica
  // Per i poi: regionId è l'externalId della town di riferimento → per il join
  regionId: { type: String, required: true },
  
  // Solo per i town: previsioni reali
  // I poi si collegano alla loro town tramite regionId
  forecasts: {
    fetchedAt:  Date,          // quando hai fatto l'ultima chiamata
    validFrom:  Date,          // start dal JSON
    validTo:    Date,          // end dal JSON
    slots3h:    [forecastSlotSchema],  // intervallo 180 min
    slots24h:   [forecastSlotSchema],  // intervallo 1440 min
  },

}, { timestamps: true });

locationSchema.index({ location: '2dsphere' });
locationSchema.index({ type: 1, regionId: 1 });

export default mongoose.model('Location', locationSchema);  