# Integration Test Instructions

## U1 adapter and persistence integration

Run `./mvnw verify` from `backend/`. Spring MVC, security-filter, JPA/Flyway, request-correlation,
rate-limit, error-contract and persistence rollback tests execute against controlled local test
configuration. No shared database or network service is needed.

## U1/U2 browser integration

Run `npm run test:e2e` from `frontend/`. Playwright starts an isolated U1 on port 18080 and Vite on
port 5180, waits for their health checks, runs one worker, and stops both processes afterward.

The suite exercises real HTTP integration for task creation/deletion, pointer and keyboard
scheduling, unscheduling, conflict rollback/candidate acceptance, completion, filtering, transport
loss and recovery. See `e2e-test-instructions.md` for artifacts and browser projects.

The Tempo Phase 1 run additionally proves dnd-kit pointer drops on bounded 15-minute slots, the
unchanged keyboard interaction, persistent rollback feedback and the presentation-only task groups.
