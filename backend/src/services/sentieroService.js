import Sentiero from "../models/Sentiero.js";

/**
 * Servizio dedicato ai **sentieri SAT** (mountain pathways).
 *
 * Le route `/api/v1/sentieri/*` consumano queste funzioni.
 * Tutti gli endpoint sono pubblici per l'app Android.
 */

/**
 * GET /api/v1/sentieri
 * Recupera tutti i sentieri con filtri opzionali.
 */
export const getAllSentieri = async (req, res) => {
  /*
     #swagger.tags = ['Sentieri']
     #swagger.description = 'Recupera tutti i sentieri con filtri opzionali (difficoltà, destinazione).'
  */
  try {
    const { difficolta, destinazione, limit = 100 } = req.query;
    
    let query = {};
    
    if (difficolta) {
      query.difficolta = difficolta.toUpperCase();
    }
    
    if (destinazione) {
      query['puntoFine.nome'] = new RegExp(destinazione, 'i');
    }
    
    const sentieri = await Sentiero.find(query)
      .limit(parseInt(limit))
      .select('-percorsoCoordinate') // Exclude coordinates for list view (performance)
      .lean();
    
    res.status(200).json({
      message: "Sentieri recuperati con successo.",
      count: sentieri.length,
      data: sentieri
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /api/v1/sentieri/:codice
 * Recupera un singolo sentiero con coordinate complete.
 */
export const getSentieroByCode = async (req, res) => {
  /*
     #swagger.tags = ['Sentieri']
     #swagger.description = 'Recupera i dettagli completi di un sentiero (include coordinate).'
  */
  try {
    const sentiero = await Sentiero.findOne({ 
      codice: req.params.codice.toUpperCase() 
    }).lean();
    
    if (!sentiero) {
      return res.status(404).json({ message: "Sentiero non trovato." });
    }
    
    res.status(200).json({
      message: "Sentiero recuperato con successo.",
      data: sentiero
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /api/v1/sentieri/destinazioni
 * Recupera tutte le destinazioni uniche con coordinate e statistiche.
 */
export const getAllDestinazioni = async (req, res) => {
  /*
     #swagger.tags = ['Sentieri']
     #swagger.description = 'Recupera tutte le destinazioni uniche (punti finali dei sentieri).'
  */
  try {
    const destinazioni = await Sentiero.aggregate([
      {
        $group: {
          _id: '$puntoFine.nome',
          coordinate: { $first: '$puntoFine.coordinate' },
          quota: { $first: '$puntoFine.quota' },
          numeroSentieri: { $sum: 1 }
        }
      },
      {
        $project: {
          _id: 0,
          nome: '$_id',
          coordinate: 1,
          quota: 1,
          numeroSentieri: 1
        }
      },
      { $sort: { nome: 1 } }
    ]);
    
    res.status(200).json({
      message: "Destinazioni recuperate con successo.",
      count: destinazioni.length,
      data: destinazioni
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /api/v1/sentieri/destinazioni/:nome/sentieri
 * Recupera tutti i sentieri che portano a una specifica destinazione.
 */
export const getSentieriByDestination = async (req, res) => {
  /*
     #swagger.tags = ['Sentieri']
     #swagger.description = 'Recupera tutti i sentieri che conducono a una destinazione specifica.'
  */
  try {
    const destinationName = decodeURIComponent(req.params.nome);
    
    const sentieri = await Sentiero.find({
      'puntoFine.nome': destinationName
    })
      .select('-percorsoCoordinate') // Exclude coordinates for list view
      .lean();
    
    if (sentieri.length === 0) {
      return res.status(404).json({ 
        message: "Nessun sentiero trovato per questa destinazione." 
      });
    }
    
    res.status(200).json({
      message: "Sentieri recuperati con successo.",
      destinazione: destinationName,
      count: sentieri.length,
      data: sentieri
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /api/v1/sentieri/stats
 * Recupera statistiche aggregate sui sentieri.
 */
export const getStats = async (req, res) => {
  /*
     #swagger.tags = ['Sentieri']
     #swagger.description = 'Recupera statistiche aggregate (totale sentieri, difficoltà, destinazioni).'
  */
  try {
    const totalTrails = await Sentiero.countDocuments();
    
    const byDifficulty = await Sentiero.aggregate([
      { $group: { _id: '$difficolta', count: { $sum: 1 } } },
      { $sort: { _id: 1 } }
    ]);
    
    const uniqueDestinations = await Sentiero.distinct('puntoFine.nome');
    
    res.status(200).json({
      message: "Statistiche recuperate con successo.",
      data: {
        totalTrails,
        totalDestinations: uniqueDestinations.length,
        byDifficulty: byDifficulty.reduce((acc, { _id, count }) => {
          acc[_id] = count;
          return acc;
        }, {})
      }
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};