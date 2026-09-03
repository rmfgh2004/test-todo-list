# U2 Frontend Planning Experience Tech Stack Decisions

Decisions already fixed in `aidlc-inputs/01-tech-stack-decisions.md` are ratified, not re-opened.
This document records the choices that document left undecided, and the constraints attached to each.

## 1. Runtime and Build (ratified)

| Area | Decision | Rationale and constraint |
|---|---|---|
| Language | TypeScript 5.x, `strict` on | Approved baseline; `any` and non-null assertions are lint errors outside generated files. |
| Framework | React 19.2.x | Approved baseline. |
| Build | Vite 8.2.x | Approved baseline; dev server binds to loopback only and loads no third-party origin. |
| Output | Static bundle, no SSR | One local user, no server runtime to operate. |
| Server state | TanStack Query | Week/list/task cache keys, snapshot rollback and bounded retry are all first-class. |
| UI state | React local state | No global store is justified; the server is the source of truth. |
| Styling | CSS Modules + global design tokens | Approved baseline; supports the light/dark variants in the design screens with no runtime CSS-in-JS cost. |
| Icons | Lucide React | Approved baseline; icons ship in the bundle, never from a CDN. |
| Drag and drop | dnd-kit | Approved baseline; provides pointer and keyboard sensors, which NFR-004 requires. **Remediation (Tempo Phase 1, 2026-09-03)**: the shipped code used native HTML5 `draggable`/`onDrop` instead, a drift from this decision rather than a re-decision; Phase 1 replaces it with dnd-kit's sensors. |

## 2. Contract and Transport (decided here)

| Area | Decision | Rationale and constraint |
|---|---|---|
| Type generation | **`openapi-typescript`** (devDependency) | Emits type declarations only — zero runtime dependency and no generated abstraction to fight. The generated file is committed; CI regenerates and fails on a diff (NFR-007). |
| HTTP client | Hand-written `fetch` wrapper over the generated types | The functional design already fixes the client's behaviour (request-ID generation, response validation, `SafeApiError` normalization); a generated client would have to be wrapped anyway. No axios — `fetch` is sufficient and adds no dependency. |
| Query hooks | Hand-written TanStack Query hooks per feature | The snapshot/rollback and conflict-transition policy in `business-logic-model.md` is not expressible in a hook generator's template. |
| Rejected | `orval` | Generates hooks whose retry and cache semantics conflict with the approved rollback policy; wider generated surface and more configuration for no net saving. |
| Rejected | Hand-written transport types | Explicitly prohibited for U2 by `unit-of-work.md` ("no manual transport type definitions that duplicate OpenAPI"). |

The generated file is the only place transport types are defined. Any view model derives from them.

## 3. Test and Quality Tooling

| Area | Decision | Purpose |
|---|---|---|
| Unit/component | Vitest 4.1.x + React Testing Library | Approved baseline; jsdom environment. |
| Property tests | fast-check integrated into Vitest | PBT-02, PBT-03, PBT-07, PBT-08, PBT-09 with logged seeds. |
| API mocking | **MSW**, handlers typed from the generated contract types | Deterministic unit/component transport including failure paths (400 field errors, 409 conflict, 409 stale, 429 with `Retry-After`, 5xx) that are hard to provoke against a real server. A contract change breaks handler compilation. |
| E2E | Playwright 1.62.x against a **real U1 process** on the in-memory H2 profile | Satisfies the `unit-of-work.md` completion criterion; catches contract interpretation differences that mocks cannot. |
| Browser matrix | Chromium, at desktop and 320px mobile viewports | The app is a local development tool with no stated cross-browser requirement; adding WebKit/Firefox triples E2E time for no approved requirement. Revisit if a browser requirement is ever added. |
| Accessibility | `axe-core` via `@axe-core/playwright` and a Vitest integration | Serious/critical violations fail the build (NFR-004). |
| Coverage | Vitest V8 coverage gate | 80% statements/functions/lines, 75% branches. |
| Lint | ESLint with React, hooks, a11y and import-boundary rules | Enforces the no-`innerHTML`/no-`eval` rules (UR-007) and the "UI never calls the API client directly" boundary. |
| Format | Prettier | Deterministic formatting, matching U1's Spotless role. |
| Bundle budget | Build-time gzip size check, 250KB ceiling | Blocking (NFR-005). |
| Supply chain | `npm audit` at the configured severity + CycloneDX SBOM in CI | SECURITY-10, matching U1's Dependency-Check/CycloneDX gate. |

`package-lock.json` pins exact versions. A new library requires official-source, maintenance, license
and vulnerability review, and is not added when React, the browser platform or an existing dependency
provides a small testable implementation.

## 4. Profiles and Commands

| Command | Contents |
|---|---|
| `npm run verify` | type-check → lint → format check → unit/component tests + coverage gate → build + bundle ceiling |
| `npm run test:e2e` | Playwright desktop + 320px journeys against a real U1 instance |
| `npm run test:capacity` | 1,000-task fixture render measurement, separated like U1's `-Pcapacity` so the default build stays fast |

## 5. Rejected Alternatives

| Rejected | Reason |
|---|---|
| Next.js / SSR framework | No server runtime is in scope; a static bundle plus U1 is the whole deployment. |
| Redux / Zustand global store | Server state belongs to TanStack Query; UI state is local. A global store would invite duplicated business state, which UR-072 forbids. |
| HTML5 native drag-and-drop | No usable keyboard equivalent; NFR-004 would fail. |
| Tailwind or CSS-in-JS | The approved baseline is CSS Modules + tokens; changing it is scope expansion with no requirement behind it. |
| Storybook | A component catalogue is useful but is not required by any approved requirement and adds a large dependency surface. |
| Pending-save queue library | Explicitly rejected by the Q4 decision — rollback with manual retry is the approved behaviour. |
