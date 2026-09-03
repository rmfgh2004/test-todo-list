# Build Instructions

## Prerequisites

- Java 17 or newer. The verified environment used Temurin 17.0.18.
- Node.js 20.19 or newer and npm. The verified environment used Node.js 26.8.1 and npm 11.19.0.
- No external service is required for the standard build; U1 uses an in-memory H2 database.

## Backend

From `backend/`:

```bash
./mvnw verify
```

This compiles Java, runs the standard tests, verifies JaCoCo thresholds and formatting, packages
`target/todo-backend-0.0.1-SNAPSHOT.jar`, checks the OpenAPI contract, and creates CycloneDX SBOMs in
`target/`.

## Frontend

From `frontend/`:

```bash
npm ci
npm run verify
```

The verification chain runs type checking, lint, formatting, generated-contract drift, coverage,
the production build, and the 250KB gzip bundle gate. The deployable static output is `dist/`.

## Local execution

Run U1 from `backend/` with `./mvnw spring-boot:run`, then U2 from `frontend/` with `npm run dev`.
The services bind to `127.0.0.1:8080` and `127.0.0.1:5173`. Persistent local storage is optional;
its encrypted `file` profile requires `PLANNING_DB_PASSWORD` as documented in `backend/README.md`.
