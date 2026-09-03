# Contract Test Instructions

The source contract is `backend/openapi/planning-api.yaml`.

- From `backend/`, `./mvnw verify` runs `OpenApiContractDriftTest` and confirms the documented routes
  and served contract bytes match the application.
- From `frontend/`, `npm run contract:check` regenerates the TypeScript declaration in memory and
  fails when it differs from `src/shared/api/generated/planning-api.d.ts`.
- After an approved contract change, run `npm run contract:generate`, review the generated diff, and
  then rerun both complete verification gates.

Do not hand-edit or wrap the generated wire schema with independently maintained DTO definitions.
