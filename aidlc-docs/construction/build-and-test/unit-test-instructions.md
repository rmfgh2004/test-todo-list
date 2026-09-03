# Unit Test Instructions

## Backend

From `backend/`, run `./mvnw test` for the fast unit/integration loop or `./mvnw verify` for the
required complete gate. The standard gate excludes only the tagged capacity and restore fixtures.
Reports are written to `target/surefire-reports/`; JaCoCo HTML is written to
`target/site/jacoco/index.html` during `verify`.

Required coverage thresholds are 80% line and branch across the bundle and 90% branch for
`SchedulePolicy`. jqwik property tests use reproducible seeds reported by the test runner.

## Frontend

From `frontend/`, run `npm test` for the component loop or `npm run test:coverage` for the coverage
gate. The latter covers hooks, stores, views, accessibility helpers, API transport, optimistic
rollback, property tests, and generated-contract mocks. Coverage output is under `coverage/`.

The required frontend thresholds are 80% statements, branches, functions, and lines.

Tempo Phase 1 reference result: 108 tests across 22 files; 85.37% statements, 80.58% branches,
81.09% functions and 87.73% lines.
