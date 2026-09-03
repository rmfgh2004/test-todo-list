# U2 Frontend Planning Experience Test Summary

- **Verified at**: 2026-09-02 20:29:33 (KST)
- **Unit/component/contract-mock**: 21 files, 101 tests passed
- **Coverage**: statements 87.31%, branches 82.60%, functions 83.56%, lines 89.45%
- **Real-U1 E2E**: 10/10 passed across desktop and 320x800 projects
- **Captioned A-Z journey**: 1/1 desktop test passed in 28.5s; one 901,015-byte WebM covers create,
  edit, priority triple encoding, live capacity/slot preview, pointer/keyboard scheduling, structured
  conflict rollback, due-date groups, completion/filtering, unscheduling and deletion
- **Video evidence**: `frontend/test-results/` contains 28 Git-ignored WebM files (988KB total) from
  the latest passing run; Playwright records every run with `video: 'on'`
- **Accessibility**: primary week/list states and editor/delete/conflict/scheduling surfaces scanned;
  zero serious or critical axe findings
- **Performance**: one weekly request per initial view; operable under 2 seconds in both browser
  projects; 1,000-task rerender passed in 120ms against a 300ms ceiling
- **Bundle**: 99.1KB gzip / 250KB allowed
- **Contract**: generated OpenAPI declaration drift check passed
- **Supply chain**: `npm audit --audit-level=high` found 0 vulnerabilities; CycloneDX 1.6 SBOM
  generated with 454 components and 496 dependency relationships

The E2E journeys cover create/delete, pointer scheduling, keyboard arrow scheduling, unscheduling,
real conflict rollback and candidate acceptance, completion, URL filtering and simulated transport
loss followed by recovery against the still-running real U1. PBT uses fast-check seed `20260901` and
keeps shrinking enabled for PBT-02, PBT-03, PBT-07, PBT-08 and PBT-09.

No threshold was lowered while resolving failures. Step 13 first exposed a lint failure in the E2E
type import, missing keyboard arrow movement/focus containment, and a mobile assertion that scheduled
outside the visible single-day view; code and journey scope were corrected and all gates rerun.
