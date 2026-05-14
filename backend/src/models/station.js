import mongoose from "mongoose";

const MAX_AIR_TEMPERATURE_ITEMS = 10;

const airTemperatureSchema = new mongoose.Schema(
  {
    UM:    { type: String, default: '°C' },
    date:  { type: String, required: true },
    value: { type: Number, required: true },
  },
  { _id: false }
);

const stationInfoSchema = new mongoose.Schema(
  {
    code:      { type: String },
    name:      { type: String },
    shortname: { type: String },
    elevation: { type: Number },
    latitude:  { type: Number },
    longitude: { type: Number },
    east:      { type: Number },
    north:     { type: Number },
    startdate: { type: String },
    enddate:   { type: String, default: '' },
  },
  { _id: false }
);

const temperatureListSchema = new mongoose.Schema(
  {
    stationCode:     { type: String, index: true },
    stationInfo:     { type: stationInfoSchema, default: null },
    sourceUrl:       { type: String },
    fetchedAt:       { type: Date, default: Date.now },
    air_temperature: {
      type: [airTemperatureSchema],
      default: [],
      validate: [
        (v) => v.length <= MAX_AIR_TEMPERATURE_ITEMS,
        `air_temperature: massimo ${MAX_AIR_TEMPERATURE_ITEMS} elementi`,
      ],
    },
  },
  { collection: 'temperature_lists' }
);

export default  mongoose.model('Station', temperatureListSchema);