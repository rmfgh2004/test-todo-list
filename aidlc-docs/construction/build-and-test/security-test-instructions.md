# Security Test Instructions

## Backend

From `backend/`:

```bash
./mvnw verify
./mvnw -Prestore verify
./mvnw -Psecurity-scan verify
```

The standard gate covers headers, loopback/CORS boundaries, error redaction, request IDs, rate
limits, encrypted-datasource guards and dependency SBOM generation. The restore profile performs an
encrypted stop-copy-start recovery and verifies that a task title is absent from raw database bytes.
OWASP Dependency-Check rejects dependencies scoring CVSS 7 or higher. Its first run needs vulnerability
database access and may be substantially faster with an NVD API key supplied through the standard
Dependency-Check configuration; never commit the key.

## Frontend

From `frontend/`:

```bash
npm run audit:deps
npm run sbom
npm run verify
```

The audit gate rejects high-severity npm advisories. CycloneDX output is `sbom.json`, which is local
evidence and Git-ignored. Lint and build gates reject unsafe HTML/evaluation patterns and prevent MSW
mock assets from entering production output.
