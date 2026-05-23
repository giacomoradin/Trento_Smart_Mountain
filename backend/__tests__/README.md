# Jest Testing Setup - Trento Smart Mountain Backend

Questa guida spiega come utilizzare i test Jest per il backend TSM.

## 📁 Struttura Test

```
backend/
├── __tests__/
│   ├── setup.js                    # Configurazione MongoDB Memory Server
│   ├── helpers/
│   │   └── authHelper.js           # Utility per creare utenti e token JWT
│   └── routes/
│       ├── auth.test.js            # Test registrazione e login
│       └── hiker.test.js           # Test route protette escursionisti
└── src/
    ├── app.js                      # Express app (usata nei test)
    ├── server.js                   # Server startup (NON usato nei test)
    └── ...

jest.config.js                       # Configurazione Jest
package.json                         # Script npm test
```

## 🚀 Come Eseguire i Test

### Esegui tutti i test
```bash
npm test
```

### Esegui test in modalità watch (rilancia automaticamente quando modifichi codice)
```bash
npm run test:watch
```

### Esegui un singolo file di test
```bash
NODE_OPTIONS=--experimental-vm-modules npx jest backend/__tests__/routes/auth.test.js
```

### Esegui test senza coverage report
```bash
NODE_OPTIONS=--experimental-vm-modules npx jest
```

### Esegui test con output dettagliato
```bash
npm test -- --verbose
```

## 📊 Coverage Report

Dopo aver eseguito `npm test`, il report di coverage viene generato in:
- Console: riepilogo testuale
- `coverage/lcov-report/index.html`: report HTML navigabile (apri nel browser)

```bash
# Apri il report HTML (macOS)
open coverage/lcov-report/index.html

# Linux
xdg-open coverage/lcov-report/index.html

# Windows
start coverage/lcov-report/index.html
```

## 🧪 Cosa Viene Testato

### `auth.test.js` - Autenticazione
✅ **POST /auth/register/hiker**
- ✓ Registrazione con dati validi
- ✓ Token JWT restituito
- ✓ Errore per campi mancanti
- ✓ Errore per username duplicato
- ✓ Errore per email duplicata
- ✓ Errore per email invalida
- ✓ Errore per password debole

✅ **POST /auth/login**
- ✓ Login con credenziali corrette
- ✓ Token JWT restituito
- ✓ Errore per password errata
- ✓ Errore per email inesistente
- ✓ Errore per campi mancanti
- ✓ Errore per utente non verificato

### `hiker.test.js` - Route Escursionisti
✅ **GET /hikers/:id**
- ✓ Accesso con token valido
- ✓ Errore 401 senza token
- ✓ Errore 401 con token invalido (secret sbagliato)
- ✓ Errore 401 con token scaduto
- ✓ Errore 401 con token malformato
- ✓ Errore 401 senza prefisso "Bearer"
- ✓ Errore 404 per ID inesistente
- ✓ Errore 400 per ID invalido
- ✓ Utente accede al proprio profilo
- ✓ Utente visualizza profilo di altri (se permesso)

✅ **PUT /hikers/:id**
- ✓ Aggiornamento profilo con token valido
- ✓ Errore 401 senza token
- ✓ Protezione campi sensibili (role, passwordHash)

## 🛠️ Come Funzionano i Test

### 1. Setup Database (automatico)
Il file `setup.js` viene eseguito automaticamente prima dei test:
- **beforeAll**: Crea MongoDB Memory Server (database in-memory)
- **afterEach**: Pulisce tutte le collezioni dopo ogni test
- **afterAll**: Disconnette e ferma il database

### 2. Helper Functions
`authHelper.js` fornisce utility per i test:

```javascript
import { createTestHiker, generateInvalidToken } from '../helpers/authHelper.js';

// Crea un escursionista e ottieni token JWT
const { user, token, password } = await createTestHiker({
  username: 'mario',
  email: 'mario@test.com'
});

// Genera token invalido per testare errori
const badToken = generateInvalidToken();
```

### 3. Esempio Test con SuperTest

```javascript
import request from 'supertest';
import app from '../../src/app.js';

test('should login with correct credentials', async () => {
  const { password } = await createTestHiker({
    email: 'test@example.com'
  });

  const response = await request(app)
    .post('/auth/login')
    .send({
      email: 'test@example.com',
      password: password
    });

  expect(response.status).toBe(200);
  expect(response.body).toHaveProperty('token');
});
```

## 📝 Scrivere Nuovi Test

### Template Base

