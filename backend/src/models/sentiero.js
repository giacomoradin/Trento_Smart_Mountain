
import mongoose from "mongoose";
/**
 * Mongoose Schema for Mountain Pathways (Sentieri SAT)
 * 
 * This model represents individual hiking trails from the SAT (Società Alpinisti Tridentini) database.
 * Data is sourced from KML files containing trail metadata and coordinates.
 */

const coordinateSchema = new mongoose.Schema({
  lat: {
    type: Number,
    required: true,
    min: -90,
    max: 90
  },
  lon: {
    type: Number,
    required: true,
    min: -180,
    max: 180
  }
}, { _id: false });

const puntoSchema = new mongoose.Schema({
  nome: {
    type: String,
    required: true,
    trim: true
  },
  quota: {
    type: Number,
    required: true,
    min: 0
  },
  coordinate: {
    type: coordinateSchema,
    required: true
  }
}, { _id: false });

const sentieroSchema = new mongoose.Schema({
  // Unique trail code (e.g., "E131", "O245")
  codice: {
    type: String,
    required: true,
    unique: true,
    uppercase: true,
    trim: true,
    index: true
  },
  
  // Trail name/denomination
  denominazione: {
    type: String,
    default: '',
    trim: true
  },
  
  // Starting point
  puntoInizio: {
    type: puntoSchema,
    required: true
  },
  
  // Destination/ending point
  puntoFine: {
    type: puntoSchema,
    required: true
  },
  
  // Difficulty level (T, E, EE, EEA, etc.)
  difficolta: {
    type: String,
    required: true,
    trim: true,
    uppercase: true
  },
  
  // Elevation data (in meters)
  quotaMinima: {
    type: Number,
    required: true,
    min: 0
  },
  
  quotaMassima: {
    type: Number,
    required: true,
    min: 0
  },
  
  // Distance data (in meters)
  lunghezzaPlanimetrica: {
    type: Number,
    required: true,
    min: 0
  },
  
  lunghezzaInclinata: {
    type: Number,
    required: true,
    min: 0
  },
  
  // Hiking times (format: "HH:MM")
  tempoAndata: {
    type: String,
    required: true,
    match: /^\d{2}:\d{2}$/
  },
  
  tempoRitorno: {
    type: String,
    required: true,
    match: /^\d{2}:\d{2}$/
  },
  
  // Administrative/organizational data
  competenza: {
    type: String,
    default: '',
    trim: true
  },
  
  gruppoMontano: {
    type: String,
    default: '',
    trim: true
  },
  
  comuniToccati: {
    type: String,
    default: '',
    trim: true
  },
  
  // Route coordinates as raw string from KML
  // Format: "lon1,lat1 lon2,lat2 lon3,lat3 ..."
  // This compact format saves storage and can be parsed by the Android app
  percorsoCoordinate: {
    type: String,
    required: true
  }
  
}, {
  timestamps: true, // Adds createdAt and updatedAt
  collection: 'sentieri'
});

// Indexes for common queries
sentieroSchema.index({ 'puntoFine.nome': 1 }); // Query by destination
sentieroSchema.index({ difficolta: 1 }); // Filter by difficulty
sentieroSchema.index({ gruppoMontano: 1 }); // Filter by mountain group

// Virtual for calculating approximate trail length difference
sentieroSchema.virtual('dislivello').get(function() {
  return this.quotaMassima - this.quotaMinima;
});

// Instance method to get coordinate count
sentieroSchema.methods.getNumeroCoordinate = function() {
  return this.percorsoCoordinate.split(' ').length;
};

// Static method to find all trails leading to a specific destination
sentieroSchema.statics.findByDestination = function(destinationName) {
  return this.find({ 'puntoFine.nome': destinationName });
};

// Static method to find all trails in a difficulty range
sentieroSchema.statics.findByDifficulty = function(difficulties) {
  return this.find({ difficolta: { $in: difficulties } });
};

const Sentiero = mongoose.model('Sentiero', sentieroSchema);

export default Sentiero;