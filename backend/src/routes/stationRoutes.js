import express from 'express';
import { findStationsByName, findStationByCode } from '../services/stationRegistry.service.js';
const router = express.Router();

router.get('/search', async (req, res, next) => {
  /* 
    #swagger.tags = ['Stations']
    #swagger.summary = 'Cerca stazioni meteo per nome'
    #swagger.description = 'Endpoint per la ricerca testuale delle stazioni.'
    #swagger.parameters['name'] = {
        in: 'query',
        description: 'Nome della stazione (es. Trento)',
        required: true,
        type: 'string'
    }
    #swagger.responses[200] = {
        description: 'Lista di stazioni corrispondenti',
        schema: { 
            count: 1, 
            stations: [{ id: 1, name: 'Trento Nord', code: 'T0129' }] 
        }
    }
    #swagger.responses[400] = {
        description: 'Parametro name mancante',
        schema: { error: 'Parametro ?name= obbligatorio' }
    }
  */
  try {
    const { name } = req.query;
    if (!name) {
      return res.status(400).json({ error: 'Parametro ?name= obbligatorio' });
    }
    const results = await findStationsByName(name);
    res.json({ count: results.length, stations: results });
  } catch (e) {
    next(e);
  }
});

router.get('/:code', async (req, res, next) => {
  /* 
    #swagger.tags = ['Stations']
    #swagger.summary = 'Dettaglio stazione per codice'
    #swagger.parameters['code'] = {
        in: 'path',
        description: 'Codice univoco della stazione',
        required: true,
        type: 'string'
    }
    #swagger.responses[200] = {
        description: 'Dati della stazione trovata',
        schema: { name: 'Trento Nord', code: 'T0129', altitude: 194 }
    }
    #swagger.responses[404] = {
        description: 'Stazione non trovata',
        schema: { error: 'Stazione non trovata' }
    }
  */
  try {
    const station = await findStationByCode(req.params.code);
    if (!station) return res.status(404).json({ error: 'Stazione non trovata' });
    res.json(station);
  } catch (e) {
    next(e);
  }
});

export default router;