# Branch `develop`

Sei sul branch di integrazione principale del progetto **Trento Smart Mountain**.

---

## A cosa serve questo branch

`develop` è il punto di convergenza di tutto il lavoro del team prima che il codice venga promosso su `main`. Ogni feature, fix o refactor viene sviluppato su un branch dedicato e poi integrato qui tramite Pull Request. Il merge su `main` avviene solo a fine sprint, quando il contenuto di `develop` è stabile e verificato.

Non si lavora direttamente su `develop`. Se devi implementare qualcosa, apri un branch da qui.

---

## Come aprire un branch da `develop`

```bash
git checkout develop
git pull origin develop
git checkout -b <nome-branch>
```

**Formato del nome branch:**

| Caso | Formato | Esempio |
|---|---|---|
| Legato a una Issue GitHub | `<numero>-<slug>` | `18-gestione-sessione-escursione` |
| Feature tematica | `<feature>-<area>` | `auth-login-jwt` |
| Bug fix (Sprint 2+) | `bugfix/<descrizione>` | `bugfix/token-expiry` |

---

## Come rientrare su `develop`

Quando il lavoro sul tuo branch è pronto, apri una **Pull Request** verso `develop` su GitHub. Il merge richiede la revisione esplicita di almeno un altro membro del team — nessun merge diretto senza approvazione.

---

## Convenzioni sui commit

Usare il formato semantico in tutti i commit:

| Prefisso | Quando usarlo |
|---|---|
| `feat:` | Nuova funzionalità |
| `fix:` | Correzione di un bug |
| `refactor:` | Modifica strutturale senza cambio di comportamento |
| `docs:` | Aggiornamenti alla documentazione |
| `chore:` | Manutenzione, configurazione, dipendenze |

---

## Regole fondamentali

- **Mai push diretto su `main`** — solo merge tramite PR approvata.
- **Mai lavorare direttamente su `develop`** — sempre su un branch dedicato.
- **Non cancellare i branch dopo il merge** — vanno mantenuti per la verifica della storia di sviluppo.
- Prima di aprire una PR, assicurarsi che il branch sia aggiornato con l'ultimo stato di `develop` (`git pull origin develop`).

---

## Branch integrati nello Sprint 1

| Branch | Contenuto |
|---|---|
| `auth-login-jwt` | Autenticazione JWT — login, verifica email, reset password |
| `crud-mongodb` | User CRUD con MongoDB/Mongoose |
| `Swagger-setup` | Documentazione OpenAPI 3.0 con swagger-autogen |
| `API-Meteo-Integration` | Integrazione meteo TINIA |
| `18-gestione-sessione-escursione` | Sessioni di escursione (Issue #18) |
| `Reorganizatio-Repo-Structure` | Refactor struttura cartelle repository |

Il merge da `develop` su `main` è avvenuto a fine Sprint 1 e costituisce la prima release stabile del progetto.

---

## Rapporto con il branch `UI`

`UI` è un branch figlio di `develop`, nato per contenere la parte grafica dell'applicazione mobile. Nel corso dello Sprint 1 ha incorporato aggiornamenti da `develop` tramite pull. Non è un sostituto di `develop` — le feature e i fix continuano a transitare da qui.
