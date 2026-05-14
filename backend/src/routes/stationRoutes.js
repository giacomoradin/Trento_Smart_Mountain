import express from 'express';
import mongoose from 'mongoose';
import { 
  findRemoteStationsByName, 
  findRemoteStationByCode,
  findLocalStationsByName,
  findLocalStationByCode,
  saveStationToDb,
  deleteStationFromDb,
  refreshStationData
} from '../services/stationRegistry.service.js';


const router = express.Router();

/* --- ROTTE REMOTE (RICERCA SU METEOTRENTINO) --- */

router.get('/remote/search', async (req, res, next) => {
  /* #swagger.tags = ['Stations']
    #swagger.summary = 'Cerca stazioni su MeteoTrentino (API Esterna)'
    #swagger.description = 'Interroga l'anagrafica XML di MeteoTrentino per trovare stazioni corrispondenti al nome fornito.'
    
    #swagger.parameters['name'] = {
        in: 'query',
        description: 'Parte del nome della stazione da cercare',
        required: true,
        type: 'string',
        example: 'Trento'
    }

    #swagger.responses[200] = {
        description: 'Lista di stazioni remote trovate',
        schema: [{
            name: 'TRENTO LARGERI',
            code: 'T0129',
            altitude: 204,
            latitude: 46.06,
            longitude: 11.12
        }]
    }

    #swagger.responses[400] = {
        description: 'Parametro mancante',
        schema: { error: 'Parametro ?name= obbligatorio' }
    }

    #swagger.responses[404] = {
        description: 'Nessun risultato trovato',
        schema: { error: 'Nessuna stazione trovata esternamente' }
    }
  */
  try {
    const { name } = req.query;
    if (!name) return res.status(400).json({ error: 'Parametro ?name= obbligatorio' });
    const results = await findRemoteStationsByName(name);
    if (!results.length) return res.status(404).json({ error: 'Nessuna stazione trovata esternamente' });
    res.json(results);
  } catch (err) { next(err); }
});

router.get('/remote/:code', async (req, res, next) => {
  /* #swagger.tags = ['Stations']
    #swagger.summary = 'Dettaglio stazione da MeteoTrentino per codice'
    #swagger.description = 'Recupera i metadati completi di una singola stazione direttamente dal fornitore esterno.'
    
    #swagger.parameters['code'] = {
        in: 'path',
        description: 'Codice identificativo della stazione (es. T0129)',
        required: true,
        type: 'string'
    }

    #swagger.responses[200] = {
        description: 'Dati tecnici della stazione remota',
        schema: {
            name: 'TRENTO LARGERI',
            code: 'T0129',
            shortname: 'Trento',
            altitude: 204,
            latitude: 46.06,
            longitude: 11.12
        }
    }

    #swagger.responses[404] = {
        description: 'Stazione non trovata',
        schema: { error: 'Stazione remota non trovata' }
    }
  */
  try {
    const station = await findRemoteStationByCode(req.params.code);
    if (!station) return res.status(404).json({ error: 'Stazione remota non trovata' });
    res.json(station);
  } catch (err) { next(err); }
});

/* --- ROTTE LOCAL (OPERAZIONI SU DATABASE MONGODB) --- */

router.get('/local/search', async (req, res, next) => {
  /* #swagger.tags = ['Stations']
    #swagger.summary = 'Recupera stazioni dal DB locale'
    #swagger.description = 'Recupera tutte le stazioni salvate nel database. Se viene fornito il parametro "name", filtra i risultati tramite regex.'
    
    #swagger.parameters['name'] = {
        in: 'query',
        description: 'Nome o parte del nome della stazione (opzionale)',
        required: false,
        type: 'string'
    }

    #swagger.responses[200] = {
        description: 'Lista di stazioni recuperate dal database',
        schema: [{
            _id: '64f1a2b3c4d5e6f7g8h9i0j1',
            name: 'TRENTO LARGERI',
            code: 'T0129',
            stationInfo: { altitude: 204, latitude: 46.06, longitude: 11.12 },
            fetchedAt: '2023-09-01T10:00:00.000Z'
        }]
    }
  */
  try {
    // Se name è undefined/null, passiamo una stringa vuota per evitare l'errore $regex
    const nameFilter = req.query.name || '';
    const results = await findLocalStationsByName(nameFilter);
    
    res.json(results);
  } catch (err) { 
    next(err); 
  }
});

