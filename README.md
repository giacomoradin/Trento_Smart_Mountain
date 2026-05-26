# SAT Mountain Pathways - Backend Service

Backend service for managing SAT (Società Alpinisti Tridentini) mountain pathways data. Built with Node.js, Express, and MongoDB for a university Android app project.

## Project Structure

```
tsm-backend/
├── backend/
│   └── src/
│       ├── models/
│       │   ├── user.js
│       │   ├── hiker.js
│       │   ├── refuge.js
│       │   ├── admin.js
│       │   └── Sentiero.js        # NEW: Trail model
│       ├── services/
│       │   ├── hikerService.js
│       │   ├── refugeService.js
│       │   └── sentieroService.js # NEW: Trail service
│       ├── routes/
│       │   ├── hikerRoutes.js
│       │   ├── refugeRoutes.js
│       │   └── sentieroRoutes.js  # NEW: Trail routes
│       ├── utils/
│       │   └── KMLParser.js       # NEW: KML parser utility
│       ├── app.js                 # Express app 
│       └── server.js              # Server entry point
├── scripts/
│   └── importSentieri.js          # NEW: Database import script
├── data/
│   └── sentierisat/
│       ├── sentierisat.kml        # Main KML file with all trails
│       └── sentierisat.gpx/       # GPX files (currently not used)
│           ├── est/
│           └── ovest/
├── package.json                   # Dependencies (UPDATE: add xml2js)
└── .env                           # Environment variables
```

## Features

- **Sentiero Model**: Mongoose schema for mountain pathways with full metadata
- **KML Parser**: Extracts trail data from SAT KML files
- **Import Script**: Populates MongoDB with trail data
- **REST API**: Endpoints for the Android app to query trails and destinations

## Setup

### Prerequisites

- Node.js >= 16.0.0
- MongoDB (local or Atlas)
- KML data files in `data/sentierisat/` folder

### Installation

1. Install the required dependency for KML parsing:
```bash
npm install xml2js
```

**Note**: Your project already has `mongoose` installed. You only need to add `xml2js` for the KML parser.

2. Add the import script to your `package.json` scripts section:
```json
"scripts": {
  ...
  "import": "node scripts/importSentieri.js"
}
```

3. Edit `.env` with your MongoDB connection:
```env
# For local MongoDB:
MONGODB_URI=mongodb://localhost:27017/sat-pathways

# For MongoDB Atlas:
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/sat-pathways
```

4. Place the KML file in the correct location:
```
data/sentierisat/sentierisat.kml
```

5. Import trail data:
```bash
npm run import
```

6. Start the server:
```bash
# Development (with auto-reload)
npm run dev

# Production
npm start
```

## API Endpoints

All sentieri endpoints are mounted at `/api/v1/sentieri` (consistent with your existing `/api/v1/sessions` pattern).

### Get All Trails
```
GET /api/v1/sentieri?difficolta=E&limit=50
```
Query parameters:
- `difficolta` (optional): Filter by difficulty (T, E, EE, EEA)
- `destinazione` (optional): Filter by destination name (partial match)
- `limit` (optional): Max results (default: 100)

**Note**: Excludes `percorsoCoordinate` for performance.

### Get Single Trail
```
GET /api/v1/sentieri/:codice
```
Returns full trail data including coordinates.

Example: `GET /api/v1/sentieri/E131`

### Get All Destinations
```
GET /api/v1/sentieri/destinazioni
```
Returns unique destination points with:
- Name
- Coordinates (lat/lon)
- Elevation
- Number of trails leading to it

### Get Trails by Destination
```
GET /api/v1/sentieri/destinazioni/:nome/sentieri
```
Returns all trails leading to a specific destination.

Example: `GET /api/v1/sentieri/destinazioni/RIFUGIO%20%22V.%20LANCIA%22/sentieri`

### Get Statistics
```
GET /api/v1/sentieri/stats
```
Returns:
- Total number of trails
- Total number of destinations
- Trail count by difficulty level

## Data Model

### Sentiero Schema

