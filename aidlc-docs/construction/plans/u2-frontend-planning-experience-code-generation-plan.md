# U2 Frontend Planning Experience Code Generation Plan

This plan is the single source of truth for U2 code generation. Nothing is generated that is not
written here, and each step is marked [x] in the same interaction in which it completes.

Approved inputs: U2 functional design (business-logic-model, business-rules, domain-entities,
frontend-components), U2 NFR requirements (nfr-requirements, tech-stack-decisions), U2 NFR design
(nfr-design-patterns, logical-components), and the U1 contract at `backend/openapi/planning-api.yaml`.

## Part 1 Approval

- [x] Functional, NFR requirement and NFR design artifacts analyzed
- [x] Unit stories, dependencies, interfaces and owned components analyzed
- [x] Workspace root and greenfield multi-unit code location validated (`frontend/`, absent today)
- [x] Exact generation paths and sequential TDD steps documented
- [x] Open questions below answered (A / A / A)
- [x] Entire generation sequence explicitly approved

## Unit Context

- **Unit**: U2 Frontend Planning Experience
- **Code location**: `frontend/` at the workspace root (greenfield multi-unit layout from
  `unit-of-work.md`). No application code is ever written under `aidlc-docs/`.
- **Documentation location**: `aidlc-docs/construction/u2-frontend-planning-experience/code/`
- **Owns**: F-C01~F-C09, F-N01~F-N09, S-F01~S-F04
- **Depends on**: U1 over local HTTP; the committed OpenAPI contract is the only interface
- **Owns no persistence**: every authoritative decision (overlap, version, durability) stays in U1
- **Runtime present**: Node v26.8.1, npm 11.19.0
- **Stack (already ratified, not reopened)**: TypeScript 5.x strict, React 19.2.x, Vite 8.2.x,
  TanStack Query, CSS Modules + tokens, Lucide, dnd-kit, Vitest 4.1.x + RTL, fast-check, MSW,
  Playwright 1.62.x (Chromium desktop + 320px), axe, ESLint + Prettier, openapi-typescript

## Open Questions

### Q1 — How closely should the design screens be reproduced?

The 15 reference screens in `aidlc-inputs/design/` fix the visual language, but no approved document
specifies exact colour, spacing or type values, and the screens contain out-of-scope surfaces.

- **(A) Structure-faithful, token-derived (recommended)** — reproduce layout, hierarchy, density and
  component anatomy from the screens; derive the token palette from them but adjust any pair that
  fails the WCAG AA contrast gate, and drop out-of-scope surfaces entirely.
- **(B) Pixel-faithful** — match sampled colours and spacing exactly, then fix contrast failures only
  where axe blocks the build. Higher fidelity, but the axe gate is already blocking, so failures
  become rework rather than a decision.
- **(C) Neutral system look** — ignore the screens' palette and ship a plain accessible default. Fast,
  but discards a reviewed input for no requirement.

[Answer]: **(A) Structure-faithful, token-derived.** Layout, hierarchy, density and component anatomy
follow the reference screens. The token palette is derived from them, and any pair failing the WCAG AA
contrast gate is adjusted rather than shipped. Out-of-scope surfaces are removed, not disabled.

### Q2 — What reconnect polling interval should the connectivity banner use?

The approved NFR design fixes the behaviour (queries poll while disconnected, mutations never
auto-retry) but leaves the interval unset.

- **(A) 5 seconds, capped (recommended)** — poll every 5s while disconnected, stop after 2 minutes of
  failure and leave a manual "다시 시도" control. Recovers a restarted backend within one attempt of a
  typical boot, and the cap prevents an idle tab polling forever.
- **(B) Exponential 2s → 30s** — faster first recovery, but the interval a user experiences depends on
  how long the tab has been disconnected, which is harder to test deterministically.
- **(C) 15 seconds, uncapped** — least traffic, slowest recovery.

