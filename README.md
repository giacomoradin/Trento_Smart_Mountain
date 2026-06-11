# Test API — Jest

Branch dedicato ai test di integrazione delle API REST del backend Trento Smart Mountain.

## Stack

- **Jest** — test runner
- **Supertest** — chiamate HTTP alle route Express
- **MongoDB Memory Server** — database in-memory isolato, pulito dopo ogni test

## Struttura

backend/**tests**/
├── helpers/
│ ├── authHelper.js # Crea utenti e token JWT di test
│ └── sessionHelper.js # Crea sessioni e sentieri di test
├── routes/
│ ├── account.test.js
│ ├── activity.test.js
│ ├── auth.test.js
│ ├── challenge.test.js
│ ├── emergency.test.js
│ ├── hiker.test.js
│ ├── hikeSession.test.js
│ ├── nfc.test.js
│ ├── quiz.test.js
│ ├── refreshToken.test.js
│ ├── sentieri.test.js
│ ├── session.test.js
│ ├── social.test.js
│ └── weather.test.js
├── services/
│ └── discriminator.test.js
├── utils/
│ └── geoPolyline.test.js
└── setup.js

## Comandi

bash

# Tutti i test con coverage

npm test

# Solo un file

npm test -- --testPathPatterns=sentieri

# Watch mode (sviluppo)

npm run test:watch

## Risultati

**258 test — 258 passati — 0 falliti**

| File                    | Test | Cosa copre                                                                                                     |
| ----------------------- | ---- | -------------------------------------------------------------------------------------------------------------- |
| `auth.test.js`          | 12   | Registrazione hiker, login, validazione campi, utente non verificato                                           |
| `refreshToken.test.js`  | 8    | Login con token pair, rotation, riuso token revocato, logout                                                   |
| `account.test.js`       | 22   | Anti-cheat birthDate/caiLevel, username italiani, obiettivi, change password, eliminazione account con cascade |
| `admin.test.js`         | 13   | Update role/username utente, eliminazione, 401/403/404/400/409                                                 |
| `hiker.test.js`         | 13   | Profilo escursionista, token invalidi/scaduti/malformati, accesso tra utenti                                   |
| `session.test.js`       | 24   | Crea sessione, join, status, leave, delete, stats, live tracking, meetingDate persistence                      |
| `hikeSession.test.js`   | 23   | Checklist dinamica POST/PUT/GET, freeze, autorizzazioni, meteo opzionale                                       |
| `activity.test.js`      | 8    | Crea attività libera, isolamento tra utenti, delete                                                            |
| `social.test.js`        | 57   | Follow/unfollow, feed, like, commenti, visibilità profilo, paginazione, share sessione                         |
| `challenge.test.js`     | 10   | Crea sfida, invito, risposta, cancel, validazione date                                                         |
| `emergency.test.js`     | 9    | SOS su sessione ACTIVE, idempotency, cancel, dismiss, share con gruppo                                         |
| `nfc.test.js`           | 6    | Scan valido, anti-replay giornaliero, fuori raggio, tag sconosciuto                                            |
| `quiz.test.js`          | 6    | Lettura quiz, submit, soglia, anti doppio-credito, next quiz                                                   |
| `weather.test.js`       | 11   | Auth su tutte le route, admin-only su seed/refresh, validazione parametri                                      |
| `sentieri.test.js`      | 21   | Lista con filtri (difficoltà, dislivello, distanza, tempo), stats, dettaglio, destinazioni                     |
| `discriminator.test.js` | 4    | Persistenza campi Hiker-only (socialCredits, nfcStats, experience)                                             |
| `geoPolyline.test.js`   | 5    | Campionamento polyline, pulizia campi extra, filtro coordinate non valide                                      |

## Note

- Il database viene pulito dopo ogni singolo test (`afterEach`) — i test sono completamente indipendenti
- Le route protette richiedono JWT valido generato tramite `authHelper.js`
- Le route sentieri sono pubbliche — nessun token richiesto
- Timeout configurato a 10 secondi per operazioni database
- I test girano in parallelo su worker separati (un MongoDB Memory Server per suite)

## Novità Sprint 3

