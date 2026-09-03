# Timetable Todo Frontend

React/Vite frontend for the local timetable planning application. U1 (`../backend`) is the sole
authority for validation, conflicts, versions and persistence; this package consumes only the
generated OpenAPI contract.

## Prerequisites

- Node.js 20.19 or newer and npm
- Java 17 or newer for real-backend E2E

## Install and run

```bash
npm ci
npm run dev
```

The frontend binds to `http://127.0.0.1:5173` and expects U1 at `http://127.0.0.1:8080` by default.
Start U1 from `../backend` with `./mvnw spring-boot:run`. To use the development-only MSW transport:

```bash
VITE_USE_MOCK=1 npm run dev
```

`VITE_API_BASE_URL` may select another loopback U1 URL. Mock code and the service worker are blocked
from production output.

## Contract and verification

```bash
npm run contract:generate  # regenerate types after an approved U1 contract change
npm run contract:check     # fail if committed generated types drift
npm run verify             # types, lint, format, contract, coverage, build and 250KB gate
npm run test:capacity      # 1,000-task render gate
npm run test:e2e           # starts/stops real U1 and runs desktop + 320px Playwright
npm run test:e2e:journey   # records one desktop A-Z real-use journey video
npm run audit:deps         # high-severity dependency vulnerability gate
npm run sbom               # ignored local CycloneDX 1.6 evidence file: sbom.json
```

Playwright uses U1's in-memory test profile on port 18080 and Vite on port 5180. It covers create,
delete, pointer and keyboard scheduling, unscheduling, conflict resolution, completion, list
filtering, transport-loss recovery and serious/critical axe findings. Every run records WebM videos
under the Git-ignored `test-results/regression/` directory; rerunning E2E replaces the prior run's
artifacts without deleting the separately stored A-Z evidence.
The dedicated A-Z command records one captioned desktop journey under `test-results/a-z-journey/`.
That single reviewed A-Z `video.webm` is an explicitly tracked evidence exception; all other browser
artifacts remain ignored.

## Architecture constraints

- UI components call feature hooks, never the API client.
- Wire types are aliases of `src/shared/api/generated/planning-api.d.ts`; do not hand-write them.
- Mutations are never automatically retried. Optimistic state snapshots precede writes, failures
  roll back, and successes replace cache values with the U1 payload.
- User content is rendered as text. HTML injection sinks and dynamic evaluation are lint errors.
- Board, tag, recurrence and assignee surfaces are intentionally outside the approved scope.
