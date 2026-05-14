import express from 'express';
import { fetchMeteoAndPersist } from '../services/meteo.service.js'; 
const router = express.Router();

router.get('/', async (req, res, next) => {
  /* 
    #swagger.tags = ['Meteo']
    #swagger.summary = 'Sincronizza ultima temperatura per codice stazione'
    #swagger.description = 'Recupera i dati meteo più recenti da una stazione specifica e li salva nel database. Se non viene fornito un codice, usa quello di default.'
    
    #swagger.parameters['codice'] = {
        in: 'query',
        description: 'Codice della stazione meteo (es. T0129)',
        required: false,
        type: 'string',
        example: 'T0129'
    }

    #swagger.responses[200] = {
        description: 'Sincronizzazione completata con successo',
        schema: {
            id: '64f1a2b3c4d5e6f7g8h9i0j1',
            stationCode: 'T0129',
            count: 1
        }
    }

    #swagger.responses[400] = {
        description: 'Codice stazione mancante',
        schema: { error: 'Codice mancante (query ?codice= o METEO_STATION_CODE)' }
    }
  */
  try {
    const codice = req.query.codice || process.env.METEO_STATION_CODE;
    if (!codice)
      return res.status(400).json({ error: 'Codice mancante (query ?codice= o METEO_STATION_CODE)' });

    const { doc, count } = await fetchMeteoAndPersist(codice);
    res.json({ id: doc._id, stationCode: doc.stationCode, count });
  } catch (err) {
    next(err);
  }
});

export default router;