- **Social completo**: ricerca/scoperta utenti, liste follower/seguiti, metriche escursionistiche sul profilo, classifica settimanale, **notifiche** (follow/like/commento) con badge non-letti, badge "Ti segue", **gate privacy** del profilo (`profileVisibility`).
- **Dashboard IoT rifugio** (dati mock, no ingest MQTT): sensori ambientali, edge nodes BLE-mesh, passaggi/social-credit, + scheda profilo del rifugista.
- **Bacheca rifugi**: i rifugi pubblicano avvisi/info/segnalazioni di pericolo (`/api/v1/board`), consultabili dagli escursionisti da Home, Pianificazione e Registra.

---

## Checklist escursione

# Test API — Jest

Branch dedicato ai test di integrazione delle API REST del backend Trento Smart Mountain.

## Stack

- **Jest** — test runner
- **Supertest** — chiamate HTTP alle route Express
- **MongoDB Memory Server** — database in-memory isolato, pulito dopo ogni test

## Struttura

backend/**tests**/
├── helpers/
│ ├── authHelper.js # Crea utenti e token JWT di test
│ └── sessionHelper.js # Crea sessioni e sentieri di test
├── routes/
│ ├── account.test.js
│ ├── activity.test.js
│ ├── auth.test.js
│ ├── challenge.test.js
│ ├── emergency.test.js
│ ├── hiker.test.js
│ ├── hikeSession.test.js
│ ├── nfc.test.js
│ ├── quiz.test.js
│ ├── refreshToken.test.js
│ ├── sentieri.test.js
│ ├── session.test.js
│ ├── social.test.js
│ └── weather.test.js
├── services/
│ └── discriminator.test.js
├── utils/
│ └── geoPolyline.test.js
└── setup.js

## Comandi

bash

# Tutti i test con coverage

npm test

# Solo un file

npm test -- --testPathPatterns=sentieri

# Watch mode (sviluppo)

npm run test:watch

## Risultati

**258 test — 258 passati — 0 falliti**

| File                    | Test | Cosa copre                                                                                                     |
| ----------------------- | ---- | -------------------------------------------------------------------------------------------------------------- |
| `auth.test.js`          | 12   | Registrazione hiker, login, validazione campi, utente non verificato                                           |
| `refreshToken.test.js`  | 8    | Login con token pair, rotation, riuso token revocato, logout                                                   |
| `account.test.js`       | 22   | Anti-cheat birthDate/caiLevel, username italiani, obiettivi, change password, eliminazione account con cascade |
| `admin.test.js`         | 13   | Update role/username utente, eliminazione, 401/403/404/400/409                                                 |
| `hiker.test.js`         | 13   | Profilo escursionista, token invalidi/scaduti/malformati, accesso tra utenti                                   |
| `session.test.js`       | 24   | Crea sessione, join, status, leave, delete, stats, live tracking, meetingDate persistence                      |
| `hikeSession.test.js`   | 23   | Checklist dinamica POST/PUT/GET, freeze, autorizzazioni, meteo opzionale                                       |
| `activity.test.js`      | 8    | Crea attività libera, isolamento tra utenti, delete                                                            |
| `social.test.js`        | 57   | Follow/unfollow, feed, like, commenti, visibilità profilo, paginazione, share sessione                         |
| `challenge.test.js`     | 10   | Crea sfida, invito, risposta, cancel, validazione date                                                         |
| `emergency.test.js`     | 9    | SOS su sessione ACTIVE, idempotency, cancel, dismiss, share con gruppo                                         |
| `nfc.test.js`           | 6    | Scan valido, anti-replay giornaliero, fuori raggio, tag sconosciuto                                            |
| `quiz.test.js`          | 6    | Lettura quiz, submit, soglia, anti doppio-credito, next quiz                                                   |
| `weather.test.js`       | 11   | Auth su tutte le route, admin-only su seed/refresh, validazione parametri                                      |
| `sentieri.test.js`      | 21   | Lista con filtri (difficoltà, dislivello, distanza, tempo), stats, dettaglio, destinazioni                     |
| `discriminator.test.js` | 4    | Persistenza campi Hiker-only (socialCredits, nfcStats, experience)                                             |
| `geoPolyline.test.js`   | 5    | Campionamento polyline, pulizia campi extra, filtro coordinate non valide                                      |

## Note

- Il database viene pulito dopo ogni singolo test (`afterEach`) — i test sono completamente indipendenti
- Le route protette richiedono JWT valido generato tramite `authHelper.js`
- Le route sentieri sono pubbliche — nessun token richiesto
- Timeout configurato a 10 secondi per operazioni database
- I test girano in parallelo su worker separati (un MongoDB Memory Server per suite)
