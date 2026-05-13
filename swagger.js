import swaggerAutogen from 'swagger-autogen';

const doc = {
  info: {
    title: 'Trento Smart Mountain API',
    description: 'Documentazione API',
  },
  host: 'localhost:3000',
  schemes: ['http'],
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