[Answer]: **(A) 5 seconds, capped at 2 minutes.** While disconnected, active queries refetch every 5s.
After 24 consecutive failed attempts (2 minutes) polling stops and the banner offers a manual retry.
Mutations never auto-retry at any point. The interval and cap are injected constants so tests are
deterministic.

### Q3 — How does the E2E suite obtain a running U1?

`unit-of-work.md` requires Playwright journeys against a real U1 on the in-memory H2 profile.

- **(A) Playwright starts and stops U1 itself (recommended)** — a `webServer` entry runs the Maven
  wrapper with the test profile, waits for `/actuator/health`, and tears it down. One command
  (`npm run test:e2e`) works locally and in CI, at the cost of a JVM boot inside the E2E run.
- **(B) Assume an already-running U1** — the script only checks health and fails fast with
  instructions. Faster loop while developing, but CI needs separate orchestration.
- **(C) Both, flag-selected** — `E2E_EXTERNAL_BACKEND=1` skips the managed start. Most flexible, one
  extra branch to maintain.

[Answer]: **(A) Playwright starts and stops U1 itself.** The Playwright `webServer` config runs the
Maven wrapper on the in-memory test profile, waits on `/actuator/health`, and tears it down afterwards,
so `npm run test:e2e` is the single command locally and in CI.

## Generation Steps

Each implementation step is test-first: the tests are written and observed failing before the code
that satisfies them, mirroring U1's TDD sequence. A security-checklist pass runs at the end of every
code batch, as it did for U1.

### Step 1 - Frontend Scaffold and Verification Gate

- [x] Create `frontend/package.json`, `package-lock.json`, `tsconfig*.json`, `vite.config.ts`,
      `.eslintrc`/flat config, `.prettierrc`, `index.html` and `src/main.tsx` with only the ratified
      dependencies at the approved versions.