```javascript
{
  codice: String,              // "E131", "O245", etc. (unique)
  denominazione: String,       // Trail name
  
  puntoInizio: {
    nome: String,              // Starting point name
    quota: Number,             // Elevation (meters)
    coordinate: {
      lat: Number,
      lon: Number
    }
  },
  
  puntoFine: {
    nome: String,              // Destination name
    quota: Number,
    coordinate: {
      lat: Number,
      lon: Number
    }
  },
  
  difficolta: String,          // T, E, EE, EEA
  quotaMinima: Number,         // Min elevation (meters)
  quotaMassima: Number,        // Max elevation (meters)
  lunghezzaPlanimetrica: Number, // Planimetric length (meters)
  lunghezzaInclinata: Number,    // Inclined length (meters)
  tempoAndata: String,         // "02:00" (HH:MM)
  tempoRitorno: String,        // "01:50" (HH:MM)
  
  competenza: String,          // Managing SAT section
  gruppoMontano: String,       // Mountain group
  comuniToccati: String,       // Municipalities
  
  percorsoCoordinate: String   // Raw KML coordinates string
}
```

## Integration with TSM Backend

### Step-by-Step Integration

1. **Add the Sentiero model** to `backend/src/models/Sentiero.js`

2. **Add the KML parser utility** to `backend/src/utils/KMLParser.js`

3. **Create the service** at `backend/src/services/sentieroService.js`

4. **Create the routes** at `backend/src/routes/sentieroRoutes.js`:
   ```javascript
   import express from "express";
   import { 
     getAllSentieri, 
     getSentieroByCode, 
     getAllDestinazioni, 
     getSentieriByDestination,
     getStats 
   } from "../services/sentieroService.js";

   const router = express.Router();

   router.get("/", getAllSentieri);
   router.get("/stats", getStats);  // MUST come before /:codice
   router.get("/:codice", getSentieroByCode);
   router.get("/destinazioni", getAllDestinazioni);
   router.get("/destinazioni/:nome/sentieri", getSentieriByDestination);

   export default router;
   ```

5. **Update `backend/src/app.js`** to register the routes:
   ```javascript
   import sentieroRoutes from "./routes/sentieroRoutes.js";
   
   // ... after other routes ...
   app.use("/api/v1/sentieri", sentieroRoutes);
   ```

6. **Create the import script** at `scripts/importSentieri.js`
   - Update imports to match your project structure:
     ```javascript
     import KMLParser from '../backend/src/utils/KMLParser.js';
     import Sentiero from '../backend/src/models/Sentiero.js';
     ```

7. **Update `package.json`**:
   - Add `xml2js` dependency: `npm install xml2js`
   - Add import script to scripts section

## Import Script Details

The `importSentieri.js` script:

1. Parses `sentierisat.kml` file
2. Extracts all trail placemarks
3. Processes trail metadata and coordinates
4. Imports data into MongoDB
5. Displays import statistics

### Import Statistics Example

```
Total trails in database: 245

Trails by difficulty:
  E: 180
  EE: 52
  EEA: 8
  T: 5

Unique destinations: 87

Trails by region:
  Est (E): 123
  Ovest (O): 122

Top 5 destinations (most trails):
  RIFUGIO "V. LANCIA": 8 trails
  PASSO BUOLE: 6 trails
  ...
```

## Coordinate Format

Coordinates are stored as space-separated lon,lat pairs (KML format):

```
"11.1378444,45.8545336 11.1378523,45.8544556 11.1378516,45.8543646 ..."
```

The Android app can parse this directly for rendering the trail path on the map.

## MongoDB Atlas Setup (Free Tier)

1. Create free cluster at https://cloud.mongodb.com
2. Create database user
3. Whitelist your IP (or 0.0.0.0/0 for development)
4. Get connection string
5. Update `.env` with connection string

**Note**: Free tier has 512MB storage limit. Our dataset should fit comfortably.

## Deployment on Render

1. Create new Web Service on Render
2. Connect your GitHub repository
3. Set environment variables:
   - `MONGODB_URI`: Your MongoDB Atlas connection string
   - `NODE_ENV`: `production`
4. Build command: `npm install`
5. Start command: `npm start`

After deployment, run the import script once:
```bash
# SSH into Render instance or use Render console
npm run import
```

## Development Tips

- Use Postman or Thunder Client to test API endpoints
- Check MongoDB Compass to visualize the data
- Use `nodemon` for auto-reload during development
- Monitor Render logs for production debugging

## Next Steps (To-Do)

- [ ] Create `Destinazione` model (separate collection for destinations)
- [ ] Add authentication for admin routes
- [ ] Implement caching for frequently accessed data
- [ ] Add pagination for large result sets
- [ ] Create endpoint for trail search by bounding box
- [ ] Add validation middleware
- [ ] Write unit tests
- [ ] Add API documentation (Swagger/OpenAPI)

## License

MIT - University Project

## Contributors

Your Team Names Here