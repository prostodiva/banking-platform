# 03 Non-functional Requirements

Cross-cutting requirements, written once and extended only when a slice
introduces a genuinely new concern. Each NFR names its enforcement mechanism —
an NFR nothing enforces is a wish, not a requirement.

**NFR-01 — Monetary exactness.** Amounts use `BigDecimal` + `numeric(19,4)`;
binary floating point is banned for money. Enforced by: `Money` value object
(sole representation of amounts), scale check in its constructor.

**NFR-02 — No lost updates.** Concurrent modifications of the same aggregate
must fail loudly, never silently overwrite. Enforced by: `@Version` optimistic
locking on every **mutable** aggregate table (ADR-002), and an
`OptimisticLockingFailureException` → 409 mapping so the collision reaches the
caller as a retryable status rather than a 500.

Write-once aggregates are exempt, and `Transfer` is the first: it is inserted and
never updated (ADR-003 decision 4), so there is no second writer for a version
column to detect. A version on an immutable row documents a concurrency story
that does not exist — the same reasoning that removed its `status` field.
Covered by `TransferOptimisticLockingTest`.

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

**NFR-07 — Architecture boundaries hold.** No framework behavior in domain; a
context reaches another only through its published `application.port..`, never
its `domain`, `infrastructure` or `api`. Enforced by: `ArchitectureTest`
(ArchUnit, docs/13 stage 6; port carve-out per ADR-003).

**NFR-08 — No secrets in git.** Credentials arrive via environment variables;
committed files carry only throwaway dev defaults. Enforced by: `.gitignore`
(.env, keys, local configs), env-var placeholders in `application.properties`.

**NFR-09 — Idempotent submission.** An endpoint that moves money must be safe to
retry: the same `Idempotency-Key` replays the original response, performs the
operation once and publishes its event once. Enforced by: unique index on the
key column, replay path in the handler, E2E replay test (ADR-003).

**NFR-10 — Atomicity across aggregates.** An operation spanning more than one
aggregate commits completely or not at all; no partial application is
observable. Enforced by: one `@Transactional` handler per use case, ordered lock
acquisition to avoid deadlock, E2E rollback test (ADR-003).

## Planned (added with the slice that introduces them)

- Rate limiting, caching (Redis), observability/metrics (per docs/00 goals)
- Authorization
