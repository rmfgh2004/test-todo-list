# U1 NFR Design Plan

## Progress

- [x] Approved U1 NFR requirements and functional design analyzed
- [x] Resilience patterns evaluated
- [x] Scalability and performance patterns evaluated
- [x] Security patterns evaluated
- [x] Logical/infrastructure component applicability evaluated
- [x] Default-A answers validated without ambiguity
- [x] `nfr-design-patterns.md` generated
- [x] `logical-components.md` generated
- [x] NFR, Security and PBT traceability validated

## Default-A Design Answers

| Category | Selected A | Rationale |
|---|---|---|
| Resilience | atomic local transactions, optimistic locking, fail-fast validation, no automatic command retry | retries could duplicate user intent; no remote dependency needs circuit breaking |
| Scalability | bounded indexed H2 queries and pagination, no distributed scaling | approved single-user/10,000-task capacity fits one process |
| Performance | query projections, composite indexes, no general cache, controlled performance fixture | avoids stale schedule state while meeting the p95 target |
| Security | ordered servlet filters, explicit public routes, loopback CORS, safe DTO/error boundaries, encrypted file profile | directly implements the blocking Security Baseline |
| Logical components | domain/application ports plus web, persistence and platform adapters; no queue, broker or distributed cache | preserves inward dependencies without unused infrastructure |

## Applicability Decisions

- Circuit breaker, remote retry, queue, broker, distributed cache, service discovery and load balancer:
  not applicable because U1 has no remote dependency or multi-instance topology.
- Database-operation retry: not applied to mutations; stale or failed commands return a typed response
  and require an explicit user retry.
- Read cache: not applied initially; indexed bounded H2 queries are the measured baseline and schedule
  correctness takes precedence over speculative caching.
- Infrastructure Design remains skipped because all selected elements are in-process libraries or the
  embedded H2 database.
