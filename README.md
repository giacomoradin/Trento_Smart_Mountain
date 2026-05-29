
## Checklist escursione

La checklist viene generata automaticamente per ogni sessione in stato `PLANNED` a partire dai dati del sentiero SAT e, opzionalmente, dalle previsioni meteo.

### Endpoint

| Metodo | Path | Descrizione |
|--------|------|-------------|
| `POST` | `/api/v1/sessions/:id/checklist` | Genera la checklist (solo creator) |
| `PUT` | `/api/v1/sessions/:id/checklist` | Rigenera con meteo aggiornato |
| `GET` | `/api/v1/sessions/:id/checklist` | Legge la checklist (tutti i partecipanti) |

### Body richiesto (POST e PUT)

```json
{
  "sentieroCode": "E131",
  "locationId": "uuid-location-meteo",
  "partenza": "2026-06-15T07:00:00Z"
}
```

`sentieroCode` è obbligatorio. `locationId` e `partenza` sono opzionali — senza meteo la checklist viene generata comunque dai dati del sentiero; senza `partenza` si assume la `meetingDate` della sessione alle 08:00 UTC.

### Struttura della checklist

Gli item sono raggruppati per categoria (`Abbigliamento`, `Attrezzatura`, `Sicurezza`, `Alimentazione`) e livello di priorità:

- `base` — indispensabile, senza questo non si parte
- `consigliato` — fortemente raccomandato in base al contesto
- `opzionale` — utile ma non critico

Ogni item include un campo `motivo` che spiega all'utente perché quell'oggetto è nella lista.

### Logica di generazione

I fattori considerati sono:

- **Difficoltà CAI** (`T`, `E`, `EE`, `EEA`) — determina tipo di calzature, necessità di casco, imbragatura, piccozza
- **Quota massima** — sopra i 2000 m si aggiungono strati isolanti e protezione UV; sopra i 2800 m o con zero termico vicino alla quota si aggiungono ramponi e piccozza
- **Meteo** — pioggia ≥ 40% aggiunge impermeabile in `base`; vento ≥ 50 km/h aggiunge antivento in `consigliato`; temperatura < 5°C aggiunge pile; neve fresca > 5 cm aggiunge ramponi
- **Durata** (`tempoAndata`) — calcola acqua in litri e fabbisogno calorico stimato

### Freeze

La checklist si congela alla **mezzanotte UTC del giorno prima** della `meetingDate`. Dopo il freeze qualsiasi chiamata `PUT` restituisce `403` con il timestamp di freeze. La data di freeze è esposta anche nel `GET` per permettere al client di mostrare il countdown.