# U1 Backend Planning Core Tech Stack Decisions

## 1. Runtime and Build

| Area | Decision | Rationale and constraint |
|---|---|---|
| Language | Java 17 | Approved project baseline and supported LTS runtime; compilation uses release 17. |
| Framework | Spring Boot 4.1.1 with Spring MVC | Current approved backend baseline; synchronous local CRUD does not need a reactive stack. |
| Build | Maven Wrapper and Spring Boot dependency management | Reproducible local/CI command and centralized compatible versions. |
| Packaging | Executable JAR | One local process with no servlet container deployment dependency. |
| Time | `LocalDate`, `LocalTime`, `Instant`, explicit `Asia/Seoul` policy | Separates planning-wall time from audit instants and implements NFR-006. |

## 2. Application and Persistence

| Area | Decision | Rationale and constraint |
|---|---|---|
| API | Spring Web MVC, Jakarta Validation, Jackson | Typed JSON boundary, declarative constraints and safe centralized errors. |
| Persistence | Spring Data JPA with adapter-owned entities | Parameter binding and optimistic locking while keeping the domain free of JPA. |
| Database | H2 2.4.240, in-memory for tests and AES-encrypted file mode for development/runtime | Satisfies the requested dual H2 modes and local-only deployment. |
| Migration | Flyway | Repeatable schema startup validation; no ORM auto-update outside isolated tests. |
| Transactions | Spring transaction boundary in application services | Task mutation and append-only audit commit or roll back together. |
| API contract | Generated OpenAPI from annotated controllers plus checked-in normalized contract | Makes constraints/errors discoverable and enables frontend drift detection. |

Production-like file mode uses an environment-supplied composite H2 password and a configured
`CIPHER=AES` URL. Repository configuration contains placeholders only. H2 console and remote TCP mode
remain disabled. In-memory tests receive isolated database names and do not reuse file data.

## 3. Security and Operations

| Area | Decision | Rationale and constraint |
|---|---|---|
| Web security | Spring Security filter chain with explicit public routes, loopback CORS and security headers | Authentication is out of scope, but secure defaults and route intent remain testable. |
| Rate limiting | In-process bounded token-bucket component at the HTTP boundary | The app is single-node/local; no distributed store is justified. Clock and policy are injectable for deterministic tests. |
| Request correlation | Servlet filter validates or generates an opaque request ID and returns it in every response | Implements NFR-008 without accepting unbounded/untrusted header data. |
| Errors | `@RestControllerAdvice` maps typed failures to a fixed safe schema | Prevents stack, SQL, path and framework disclosure. |
| Health | Spring Boot Actuator with only constrained health/liveness/readiness exposure | Provides local readiness without exposing environment/configuration endpoints. |
| Logging | SLF4J/Logback structured JSON output | Required fields are machine-readable; code logs identifiers and safe summaries only. |
| Supply chain | OWASP Dependency-Check and CycloneDX Maven plugin in verification/CI | Blocking vulnerability evidence and an SBOM for SECURITY-10. |

The rate limiter must not use task text as a key. Configuration chooses conservative defaults and a
bounded cache; tests cover exhaustion, refill, `429`, `Retry-After`, and failure behavior.

## 4. Test and Quality Tooling

| Area | Decision | Purpose |
|---|---|---|
| Unit/API/integration | JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc | Domain, application, HTTP and isolated H2 coverage. |
| Property tests | jqwik integrated into Maven test | PBT-02, PBT-03, PBT-07, PBT-08 and PBT-09. |
| Architecture | ArchUnit | Enforces domain independence and adapter direction. |
| Coverage | JaCoCo Maven verification | 80% line/branch overall and 90% collision-domain branch gates. |
| Formatting | Spotless | Deterministic Java and build-file formatting. |
| API drift | Normalized OpenAPI artifact comparison | Detects backend/frontend contract mismatch. |

All test and plugin versions are resolved explicitly by the Maven model or compatible BOM and recorded
in the effective build. New libraries require official-source, maintenance, license and vulnerability
review before addition. A library is not added when the JDK or existing Spring stack provides a small,
testable implementation.

## 5. Rejected Alternatives

| Alternative | Reason not selected |
|---|---|
| PostgreSQL/MySQL | Adds an external service contrary to the requested local H2 scope. |
| WebFlux/R2DBC | Reactive complexity provides no benefit for one local user and a blocking H2/JPA store. |
| Distributed cache/rate limiter | No multi-instance deployment or shared identity exists. |
| Hibernate schema auto-update | Weak migration reproducibility and unsafe file-mode evolution. |
| Lombok in the domain | Avoids hidden generated behavior in invariant-heavy models and reduces supply-chain surface. |
| Cloud observability/HA stack | Operations and resiliency extensions are explicitly out of scope. |

## 6. Implementation Guardrails

- Package by domain capability, with `domain`, `application`, `adapter.in.web`, and
  `adapter.out.persistence` dependencies pointing inward.
- Pin public API DTOs; never serialize JPA entities or exceptions.
- Use repository/query APIs with bound parameters and bounded result types.
- Keep secrets out of defaults, tests, logs, OpenAPI examples and Git.
- Run `./mvnw verify` as the single backend quality gate and append the mandatory security review after
  every code-change batch.
