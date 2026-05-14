

## 📌 Panoramica del Sistema

Il sistema gestisce l'integrazione di dati meteorologici tra **MeteoTrentino** e un database locale **MongoDB**. L'obiettivo è la persistenza sincronizzata dei dati delle stazioni e delle temperature, garantendo coerenza tra metadati locali e sorgente remota.

---

## 🛠 Architettura e Moduli Aggiornati

### 1. Modulo Meteo: Sincronizzazione Avanzata

- **Fetch & Persist Automizzato:** La funzione `fetchMeteoAndPersist` esegue un'operazione di **Upsert** sul database locale.
    
- **Arricchimento Dati:** Recupera metadati aggiornati tramite `findRemoteStationByCode` ad ogni download di temperatura.
    

### 2. Modulo Stations: Gestione Ibrida e Persistenza

- **Refresh Centralizzato (Novità):** Implementata la rotta `PUT /:id` che forza la sincronizzazione dei metadati tecnici senza permettere modifiche manuali arbitrarie, mantenendo l'integrità dei dati ufficiali.
    
- **Robustezza e Validazione:** Tutte le rotte (`PUT`, `DELETE`, `POST`) ora includono l'importazione di `mongoose` per validare preventivamente il formato degli `ObjectId`, prevenendo crash applicativi.
    
- **Prevenzione Duplicati:** L'uso di `findOneAndUpdate` e della logica di "Refresh per ID" garantisce che ogni stazione fisica corrisponda a un unico record locale.
    

---

## 📂 Stato del Piano d'Azione CRUD

|**Risorsa**|**Metodo**|**Endpoint**|**Stato**|**Descrizione**|
|---|---|---|---|---|
|**Meteo**|`GET`|`/meteo?codice=X`|✅|Scarica temperatura e aggiorna metadati stazione nel DB.|
|**Stations**|`POST`|`/`|✅|Importazione iniziale di una stazione da remoto a locale.|
|**Stations**|`DELETE`|`/:id`|✅|Rimozione sicura tramite ID MongoDB.|
|**Stations**|`PUT`|`/:id`|✅|**Refresh Sincronizzato**: Ricarica metadati tecnici dal remoto.|
|**Stations**|`GET`|`/local/search`|✅|Recupero totale o filtrato dal DB locale.|

---

## 📝 Note Tecniche Importanti

- **Modularità ES:** Assicurarsi sempre che le nuove funzioni (come `refreshStationData`) siano esplicitamente esportate nel service e importate nelle routes per evitare `ReferenceError`.
    
- **Schema Interno:** Il modello `TemperatureList` (alias `Station`) gestisce sia l'anagrafica (`stationInfo`) che le rilevazioni (`air_temperature`).
    
- **Mongoose Import:** È obbligatorio in `stationRoutes.js` per utilizzare `mongoose.Types.ObjectId.isValid()`.
    

---

## 🧪 Workflow di Test Suggerito

Per verificare che tutto funzioni come previsto, segui questa sequenza di test (usando Postman, Swagger o cURL):

### 1. Test di Importazione (POST)

- **Azione:** `POST /stations` con body `{ "code": "T0129" }`.
    
- **Verifica:** Il database deve creare un nuovo documento. Controlla che `stationInfo` sia popolato e `air_temperature` sia `[]`.
    

### 2. Test di Sincronizzazione Meteo (GET)

- **Azione:** `GET /meteo?codice=T0129`.
    
- **Verifica:** Il documento creato al punto 1 deve ora contenere un elemento nell'array `air_temperature`. Non deve essere creato un secondo documento (duplicato).
    

### 3. Test di Refresh Metadati (PUT)

- **Azione:** `PUT /stations/[ID_DI_MONGO]`.
    
- **Verifica:** Il campo `fetchedAt` deve aggiornarsi all'ora attuale e i metadati tecnici devono essere ricaricati dal remoto. Se l'ID è malformato, deve rispondere `400`.
    

### 4. Test di Rimozione (DELETE)

- **Azione:** `DELETE /stations/[ID_DI_MONGO]`.
    
- **Verifica:** Il documento deve sparire dal DB. Una successiva `GET /local/search` non deve mostrarlo.
    

---

**Stato Integrazione:** 🟢 **Operativo**. La logica di sincronizzazione è ora completa e protetta da errori di validazione ID.