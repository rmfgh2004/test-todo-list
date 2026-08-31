# U1 NFR Requirements Plan

## Progress

- [x] U1 functional design analyzed
- [x] Eight NFR categories evaluated
- [x] Default-A answers and rationale recorded
- [x] Answers validated without ambiguity
- [x] `nfr-requirements.md` generated
- [x] `tech-stack-decisions.md` generated
- [x] Security/PBT applicability and stable IDs validated

## Decisions

| Category | Selected A | Rationale |
|---|---|---|
| Scalability | single local user, 10,000 tasks, bounded week/page queries | realistic local capacity without distributed complexity |
| Performance | p95 <=300ms at 1,000 tasks, startup health <=5s | measurable and aligned with approved NFR-005 |
| Availability | reliable while local process runs; backup/restore, no HA | cloud resiliency is explicitly disabled/out of scope |
| Security | full applicable Security Baseline, encrypted file DB, loopback-only public API | user mandated blocking security review |
| Tech stack | Java 17, Spring Boot 4.1.1, Maven, H2 2.4.240, Flyway | approved, supported stack and local environment |
| Reliability | atomic transactions, optimistic locking, safe global errors, health | protects schedule consistency and recoverability |
| Maintainability | architecture tests, OpenAPI, >=80% line/branch, jqwik | stable ID and test policy compliance |
| API usability | consistent safe errors, field errors, 409 conflict and request ID | frontend can recover and explain outcomes accessibly |
