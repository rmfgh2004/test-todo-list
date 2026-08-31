# U2 Business Rules

U2 rules are **presentation and interaction rules**, prefixed `UR-`. They never replace a U1 `BR-`
rule. Where a `UR-` rule mirrors a server rule it is defence in depth: the client may reject early,
but only the server decides. A client check that disagrees with the server always loses.

## Rule Precedence

1. A server response overrides any local prediction, optimistic value or preview.
2. A client validation may only **narrow** what the server accepts, never widen it.
3. A rule that would require a change to the approved U1 contract is out of scope for U2.

## 1. Client Validation Rules (mirror of BR-001~BR-005, defence in depth)

| ID | Requirement | Rule | On violation |
|---|---|---|---|
| UR-001 | FR-003 | `title` trimmed, nonblank, 1~120 characters | inline field error, submit blocked |
| UR-002 | FR-003 | `description` optional, at most 2,000 characters | inline field error + live counter |
| UR-003 | FR-003 | `priority` selected from the three enum values only | no free-text input exists |
| UR-004 | FR-003 | `estimateMinutes` 15~840 and a multiple of 15 | stepper restricted to valid values |
| UR-005 | FR-003 | `dueDate` optional ISO local date | date input, inline error on a bad value |
| UR-006 | FR-004 | edit submits the **full** content set; a cleared `description`/`dueDate` sends `null` | matches the PATCH replacement contract |
| UR-007 | NFR-003 | user text renders as text only — no `innerHTML`, no `dangerouslySetInnerHTML`, no `eval` | lint rule fails the build |
| UR-008 | FR-004 | server `fieldErrors` map onto the same fields as the local rules and focus the first invalid field | server error wins over local state |

## 2. Time Geometry Rules (display mirror of BR-010~BR-013)

| ID | Requirement | Rule | On violation |
|---|---|---|---|
| UR-010 | FR-001 | the grid renders exactly 08:00~22:00 on a 15-minute pitch: 56 slots per day | invariant, PBT-covered |
| UR-011 | FR-001 | a block's span equals `estimateMinutes / 15` slots; end is derived, never entered | invariant |
| UR-012 | FR-006 | a drop snaps to the nearest 00/15/30/45 boundary | snapped preview |
| UR-013 | FR-006 | a proposal whose derived end exceeds 22:00 is rejected before the request | preview marked invalid, no request sent |
| UR-014 | FR-002 | `weekStart` must be a Monday; a non-Monday URL value is normalized once to that week's Monday **and the URL is rewritten** | never sent unnormalized to the API |
| UR-015 | FR-001 | all rendering uses Asia/Seoul wall time; no UTC conversion and no host-timezone arithmetic | PBT round-trip |
| UR-016 | FR-001 | today, weekend, priority, conflict and completion each carry a non-colour cue (icon, text or border) | accessibility test fails |

## 3. Backlog and Capacity Rules

| ID | Requirement | Rule | On violation |
|---|---|---|---|
| UR-020 | FR-005 | the backlog renders exactly `WeeklyPlanView.backlog`; membership is never computed locally | render invariant |
| UR-021 | FR-005 | the displayed order is the server order; the local sort key is only a tie-break | server order wins |
| UR-022 | FR-005 | each card shows title, priority, estimate and due date; an absent due date shows an empty state, not blank | render invariant |
| UR-023 | Q3 | header capacity = `5880 - Σ estimateMinutes of incomplete scheduled tasks in the week`, floored at 0 | derived from the same payload as the grid |
| UR-024 | Q3 | capacity is display-only and authorises nothing | no code path reads it before a mutation |

## 4. Scheduling Interaction Rules

| ID | Requirement | Rule | On violation |
|---|---|---|---|
| UR-030 | FR-006 | every drag has an equivalent keyboard path (Space to pick up, arrows to move, Enter to confirm, Esc to abort) and a form path (date + start time) | accessibility test fails |
| UR-031 | FR-006 | a proposal always sends `{date, startTime, expectedVersion}`; the client never sends an end time | contract invariant |
| UR-032 | NFR-004 | the optimistic placement is applied only after a snapshot is captured | rollback would be impossible otherwise |
| UR-033 | FR-006 | one scheduling mutation is in flight per task at a time; a second interaction on the same task is ignored until settled | drag disabled while saving |
| UR-034 | FR-008 | unschedule keeps all content and returns the task to the backlog from the server response, not from local memory | render invariant |
| UR-035 | NFR-004 | every state transition (proposing → saving → scheduled / conflict / failed) announces one concise ARIA-live message | accessibility test fails |

