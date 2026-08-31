# U2 Frontend Planning Experience NFR Requirements Plan

Artifacts are generated only after every question below is answered and every ambiguity is resolved.

## Steps

- [x] Read the approved U2 functional design artifacts
- [x] Read the approved NFR-001~NFR-008 and the Security Baseline / PBT assignments for U2
- [x] Read the fixed stack in `aidlc-inputs/01-tech-stack-decisions.md`
- [x] Identify what the approved documents leave undecided for U2
- [x] Collect answers to the questions below
- [x] Resolve any follow-up ambiguity
- [x] Generate `nfr-requirements.md` and `tech-stack-decisions.md`

## Already Fixed — Not Re-Opened

From `aidlc-inputs/01-tech-stack-decisions.md` and the approved requirements:

- React 19.2 + Vite 8.2, TypeScript 5 strict, TanStack Query, CSS Modules + design tokens,
  Lucide icons, dnd-kit.
- Vitest 4.1, React Testing Library, Playwright 1.62, fast-check.
- Coverage floors (NFR-001): statements/functions/lines 80%, branches 75%.
- Performance targets (NFR-005): first screen interactive within 2s on the documented development
  machine; backend p95 300ms is U1's obligation, not U2's.
- Accessibility target (NFR-004): WCAG 2.2 AA, keyboard-operable drag alternative, ARIA-live status,
  no colour-only encoding.
- Security assignments for U2 (story map): SECURITY-04, 05, 09, 10, 13, 15 supporting or shared;
  authentication, IAM and cloud rules are N/A.
- PBT assignments for U2: PBT-02, PBT-03, PBT-07, PBT-08, PBT-09 with fast-check.
- No external CDN, no runtime script from a third-party host.

## Undecided — Questions

### Q1 — How are the OpenAPI client types produced?

The functional design requires generated transport types with a build-failing drift check, but no
approved document names the generator.

[Answer]: **`openapi-typescript`.** It emits type declarations only — no runtime dependency and one
devDependency. The fetch wrapper and the TanStack Query hooks are hand-written because the functional
design already fixes their contract (snapshot rollback, conflict transitions, request-ID generation),
which a hook generator would fight. The generated file is committed; CI regenerates it and fails on a
diff, satisfying the NFR-007 drift requirement.

### Q2 — What does the E2E suite run against?

`unit-of-work.md` requires Playwright journeys "against real U1", while unit and component tests need
a deterministic transport.

[Answer]: **Hybrid.** Unit and component tests use MSW handlers typed from the generated contract
types, so a contract change breaks compilation rather than silently passing. Playwright E2E starts a
real U1 process on the in-memory H2 profile and runs the desktop and 320px journeys against it. This
meets the `unit-of-work.md` completion criterion ("against real U1") without requiring a JVM for every
unit test run.

### Q3 — Is accessibility a blocking automated gate?

NFR-004 sets WCAG 2.2 AA as a target but does not say how it is enforced.

[Answer]: **Blocking.** `axe-core` scans run on every primary screen and every dialog; any serious or
critical violation fails the build. Playwright additionally asserts the keyboard scheduling path,
focus trap and restore, and the ARIA-live announcements. Contrast and reading-order checks that axe
cannot decide stay on the manual checklist.

### Q4 — Is there a bundle/performance budget enforced by the build?

NFR-005 states a 2-second interactive target but defines no measurable build-time budget.

[Answer]: **Bundle ceiling enforced, render time measured.** The initial JS bundle is capped at
250KB gzip and the build fails above it. Weekly-grid render time with a 1,000-task fixture is measured
by Playwright and recorded, but it runs under a separate profile — mirroring U1's `-Pcapacity` — so
the default build stays fast.
