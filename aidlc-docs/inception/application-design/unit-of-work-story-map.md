# Unit of Work Story Map

## Story Responsibility Map

| Story | User Outcome | U1 Backend Planning Core | U2 Frontend Planning Experience | Integration Gate |
|---|---|---|---|---|
| US-001 | 이번 주 계획 확인 | Supporting: weekly/backlog query | Primary: grid, backlog, navigation states | CP-02/CP-04 E2E |
| US-002 | 할 일 빠르게 기록 | Primary: validation, create, audit | Primary: form, feedback, cache update | CP-02 E2E |
| US-003 | 할 일 내용 유지보수 | Primary: update/delete transaction | Primary: editor, confirm, rollback | CP-02 E2E |
| US-004 | 할 일을 실제 시간에 배치 | Primary: time validation and save | Primary: drag/direct input and preview | CP-03 E2E |
| US-005 | 충돌을 이해하고 해결 | Primary: overlap, 409, transaction | Primary: comparison, choice, focus | CP-03 E2E |
| US-006 | 계획에서 다시 빼기 | Primary: unschedule and field preservation | Primary: command and backlog return | CP-03 E2E |
| US-007 | 실행 완료 기록 | Primary: idempotent state and audit | Primary: control and cache sync | CP-04 E2E |
| US-008 | 목록으로 누락 점검 | Supporting: filter/page/sort query | Primary: list, URL filter and empty state | CP-04 E2E |
| US-009 | 어느 기기·입력 방식에서도 계획 | Supporting: semantic safe API states | Primary: responsive, keyboard, aria-live | CP-05 mobile/a11y |
| US-010 | 재시작 후에도 계획 신뢰 | Primary: encrypted persistence, safe error, audit | Supporting: error/request ID feedback | CP-05 security/restart |

`Primary` may appear in both units when a story is an end-to-end vertical slice. It means each unit
owns a distinct necessary outcome, not duplicate business logic.

## Functional Requirement Map

| Requirement | U1 Responsibility | U2 Responsibility |
|---|---|---|
| FR-001 | weekly range projection | weekly/day timetable presentation |
| FR-002 | week-scoped query contract | previous/next/today and URL state |
| FR-003 | create validation, persistence, audit | accessible create form and feedback |
| FR-004 | update/delete transaction and 404 | editor, confirmation and rollback |
| FR-005 | unscheduled query and stable fields | backlog ordering/cards/states |
| FR-006 | authoritative schedule validation/save | drag and direct-input proposal |
| FR-007 | overlap, candidate, 409 without mutation | explicit keep/move/cancel dialog |
| FR-008 | idempotent unschedule preserving content | timeline removal and backlog return |
| FR-009 | idempotent completion and audit | completion control and cross-view sync |
| FR-010 | bounded filter/page/sort API | list table, URL query and empty states |
| FR-011 | safe semantic states for consumers | desktop/mobile/keyboard complete UX |
| FR-012 | encrypted H2 file and memory profile | persistent-state feedback only |
| FR-013 | append-only audit in each mutation | display safe operation failure only |

## Non-Functional Requirement Map

| Requirement | U1 Primary Work | U2 Primary Work | Shared Gate |
|---|---|---|---|
| NFR-001 | JUnit/MockMvc/H2 coverage | Vitest/RTL/Playwright coverage | root test report |
| NFR-002 | jqwik time/conflict properties | fast-check transport/geometry properties | seed logging |
| NFR-003 | encryption, validation, headers, CORS, rate limit, safe error | no unsafe HTML/eval, typed validation, CSP-compatible UI | security checklist |
| NFR-004 | semantic error/data contracts | WCAG, keyboard, focus, aria-live, mobile | Playwright/a11y |
| NFR-005 | bounded indexed queries and p95 target | render/query efficiency and stable layout | performance checks |
| NFR-006 | domain invariant, optimistic lock, transaction rollback | cache snapshot and UI rollback | conflict E2E |
| NFR-007 | architecture tests, OpenAPI owner | strict TS, generated client and lint | contract diff |
| NFR-008 | structured request-ID log, health, safe global errors | error boundary, retry and request ID display | fault-path E2E |

## Security Baseline Assignment

| Rules | Owner | Supporting/Reason |
|---|---|---|
| SECURITY-01 | U1 | encrypted H2 and external key; embedded transport N/A |
| SECURITY-02 | N/A | no load balancer, gateway or CDN |
| SECURITY-03 | U1 | U2 propagates/displays request ID without sensitive data |
| SECURITY-04 | U1/U2 | response/dev-server headers and CSP-compatible frontend |
| SECURITY-05 | U1 primary | U2 provides defense-in-depth client validation |
| SECURITY-06 | N/A | no IAM |
| SECURITY-07 | U1 | loopback binding/CORS; cloud network N/A |
| SECURITY-08 | U1 | routes explicitly public/local; authentication/object auth N/A |
| SECURITY-09 | U1/U2 | safe errors, no console/default pages, supported runtimes |
| SECURITY-10 | U1/U2/root | exact dependencies, audits and SBOM |
| SECURITY-11 | U1 primary | U2 makes abuse/conflict outcomes explicit |
| SECURITY-12 | N/A | no user authentication; U1 externalizes DB key |
| SECURITY-13 | U1 primary | U2 generated contract and no external scripts |
| SECURITY-14 | N/A | no cloud alerting; U1 request-ID logging remains applicable |
| SECURITY-15 | U1/U2 | transaction and cache rollback, global boundaries |

## PBT Partial Assignment

| Rule | U1 | U2 |
|---|---|---|
| PBT-02 | date/time transport round trip with jqwik | generated contract parse/format with fast-check |
| PBT-03 | overlap/end/15-minute invariants | grid geometry/range invariants |
| PBT-07 | reusable Task/ScheduleSlot generators | reusable transport/geometry generators |
| PBT-08 | jqwik shrinking and seed output | fast-check shrinking and seed output |
| PBT-09 | jqwik dependency and JUnit integration | fast-check dependency and Vitest integration |

## Coverage Check

- Stories assigned: US-001 through US-010 (10/10).
- Functional requirements assigned: FR-001 through FR-013 (13/13).
- Non-functional requirements assigned: NFR-001 through NFR-008 (8/8).
- Security/PBT blocking responsibilities assigned or explicitly N/A.

