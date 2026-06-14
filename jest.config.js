export default {
  testEnvironment: 'node',

  // ES Modules support
  transform: {},

  // CRITICO con ESM nativo (transform: {}): il provider di coverage di default
  // è "babel" e instrumenta SOLO il codice che passa per una trasformazione.
  // Senza transform, babel non instrumenta nulla → la coverage risultava 0% su
  // tutto pur con i test verdi. "v8" usa la coverage nativa di Node e funziona
  // con gli ES Modules senza trasformazione.
  coverageProvider: 'v8',
  
  // Setup file che viene eseguito prima di ogni test suite
  setupFilesAfterEnv: ['<rootDir>/backend/__tests__/setup.js'],
  
  // Pattern per trovare i file di test
  testMatch: [
    '**/backend/__tests__/**/*.test.js',
    '**/?(*.)+(spec|test).js'
  ],
  
  // File da escludere dalla coverage
  coveragePathIgnorePatterns: [
    '/node_modules/',
    '/backend/src/server.js',
    '/backend/__tests__/setup.js',
    '/backend/__tests__/helpers/',
  ],
  
  // File da includere nella coverage
  collectCoverageFrom: [
    'backend/src/**/*.js',
    '!backend/src/server.js',
  ],

  // Gate di copertura: la CI (npm test) FALLISCE se la copertura scende sotto
  // queste soglie. Impostate poco sotto la baseline reale (stmts 84 / branch 64
  // / funcs 79 / lines 84) per evitare flakiness, da alzare ("ratchet") man mano
  // che aggiungiamo test. Previene il regresso silenzioso della copertura.
  coverageThreshold: {
    global: {
      statements: 88,
      branches: 67,
      functions: 84,
      lines: 88,
    },
  },
  
  // Mostra i test individuali nel report
  verbose: true,
  
  // Timeout per i test (utile per operazioni database)
  testTimeout: 10000,
  
  // Forza l'uscita dopo i test (evita hang con MongoDB)
  forceExit: true,

  // Pulisci i mock dopo ogni test
  clearMocks: true,

  testPathIgnorePatterns: ['/node_modules/', '/.claude/'],
};