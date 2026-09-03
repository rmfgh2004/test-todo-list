# Timetable Todo Frontend

React/Vite frontend for the local timetable planning application. U1 (`../backend`) is the sole
authority for validation, conflicts, versions and persistence; this package consumes only the
generated OpenAPI contract.

## Prerequisites

- Node.js 20.19 or newer and npm
- Java 17 or newer for real-backend E2E

## SDLC Pod environment

Use Node.js **20.19 or newer**; Node 20.19.x is the reproducible baseline and the verified local run
used Node 26.8.1/npm 11.19.0. The lockfile is npm lockfile v3, so use `npm ci`, never an unconstrained
`npm install`, in a clean Pod. A cold install requires npm registry access or a pre-populated npm
cache.

Runtime dependencies are React/React DOM 19.2.8, TanStack React Query 5.102.8, dnd-kit core 6.3.1,
dnd-kit utilities 3.2.2 and Lucide React 1.38.0. The main toolchain is TypeScript 5.9.3, Vite 8.2.2,
Vitest 4.1.11, Playwright 1.62.1, ESLint 9.39.5 and Prettier 3.9.6; all direct versions are exact in
`package.json` and the full tree is pinned by `package-lock.json`.

Other directly pinned test/build packages are axe-core and `@axe-core/playwright` 4.13.0, jsdom
30.0.1, Testing Library React 16.3.3, user-event 14.6.6, jest-dom 7.0.1, fast-check 4.9.0, MSW
2.15.0, openapi-typescript 7.13.0 and CycloneDX npm 6.0.1.

After repository separation, the full verification checkout must preserve this sibling layout:
`<workspace>/frontend` and `<workspace>/backend`. Contract scripts read
`../backend/openapi/planning-api.yaml`, and Playwright starts `../backend/mvnw`; frontend-only
checkout is sufficient only for isolated type, lint, unit and build commands.

For build/unit tests the Pod needs Node and npm only. Real-backend Playwright additionally needs
**JDK 17, Bash, CA certificates, curl/wget, unzip, Chromium and its Linux libraries in the same test
container**, because Playwright starts `../backend/mvnw` itself. On Debian/Ubuntu-compatible runners:

```bash
npm ci
./node_modules/.bin/playwright install --with-deps chromium
```

Writable output paths are `node_modules/`, `dist/`, `coverage/`, `test-results/` and `sbom.json`.
Reusable caches are the npm cache and the Playwright browser directory. `CLAUDE.md` contains the
complete cache, networking, resource and artifact matrix for automated agents.

For a separated frontend repository, a Node 20.19.x Linux image is the build/unit-test baseline;
pin the image by digest in the SDLC platform. The full E2E image must additionally install JDK 17
and then run the local Playwright install command above. npm installs React and every JavaScript
framework from the lockfile; no global Vite, TypeScript, Vitest or Playwright installation is used.

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

For an isolated multi-Pod test namespace, `npm run dev -- --host 0.0.0.0` exposes Vite and
`VITE_API_BASE_URL` must be a Backend URL that the **browser**, not merely the frontend Pod, can
resolve. Configure the Backend CORS allowlist with the exact frontend Origin. The default loopback
configuration remains the recommended single-Pod/port-forward setup.

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
