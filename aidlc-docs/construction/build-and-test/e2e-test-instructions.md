# End-to-End Test Instructions

From `frontend/`:

```bash
npm run test:e2e
npm run test:e2e:journey
```

Playwright automatically starts the real U1 test profile at `127.0.0.1:18080` and U2 at
`127.0.0.1:5180`; do not start either port manually. It runs the same journeys with desktop Chromium
and a 320x800 viewport. Browser installation, if missing on a new machine, is performed once with
`npx playwright install chromium`.

Regression executions record WebM files under Git-ignored `frontend/test-results/regression/`.
Failed tests also
retain a trace and screenshot. Inspect a trace with `npx playwright show-trace <trace.zip>`, or open a
`.webm` file with a local video player. Rerunning the suite replaces the prior artifacts, so copy any
run that must be retained outside `test-results/` before executing it again.

`test:e2e:journey` runs one captioned desktop test from application entry through create, edit,
pointer and keyboard scheduling, conflict resolution, completion, filtering, unscheduling and final
deletion. The Tempo Phase 1 journey also asserts priority triple encoding, live capacity/slot preview,
structured rollback feedback and movement between 오늘/이번 주/완료 groups. Its single video is written
below `frontend/test-results/a-z-journey/`.
The reviewed A-Z `video.webm` is intentionally tracked as the sole exception to the ignored browser
artifact policy; rerun and review it before staging an update.
