# Trento Smart Mountain 🏔️

## Overview
**Trento Smart Mountain** è un ecosistema digitale innovativo per l'ambiente montano, sviluppato come progetto per il corso di Ingegneria del Software (Università di Trento). La piattaforma mira a superare i limiti delle tradizionali app di navigazione passiva, offrendo strumenti attivi per la **sicurezza**, la **sostenibilità** e la **gamification**.

### Pilastri del Progetto
1.  **Sicurezza Proattiva:** Gestione delle emergenze tramite reti ibride (4G/5G e fallback BLE Mesh offline).
2.  **Sostenibilità & Gamification:** Percorsi educativi e certificazione di vetta tramite NFC per guadagnare "Social Credits" ($S_c$).
3.  **Monitoraggio IoT:** Gestione intelligente dei rifiuti e dell'affollamento nei rifugi alpini tramite Edge Computing.

---

## Architettura di Sistema
Il progetto adotta un pattern **Offline-First** basato sul principio *Store-and-Forward*, garantendo la continuità operativa anche in totale assenza di segnale.

- **Backend:** Monolite Modulare in Node.js con MongoDB.
- **Mobile:** App Android nativa in Kotlin (MVVM).
- **IoT:** Edge Gateway basati su MQTT per la comunicazione con sensori e macchinari.
- **Comunicazione:** Protocollo di relay SOS firmato ECC via BLE Mesh.

---

## Struttura della Repository (Monorepo)
- `/backend`: API RESTful e logica di business del server.
- `/mobile`: Codice sorgente dell'applicazione Android.
- `/iot`: Script e configurazioni per i gateway e i sensori di rifugio.
- `/docs`: Documentazione tecnica (D1, D2), diagrammi UML e backlog di progetto.

---

## Flusso di Lavoro (SCRUM & Git Flow)
Seguiamo la metodologia Agile SCRUM con cicli di sviluppo settimanali.

### Strategia di Branching
- `main`: Branch stabile per le release ufficiali.
- `develop`: Branch di integrazione per lo sviluppo corrente.
- `feature/<ID-UserStory>`: Branch temporanei per lo sviluppo di nuove funzionalità (es. `feature/1-login`).
- `bugfix/<descrizione>`: Branch per la risoluzione di problemi riscontrati.

### Sprint 1: Obiettivi Principali
- [ ] Implementazione Autenticazione OAuth (Google/Facebook).
- [ ] Sviluppo Android Foreground Service per tracking continuo.
- [ ] Setup schema DB e inizializzazione sessioni escursione.
- [ ] Algoritmo base per checklist dinamica (Meteo/Percorso).
- [ ] Prototipo UI per la Dashboard SOS.

---

## Setup Locale
*(Istruzioni in fase di aggiornamento)*

### Root
```bash
npm install
npm run dev
```

### Mobile
Apri la cartella `mobile/` con Android Studio e sincronizza con Gradle.

---

## Contatti e Licenza
Sviluppato da: **Giacomo Radin**

© 2026 Giacomo Radin. Tutti i diritti riservati. La riproduzione o l'uso non autorizzato di questo codice è vietata.