- [x] Configure the ESLint import-boundary rule (UI never imports the API client; features never
      import each other's components; generated types import nothing) and the no-`innerHTML` /
      no-`dangerouslySetInnerHTML` / no-`eval` rules with no allowlist.
- [x] Configure Vitest (jsdom, V8 coverage gate 80% statements/functions/lines, 75% branches),
      fast-check and the `verify` / `test:e2e` / `test:capacity` scripts.
- [x] Add a scaffold smoke test first, make it pass, run the code-batch security review, then mark
      Step 1.
      **Done.** ESLint 10 was rejected: `eslint-plugin-jsx-a11y` 6.10.2 (required by NFR-004) has no
      ESLint 10 peer range, and forcing it with `--legacy-peer-deps` would weaken the exact-dependency
      posture of SECURITY-10. Pinned ESLint 9.39.5 instead; `npm audit` reports 0 vulnerabilities.
      Revisit when jsx-a11y adds ESLint 10 support.

### Step 2 - Contract Types, Contract Fix and Drift Gate

- [x] Add the additive 429 / `RATE_LIMITED` / `Retry-After` documentation to
      `backend/openapi/planning-api.yaml` (carried U1 defect; documentation only, no U1 behaviour
      change) and extend the U1 drift test so response codes are compared, not only path and method.
- [x] Generate `frontend/src/shared/api/generated/planning-api.d.ts` with `openapi-typescript`,
      commit it, and add the CI regeneration check that fails on any diff (F-N07, NFR-007).
- [x] Verify no hand-written transport type exists anywhere in `frontend/src`.
- [x] Run the U1 build to confirm the contract change breaks nothing, run the security review, mark
      Step 2.
      **Done.** The extended drift test was negative-tested: removing one `'429'` entry fails
      `NFR_007_every_operation_documents_the_globally_reachable_rate_limit_response`, and the contract
      was restored byte-identically (confirmed by `npm run contract:check`).

### Step 3 - Pure Core Tests First (time, geometry, capacity)

- [x] Create failing tests under `frontend/src/shared/time/` and `frontend/src/shared/grid/` for ISO
      parse/format round trips, 15-minute alignment, `slotSpan = estimateMinutes / 15`, the
      08:00~22:00 window, week ranges and derived available minutes.
- [x] Add reusable fast-check generators for `TaskView`, `ScheduleView` and drop positions with seed
      logging (PBT-02, PBT-03, PBT-07, PBT-08, PBT-09).
- [x] Confirm the tests fail for missing implementation.
      **Done.** All three suites failed first because `calendar`, `geometry` and `capacity` did not
      exist. The UR-012 exact-midpoint ambiguity was resolved by the user as option A: round upward.

### Step 4 - Pure Core Implementation

- [x] Implement the geometry and time helpers as pure functions with no DOM, cache or network access,
      each carrying its stable FR/UR/PBT ID in a docstring.
- [x] Make Step 3 tests pass, add a fixed regression example for every shrunk counterexample, run the
      security review, mark Steps 3~4.
      **Done.** 20 Step 3~4 tests pass with fixed seed `20260901`; shrinking remains enabled and no
      counterexample was found, so no new shrunk regression case was required. Full `npm run verify`
      passes with 21 tests, 96.29% line and 88.76% branch coverage, and a 58.0KB gzip bundle.

### Step 5 - Transport Tests First (client, errors, mocks)

- [ ] Create MSW handlers typed from the generated contract covering success plus 400 field errors,
      404, 409 conflict, 409 stale, 429 with `Retry-After`, 5xx and transport loss (F-N08).
- [ ] Create failing tests for the fetch wrapper (request-ID generation and propagation, response
      validation, allowlisted query serialization with out-of-range fallback) and for the error
      normalizer's seven `SafeApiError` kinds including `unknown` degradation (F-N05, SECURITY-05).
- [ ] Confirm the failing state.

### Step 6 - API Client, Error Normalizer and Connectivity Monitor

- [ ] Implement `frontend/src/shared/api/` — client, normalizer, connectivity state from consecutive
      transport failures with the Q2 interval, and the mock installer gated on `VITE_USE_MOCK=1`
      (F-C08, F-N03, F-N05, F-N08).
- [ ] Assert that only transport failures raise `disconnected`; any HTTP status routes normally.
- [ ] Add the production-build check that fails if the mock module is reachable from the bundle.
- [ ] Make Step 5 tests pass, run the security review, mark Steps 5~6.

### Step 7 - Cache and Mutation Coordinator Tests First

- [ ] Create failing tests for cache keys (`['week', weekStart]`, `['tasks', query]`, `['task', id]`),
      scoped invalidation including the cross-week move, bounded query retry (max 2) and no automatic
      mutation retry (F-N02).
- [ ] Create failing tests for snapshot-and-rollback: snapshot before optimistic write, rollback
      completes before the conflict dialog renders, success replaces rather than merges, delete is
      never optimistic, single-flight per task, and the S-F03 state machine transitions (F-N04).
- [ ] Confirm the failing state.

### Step 8 - Cache Policy and Mutation Coordinator

- [ ] Implement the query client configuration and the mutation coordinator with `expectedVersion`
      propagation, snapshot rollback and the single-flight guard.
- [ ] Make Step 7 tests pass, run the security review, mark Steps 7~8.

### Step 9 - Design Tokens, Shell and Feedback Tests First

- [ ] Create failing tests for the theme root (system default, persisted manual override, no component
      reading the theme), the responsive breakpoint, the route error boundaries (fallback shows the
      request ID, never a stack trace), the two live regions with exactly one announcement per
      transition, and the connectivity banner with mutating controls disabled (F-N01, F-N06, F-C01,
      F-C09).
- [ ] Confirm the failing state.

### Step 10 - Tokens, Application Shell and Accessible Feedback

- [ ] Create the light/dark token files derived per the Q1 answer, the shell layout, navigation,
      theme toggle, skeleton/empty/error surfaces, toasts, copyable request ID and the
      `Retry-After`-gated retry control.
- [ ] Make Step 9 tests pass, run the axe integration on both themes, run the security review, mark
      Steps 9~10.

### Step 11 - Feature Slice Tests First (US-001~US-009)

- [ ] Create failing component tests per slice under `frontend/src/features/`:
      timetable (F-C02 grid, capacity indicator, week navigation), backlog (F-C03), task editor
      (F-C04 create/update/delete with confirm and rollback), scheduling interaction (F-C05 pointer,
      keyboard and date-time form converging on one `SlotProposal`), conflict resolution (F-C06
      comparison, choice, focus restoration) and task list (F-C07 URL-driven filters, paging, empty
      state).
- [ ] Assert render-count stability on the 56x7 grid during drag (NFR-005) and that out-of-scope
      controls are absent rather than disabled (UR-070~072).
- [ ] Confirm the failing state.

### Step 12 - Feature Slice Implementation

- [ ] Implement each slice with its own query/mutation hooks; UI components never import the API
      client.
- [ ] Implement UR-001~UR-072 client mirrors as defensive prediction only — a contradicting server
      response always wins.
- [ ] Make Step 11 tests pass, refactor, run the security review, mark Steps 11~12.

### Step 13 - E2E, Accessibility, Capacity and Supply-Chain Gates

- [ ] Add Playwright desktop and 320px journeys against a real U1 per the Q3 answer, covering
      CP-02~CP-05: create, schedule by drag, schedule by keyboard, conflict resolve, unschedule,
      complete, list filtering, and backend-restart error feedback.
- [ ] Add the `@axe-core/playwright` scans (serious/critical fail the build), the 250KB gzip bundle
      ceiling as a build gate, the `test:capacity` 1,000-task render measurement, `npm audit` and the
      CycloneDX SBOM.
- [ ] Run `npm run verify` and `npm run test:e2e`; resolve every failure without lowering a threshold.
- [ ] Run the full SECURITY-01~15 checklist for U2, mark Step 13.

### Step 14 - Documentation and U2 Evidence

- [ ] Create `frontend/README.md` covering install, dev with and without `VITE_USE_MOCK`, contract
      regeneration, the verify/e2e/capacity commands and the U1 dependency.
- [ ] Create `aidlc-docs/construction/u2-frontend-planning-experience/code/code-summary.md`,
      `test-summary.md`, `traceability.md` and `security-review.md`.
- [ ] Verify no hand-written transport types, no duplicate files, no unchecked plan item and no
      unresolved applicable checklist item; mark Step 14.

## Story Coverage

| Story | Steps |
|---|---|
| US-001 이번 주 계획 확인 | 3, 4, 7, 8, 11, 12, 13 |
| US-002 할 일 빠르게 기록 | 5, 6, 11, 12, 13 |
| US-003 할 일 내용 유지보수 | 7, 8, 11, 12, 13 |
| US-004 할 일을 실제 시간에 배치 | 3, 4, 11, 12, 13 |
| US-005 충돌을 이해하고 해결 | 7, 8, 11, 12, 13 |
| US-006 계획에서 다시 빼기 | 11, 12, 13 |
| US-007 실행 완료 기록 | 7, 8, 11, 12, 13 |
| US-008 목록으로 누락 점검 | 11, 12, 13 |
| US-009 어느 기기·입력 방식에서도 계획 | 9, 10, 11, 12, 13 |
| US-010 재시작 후에도 계획 신뢰 (supporting) | 5, 6, 9, 10, 13 |

## Completion Gate

- Every step above is [x].
- `npm run verify` and `npm run test:e2e` pass without a lowered threshold.
- The generated contract file is committed and its regeneration check is clean.
- Applicable SECURITY-01~15 items and the PBT-02/03/07/08/09 obligations are satisfied.