```javascript
import request from 'supertest';
import app from '../../src/app.js';
import { createTestHiker } from '../helpers/authHelper.js';

describe('Nome della Route/Feature', () => {
  
  describe('HTTP_METHOD /endpoint', () => {
    
    test('should do something when valid', async () => {
      // Arrange: prepara dati
      const { token } = await createTestHiker();
      
      // Act: esegui richiesta
      const response = await request(app)
        .post('/your/endpoint')
        .set('Authorization', `Bearer ${token}`)
        .send({ data: 'value' });
      
      // Assert: verifica risultato
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('expectedField');
    });
    
    test('should fail when invalid', async () => {
      const response = await request(app)
        .post('/your/endpoint')
        .send({ invalid: 'data' });
      
      expect(response.status).toBe(400);
    });
  });
});
```

### Best Practices

1. **Isola i test**: Ogni test deve essere indipendente
2. **Usa nomi descrittivi**: `should return 401 when token is missing`
3. **Testa casi limite**: valori nulli, stringhe vuote, ID invalidi
4. **Testa scenari di errore**: non solo il "happy path"
5. **Non fare assunzioni sui dati**: usa `createTestHiker()` per ogni test

## 🐛 Troubleshooting

### ❌ Test timeout
**Problema**: `Timeout - Async callback was not invoked within the 5000 ms timeout`

**Soluzione**: Il test supera i 5 secondi. Aumenta il timeout in `jest.config.js`:
```javascript
testTimeout: 10000 // 10 secondi
```

### ❌ MongoDB connection error
**Problema**: `MongoMemoryServer failed to start`

**Soluzione**: 
1. Verifica che `mongodb-memory-server` sia installato: `npm install --save-dev mongodb-memory-server`
2. Controlla che non ci siano altre istanze MongoDB in ascolto sulla stessa porta
3. Prova a cancellare cache: `rm -rf ~/.cache/mongodb-binaries`

### ❌ Cannot find module
**Problema**: `Cannot find module './models/user.js'`

**Soluzione**: Assicurati che i path relativi includano `.js`:
```javascript
import User from '../../src/models/user.js'; // ✓
import User from '../../src/models/user';    // ✗
```

### ❌ Tests hang dopo completamento
**Problema**: I test completano ma il processo non termina

**Soluzione**: Verifica che `forceExit: true` sia in `jest.config.js`

### ❌ Module import errors
**Problema**: `SyntaxError: Cannot use import statement outside a module`

**Soluzione**: Verifica che `package.json` abbia `"type": "module"` e che gli script usino `NODE_OPTIONS=--experimental-vm-modules`

## 📚 Matcher Jest Più Usati

```javascript
// Uguaglianza
expect(value).toBe(4);                    // Strict equality (===)
expect(object).toEqual({ a: 1 });         // Deep equality

// Proprietà oggetti
expect(obj).toHaveProperty('key');
expect(obj).toHaveProperty('key', 'value');

// Truthiness
expect(value).toBeTruthy();
expect(value).toBeFalsy();
expect(value).toBeNull();
expect(value).toBeUndefined();
expect(value).toBeDefined();

// Numeri
expect(number).toBeGreaterThan(3);
expect(number).toBeLessThanOrEqual(5);

// Stringhe
expect(string).toMatch(/regex/);
expect(string).toContain('substring');

// Array
expect(array).toContain(item);
expect(array).toHaveLength(3);

// Async/Promises
await expect(promise).resolves.toBe(value);
await expect(promise).rejects.toThrow();

// Mock/Spy (avanzato)
expect(mockFn).toHaveBeenCalled();
expect(mockFn).toHaveBeenCalledWith('arg');
```

## 🎯 Prossimi Passi

1. ✅ Test per autenticazione (POST /auth/register, /auth/login)
2. ✅ Test per route protette (GET /hikers/:id)
3. ⬜ Test per HikeSession (POST /api/v1/sessions, GET /api/v1/sessions)
4. ⬜ Test per Refuge routes (POST /refuges, GET /refuges/:id)
5. ⬜ Test per Admin routes
6. ⬜ Test per Weather routes
7. ⬜ Test per middleware (errorMiddleware, authMiddleware)
8. ⬜ Test per services (hikerService, authService)

## 📞 Supporto

Per domande o problemi:
1. Consulta la [documentazione Jest](https://jestjs.io/docs/getting-started)
2. Consulta la [documentazione SuperTest](https://github.com/ladjs/supertest)
3. Controlla i log di errore nei test
4. Verifica che tutte le dipendenze siano installate: `npm install`

---

**Creato**: Maggio 2026  
**Autore**: Giacomo Radin  
**Progetto**: Trento Smart Mountain Backend