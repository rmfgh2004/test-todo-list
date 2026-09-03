# Performance Test Instructions

## Backend capacity and latency

From `backend/`:

```bash
./mvnw -Pcapacity verify
```

This includes `CapacityAndLatencyTest`: a warmed 1,000-task weekly-read p95 check against 300ms,
10,000-task bounded-result behavior, and readiness. The profile continues to run the ordinary U1
gate and excludes only restore fixtures.

## Frontend capacity and interaction

From `frontend/`:

```bash
npm run test:capacity
npm run test:e2e
```

The capacity fixture renders and rerenders 1,000 tasks against a 300ms ceiling. Browser E2E also
asserts a single initial weekly request and completion of the measured primary interaction within
two seconds on desktop and 320px projects.

Run these checks on an otherwise idle local machine. A failure must be investigated; do not widen a
threshold solely to accommodate an overloaded runner.
