export default {
  testEnvironment: 'node',
  
  // ES Modules support
  transform: {},
  
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