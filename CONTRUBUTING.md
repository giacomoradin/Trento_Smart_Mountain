
# Contributing to Trento Smart Mountain

## 1. Project Overview

Trento Smart Mountain is a modular monolith backend system with supporting mobile and IoT components. The backend is built with Node.js (Express), MongoDB, JWT authentication, Swagger documentation, and MQTT integration for IoT communication.

The repository is structured as a monorepo with a single npm root at the repository level.

---

## 2. Repository Structure

```text
repo root (npm root)
├── package.json
├── backend/
│   └── src/
│       ├── server.js
│       ├── app.js
│       ├── apiTracker/
│       ├── models/
│       ├── routes/
│       ├── services/
│       ├── middlewares/
│
├── mobile/
├── iot/
├── docs/
```

### Key Principle

All backend execution and npm commands are managed from the repository root.

---

## 3. Setup Instructions

### Install dependencies

```bash
npm install
```

If installation fails:

```bash
rm -rf node_modules package-lock.json
npm install
```

---

## 4. Running the Project

### Development mode

```bash
npm run dev
```

### Production mode

```bash
npm start
```

### Swagger documentation generation

```bash
npm run swagger
```

---

## 5. Backend Architecture Guidelines

The backend follows a modular monolith structure.

### Layer responsibilities

| Layer        | Responsibility                           |
| ------------ | ---------------------------------------- |
| server.js    | Application entry point                  |
| app.js       | Express configuration                    |
| routes/      | HTTP route definitions                   |
| services/    | Business logic                           |
| models/      | Database schemas                         |
| middlewares/ | Authentication and request filtering     |
| apiTracker/  | Swagger generation and API documentation |

---

## 6. Code Conventions

### General rules

* Use ES Modules (`import/export`)
* Keep routes thin (no business logic in routes)
* Business logic must live in `services/`
* Database access must be handled via `models/`
* Middleware must not contain business logic

### Naming conventions

* Files: `camelCase.js`
* Services: `*Service.js`
* Models: singular lowercase (`user.js`, `hikeSession.js`)
* Routes: `*Routes.js`

---

## 7. Authentication Rules

* Authentication is based on JWT tokens
* Passwords must be hashed using bcrypt
* Auth logic must remain inside `authService.js`
* Middleware must validate tokens only (no business logic)

---

## 8. Swagger API Documentation

Swagger is generated using `swagger-autogen`.

### Important rules

* The entry file for Swagger must be `backend/src/app.js`
* Paths must always be resolved relative to the swagger script location

Correct implementation:

```js
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const endpointsFiles = [
  path.join(__dirname, "../app.js")
];
```

---

## 9. MQTT / IoT Guidelines

* MQTT logic is isolated from REST API logic
* IoT gateway runs independently in `/iot/gateway`
* Communication happens through defined MQTT topics only
* No direct coupling between IoT and database layer

---

## 10. Commit Guidelines

### Commit format

```
type(scope): description
```

### Types

* feat: new feature
* fix: bug fix
* refactor: code restructuring
* docs: documentation changes
* chore: maintenance tasks

### Examples

```
feat(auth): add JWT login flow
fix(swagger): correct app.js path resolution
refactor(services): simplify user service logic
```

---

## 11. Pull Request Guidelines

All pull requests must:

* Be focused on a single feature or fix
* Include clear description of changes
* Not mix refactoring with feature additions
* Pass local execution tests
* Maintain existing architecture boundaries

---

## 12. Execution Rules (Critical)

### Always run commands from repository root

Correct:

```bash
npm run dev
```

Incorrect:

```bash
cd backend
npm run dev
```

---

## 13. Environment Rules

* Environment variables must be defined in `.env` (root or backend depending on deployment setup)
* Never commit `.env` files
* Use `dotenv` for configuration loading

---

## 14. Development Philosophy

This project prioritizes:

* modularity over complexity
* clear separation of concerns
* AI-readable architecture documentation
* predictable system boundaries
* minimal coupling between layers

---

## 15. Areas Under Active Development

* MQTT telemetry ingestion pipeline
* Authentication improvements (refresh tokens planned)
* Mobile backend synchronization improvements
* Swagger documentation stability improvements

---

## 16. Getting Help

When contributing:

* Check `docs/` for system-specific documentation
* Review `backend/src` structure before adding new modules
* Maintain consistency with existing service architecture

