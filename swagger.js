import swaggerAutogen from 'swagger-autogen';

const doc = {
  info: {
    title: 'Trento Smart Mountain API',
    description: 'Documentazione API',
  },
  host: 'trento-smart-mountain-xz7u.onrender.com',
  schemes: ['https'],
  tags: [
    { name: 'Auth', description: 'Login, registrazione e gestione email verification / reset password' },
    { name: 'Hikers', description: 'Endpoint dedicati agli escursionisti (groupLeader)' },
    { name: 'Refuges', description: 'Endpoint dedicati agli account rifugio' },
    { name: 'Admin', description: 'Gestione utenti riservata agli amministratori' },
    { name: 'Sessions', description: 'Sessioni escursione: creazione, join, tracking, stats' },
    { name: 'Weather', description: 'Previsioni meteo TINIA con cache MongoDB 1h' },
  ],
  components: {
    securitySchemes: {
      bearerAuth: {
        type: 'http',
        scheme: 'bearer',
        bearerFormat: 'JWT',
      }
    }
  },
  security: [{ bearerAuth: [] }] 
};

const outputFile = './swagger-output.json';
const endpointsFiles = ['./backend/src/app.js'];

// --- CONFIGURAZIONE PER DISABILITARE GLI AUTO-HEADERS ---
const options = {
  openapi: '3.0.0',
  autoHeaders: false, // <--- Questo rimuoverà i campi "authorization" ovunque
  autoQuery: true,
  autoBody: true
};

// Genera passando le opzioni
swaggerAutogen(options)(outputFile, endpointsFiles, doc);