## 5. Conflict Rules (Q2 decision)

| ID | Requirement | Rule | On violation |
|---|---|---|---|
| UR-040 | FR-007 | a 409 `SCHEDULE_CONFLICT` rolls the optimistic placement back **before** the dialog opens | the user never sees a placement the server rejected |
| UR-041 | FR-007 | the dialog offers exactly three actions: keep existing, move to `nextCandidate`, cancel | no "겹쳐 두기", not even disabled |
| UR-042 | FR-007 | when `nextCandidate` is absent, the move action is not rendered and the dialog says no free slot remains this week | no fabricated candidate |
| UR-043 | FR-007 | accepting a candidate sends a **new** full schedule request (`resolutionMode: ACCEPT_CANDIDATE`) and is re-validated server-side; a second conflict reopens the dialog with the new payload | never treated as guaranteed success |
| UR-044 | FR-011 | the dialog traps focus, closes on Esc, and returns focus to the originating card | accessibility test fails |
| UR-045 | FR-007 | the dialog shows times only; it never shows the other task's title or content | the contract does not send it |

## 6. Concurrency and Version Rules

| ID | Requirement | Rule | On violation |
|---|---|---|---|
| UR-050 | NFR-006 | every mutation echoes the `version` from the last server payload as `expectedVersion` | never incremented or guessed locally |
| UR-051 | NFR-006 | a 409 `STALE_TASK` refetches the affected queries, discards the local edit and tells the user the item changed elsewhere | no silent overwrite |
| UR-052 | NFR-004 | any failed optimistic update restores the captured snapshot exactly | cache rollback test |
| UR-053 | FR-004 | delete requires an explicit confirmation step and sends `confirmed=true` with `expectedVersion` | no optimistic delete |

## 7. Failure and Feedback Rules

| ID | Requirement | Rule | On violation |
|---|---|---|---|
| UR-060 | NFR-008 | every failure surface shows the authored message, a copyable request ID and a retry affordance where retry is meaningful | feedback test fails |
| UR-061 | Q4 | a 429 rolls back immediately, announces the failure and offers manual retry disabled until `Retry-After` elapses | no pending-save queue exists |
| UR-062 | NFR-008 | mutations are never retried automatically; only idempotent GET queries retry, at most twice with backoff | no duplicate command |
| UR-063 | NFR-003 | a rendered error uses only the server's `code`, `message`, `requestId` and allowlisted field names | never the raw response body |
| UR-064 | NFR-008 | an unexpected render failure is contained by an error boundary that keeps navigation usable | boundary test fails |
| UR-065 | FR-001 | loading, empty, error and success are four distinct states; a load never renders as an empty result | state test fails |

## 8. Scope Rules (Q1 decision)

| ID | Rule |
|---|---|
| UR-070 | Only FR-001~FR-011 capabilities ship. The kanban board, assignee, tags, subtasks, comments, search, recurrence and natural-language entry are not implemented and not stubbed. |
| UR-071 | The visual language of the design screens is adopted for in-scope features; out-of-scope controls are removed rather than disabled. |
| UR-072 | No U2 change requires a U1 contract change. Any such need stops work and is raised as a contract question. |

## Test Obligation

- Each `UR-` rule has at least one Vitest/RTL test naming the requirement ID (NFR-001).
- UR-010~UR-015 are additionally covered by fast-check properties with logged seeds (PBT-02, PBT-03,
  PBT-07, PBT-08, PBT-09).
- UR-016, UR-030, UR-035, UR-044 are covered by Playwright desktop + 320px mobile runs (NFR-004).
- UR-040~UR-043 and UR-050~UR-052 are covered by E2E against a real U1 instance (NFR-006).