router.get('/local/:code', async (req, res, next) => {
  /* #swagger.tags = ['Stations']
    #swagger.summary = 'Dettaglio stazione salvata nel DB per codice'
    #swagger.description = 'Recupera il record locale di una stazione tramite il suo codice identificativo.'
    
    #swagger.parameters['code'] = {
        in: 'path',
        description: 'Codice stazione (es. T0129)',
        required: true,
        type: 'string'
    }

    #swagger.responses[200] = {
        description: 'Dati della stazione trovati nel DB locale',
        schema: {
            _id: '64f1a2b3c4d5e6f7g8h9i0j1',
            name: 'TRENTO LARGERI',
            code: 'T0129',
            stationInfo: { altitude: 204, latitude: 46.06, longitude: 11.12 }
        }
    }

    #swagger.responses[404] = {
        description: 'Stazione non presente nel database locale',
        schema: { error: 'Stazione non presente nel database' }
    }
  */
  try {
    const station = await findLocalStationByCode(req.params.code);
    if (!station) return res.status(404).json({ error: 'Stazione non presente nel database' });
    res.json(station);
  } catch (err) { next(err); }
});

router.post('/', async (req, res, next) => {
  /* #swagger.tags = ['Stations']
    #swagger.summary = 'Importa stazione da remoto a locale' 
    #swagger.description = 'Recupera i dati aggiornati da MeteoTrentino e crea un nuovo record persistente nel database MongoDB, mappando correttamente i campi tecnici.'
    
    #swagger.parameters['body'] = {
        in: 'body',
        description: 'Codice della stazione da importare',
        required: true,
        schema: { code: 'T0129' }
    }

    #swagger.responses[201] = {
        description: 'Stazione importata e salvata con successo',
        schema: {
            _id: '64f1a2b3c4d5e6f7g8h9i0j1',
            name: 'TRENTO LARGERI',
            code: 'T0129',
            stationInfo: { altitude: 204, latitude: 46.06, longitude: 11.12 },
            fetchedAt: '2023-09-01T10:00:00.000Z'
        }
    }

    #swagger.responses[400] = {
        description: 'Body della richiesta non valido',
        schema: { error: 'Il campo code è obbligatorio' }
    }

    #swagger.responses[409] = {
        description: 'Conflitto: stazione già esistente nel DB',
        schema: { error: 'Stazione già presente nel database' }
    }
  */
  try {
    const { code } = req.body;
    if (!code) return res.status(400).json({ error: 'Il campo code è obbligatorio' });

    // 1. Prendi i dati grezzi dal fornitore esterno
    const remoteData = await findRemoteStationByCode(code);
    if (!remoteData) return res.status(404).json({ error: 'Codice stazione non trovato' });

    // 2. SALVATAGGIO CENTRALIZZATO
    // Passiamo l'oggetto ricevuto direttamente al servizio
    // Sarà il servizio (o il modello) a preoccuparsi di dove mettere i campi
    const saved = await saveStationToDb({
      stationCode: remoteData.code,
      stationInfo: remoteData, // Passiamo tutto, il modello filtrerà ciò che serve
      sourceUrl: 'Importazione Manuale' 
    });

    res.status(201).json(saved);
    
  } catch (err) { 
    if (err.code === 11000) return res.status(409).json({ error: 'Stazione già presente' });
    next(err); 
  }
});

router.put('/:id', async (req, res, next) => {
  /* #swagger.tags = ['Stations']
     #swagger.summary = 'Forza sincronizzazione metadati'
     #swagger.description = 'Ricarica i metadati tecnici (quota, coordinate) dalla sorgente remota e aggiorna il DB locale.'
  */
  try {
    const { id } = req.params;
    
    // Validazione preventiva dell'ID (come da tuo report v1.2)
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ error: 'ID non valido' });
    }

    const updated = await refreshStationData(id);
    if (!updated) return res.status(404).json({ error: 'Stazione non trovata' });

    res.json(updated);
  } catch (err) {
    next(err);
  }
});

router.delete('/:id', async (req, res, next) => {
  /* #swagger.tags = ['Stations']
    #swagger.summary = 'Elimina una stazione dal database locale'
    #swagger.description = 'Rimuove permanentemente il record di una stazione dal database locale tramite il suo ID MongoDB.'
    
    #swagger.parameters['id'] = {
        in: 'path',
        description: 'ID univoco del documento MongoDB',
        required: true,
        type: 'string'
    }

    #swagger.responses[204] = {
        description: 'Stazione eliminata con successo (nessun contenuto restituito)'
    }

    #swagger.responses[404] = {
        description: 'ID non trovato nel database',
        schema: { error: 'Stazione non trovata nel DB' }
    }
  */
  try {
    const deleted = await deleteStationFromDb(req.params.id);
    if (!deleted) return res.status(404).json({ error: 'Stazione non trovata nel DB' });
    res.status(204).send();
  } catch (err) { next(err); }
});

export default router;