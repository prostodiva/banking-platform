# 03 Non-functional Requirements

Cross-cutting requirements, written once and extended only when a slice
introduces a genuinely new concern. Each NFR names its enforcement mechanism —
an NFR nothing enforces is a wish, not a requirement.

**NFR-01 — Monetary exactness.** Amounts use `BigDecimal` + `numeric(19,4)`;
binary floating point is banned for money. Enforced by: `Money` value object
(sole representation of amounts), scale check in its constructor.

**NFR-02 — No lost updates.** Concurrent modifications of the same aggregate
must fail loudly, never silently overwrite. Enforced by: `@Version` optimistic
locking on every aggregate table (ADR-002).

**NFR-03 — Auditable schema history.** Schema changes only via versioned Flyway
migrations; applied migrations are immutable; financial records are never
deleted (CLOSED is a terminal status, not a DELETE). Enforced by: Flyway
checksums, code review, `ddl-auto=validate`.

**NFR-04 — Fail fast on drift.** Mapping/schema disagreement aborts startup
rather than surfacing at runtime. Enforced by: `spring.jpa.hibernate.ddl-auto=validate`.

**NFR-05 — Standard error contract.** All API errors are RFC 9457
`ProblemDetail`; stack traces never reach clients. Enforced by:
`GlobalExceptionHandler` (shared/web).

**NFR-06 — Tests prove production behavior.** Business rules are unit-tested
without Spring; integration tests run against real Postgres (Testcontainers);
H2 is banned. Enforced by: test conventions (docs/11), `TestcontainersConfiguration`.

**NFR-07 — Architecture boundaries hold.** No framework behavior in domain, no
cross-slice coupling. Enforced by: `ArchitectureTest` (ArchUnit, docs/13 stage 6).

**NFR-08 — No secrets in git.** Credentials arrive via environment variables;
committed files carry only throwaway dev defaults. Enforced by: `.gitignore`
(.env, keys, local configs), env-var placeholders in `application.properties`.

## Planned (added with the slice that introduces them)

- Idempotency of payment submission (payments slice — will need an ADR)
- Rate limiting, caching (Redis), observability/metrics (per docs/00 goals)
