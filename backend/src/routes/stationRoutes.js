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
    #swagger.responses[404] = {
        description: 'Nessuna stazione trovata',
        schema: { error: 'Nessuna stazione corrisponde al nome cercato' }
    }
  */
  try {
    const { name } = req.query;
    
    // Validazione input
    if (!name || name.trim() === '') {
      return res.status(400).json({ error: 'Parametro ?name= obbligatorio' });
    }

    const results = await findStationsByName(name);

    // Gestione "Not Found" logico
    // Alternativa: di solito si restituisce un array vuoto con 200 OK.
    // stile "rigido" 404 per indicare che la risorsa non esiste:
    if (!results || results.length === 0) {
      return res.status(404).json({ error: 'Nessuna stazione corrisponde al nome cercato' });
    }

    res.status(200).json({ 
      count: results.length, 
      stations: results 
    });

  } catch (e) {
    // Gestione errori specifici (se il service li lancia)
    if (e.message === 'DATABASE_TIMEOUT') {
      return res.status(503).json({ error: 'Il database è temporaneamente rallentato' });
    }
    
    // Passa gli errori tecnici imprevisti al middleware globale
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
    const result = await fetchMeteoAndPersist(codice);
    res.json(result);
  } catch (err) {
    if (err.message === "STATION_NOT_FOUND")
      return res.status(404).json({ error: "Stazione non trovata" });
    next(err);
  }
});

export default router;