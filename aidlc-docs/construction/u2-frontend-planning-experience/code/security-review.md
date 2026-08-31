# U2 Frontend Planning Experience Security Review

## Code Generation Steps 3~4 — Pure Time and Grid Core

- **Reviewed at**: 2026-09-01 01:20:50 (KST)
- **Changed application files**: `frontend/src/shared/time/calendar.ts`,
  `frontend/src/shared/grid/geometry.ts`, `frontend/src/shared/grid/capacity.ts`
- **Test support**: three test files and `frontend/tests/generators/planning.ts`
- **Stable IDs**: FR-001, FR-002, FR-006, NFR-002, UR-010~014, UR-023~024,
  PBT-02, PBT-03, PBT-07, PBT-08, PBT-09
- **Verdict**: PASS — no blocking finding

### Security Baseline

| Rule | Result | Evidence or N/A rationale |
|---|---|---|
| SECURITY-01 | N/A | No persistence or stored secret was added. |
| SECURITY-02 | N/A | No network intermediary exists in this batch. |
| SECURITY-03 | N/A | Production logging was not added; seed output exists only in tests. |
| SECURITY-04 | N/A | No HTML-serving endpoint or header configuration changed. |
| SECURITY-05 | PASS | ISO date/time, estimate, drop and grid inputs have strict type, format and range checks. |
| SECURITY-06 | N/A | No IAM or permission policy exists. |
| SECURITY-07 | N/A | No network configuration changed. |
| SECURITY-08 | N/A | No endpoint or authorization surface changed. |
| SECURITY-09 | PASS | Pure modules contain no HTML injection sink, dynamic execution, debug page or mock transport. |
| SECURITY-10 | PASS | No dependency was added or changed; the existing lockfile and audit gate remain intact. |
| SECURITY-11 | PASS | Helpers are display-only and do not bypass U1 scheduling authority. |
| SECURITY-12 | PASS | No credential, token, environment secret or persistent authority data was introduced. |
| SECURITY-13 | PASS | `TaskView` and `ScheduleView` are aliases of generated OpenAPI types, never manual duplicates. |
| SECURITY-14 | N/A | No monitoring or alerting surface changed. |
| SECURITY-15 | PASS | Invalid values fail closed with explicit errors; there is no external resource to leak or retain. |

### Code Checklist

- Input/output: PASS — strict allowlists and boundary tests cover malformed dates, time alignment,
  08:00~22:00 limits and 15~840-minute estimates; no user HTML or raw error rendering exists.
- Access/data/web: N/A — no API, persistence, credential, browser storage, HTML sink or external
  origin was added.
- Exceptions: PASS — pure validation failures are explicit and contain no internal path or stack
  material intended for the UI.
- Supply chain: PASS — no new package; `npm run verify` and contract drift pass.
- Test evidence: PASS — RED was observed for three missing modules and separately for two
  out-of-window drop cases, then 20 focused tests and the 21-test full suite passed. Coverage is
  96.29% lines and 88.76% branches.

### PBT Compliance

| Rule | Result | Evidence |
|---|---|---|
| PBT-02 | PASS | ISO date and HH:mm parse/format round trips run over generated valid values. |
| PBT-03 | PASS | Week continuity, grid range, alignment and slot-span invariants are generated. |
| PBT-07 | PASS | Reusable constrained `TaskView`, `ScheduleView`, ISO date and drop generators exist. |
| PBT-08 | PASS | fast-check shrinking remains enabled; fixed seed `20260901` is printed on every property run. No counterexample occurred. |
| PBT-09 | PASS | fast-check runs inside the standard Vitest and `npm run verify` lifecycle. |

PBT-01, PBT-04~06 and PBT-10 are advisory under the approved partial mode.
