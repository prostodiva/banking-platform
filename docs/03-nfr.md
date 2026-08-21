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

A dev *default* is only acceptable where using it in production is obviously
broken (`local-dev-only-change-me` as a DB password fails to connect anywhere
real). **Signing keys get no default at all** — `${JWT_SECRET}` with no `:`
fallback, so a misconfigured deployment fails to start instead of signing tokens
with a value that is public on GitHub. A committed fallback HMAC secret is one of
the most common real-world token-forgery breaches.

**NFR-09 — Idempotent submission.** An endpoint that moves money must be safe to
retry: the same `Idempotency-Key` replays the original response, performs the
operation once and publishes its event once. Enforced by: unique index on the
key column, replay path in the handler, E2E replay test (ADR-003).

**NFR-10 — Atomicity across aggregates.** An operation spanning more than one
aggregate commits completely or not at all; no partial application is
observable. Enforced by: one `@Transactional` handler per use case, ordered lock
acquisition to avoid deadlock, E2E rollback test (ADR-003).

**NFR-11 — Deny by default; identity comes from the token.** Every endpoint
requires authentication unless explicitly whitelisted (`/api/auth/**`, health).
The caller's identity is read from the validated token, **never from the request
body or a path parameter the client chose**. A client cannot open an account for
another `ownerId`, or deposit into an account it does not own, by editing JSON.
Enforced by: `anyRequest().authenticated()` as the terminal rule in the filter
chain (not `permitAll`), `ownerId` removed from `OpenAccountRequest`, ownership
checks in the handler, and an E2E test per endpoint asserting 403 when the token
belongs to someone else.

Until slice 5 lands this is knowingly violated — `ownerId` is a request field
today. That is the retrofit, and the test that proves it is what closes it.

**NFR-12 — No administrative surface is reachable.** Operational endpoints
(Spring Actuator, and anything like it added later) expose configuration, heap
and threads — `/actuator/env` prints the datasource password, `/actuator/heapdump`
hands over a file containing every secret in memory. Enforced by: explicit
`management.endpoints.web.exposure.include` whitelist (`health,info` only; never
`*`), `management.server.port` separate from the API port so an edge rule can
drop it, and Spring Security covering the management context. Actuator is not a
dependency yet — this becomes live the moment observability work starts.

**NFR-13 — Least-privilege database credentials.** The application runtime must
not be able to change the schema. Two roles: a migration role owning the tables
and running Flyway (DDL), and an application role with `SELECT/INSERT/UPDATE` on
those tables and no `DROP`, no `TRUNCATE`, no ownership. Enforced by: a Flyway
migration that creates the app role and grants, `spring.flyway.user` /
`spring.flyway.password` distinct from `spring.datasource.*`.

Today both are `bankapp`, the database owner, so a SQL-injection or a bad
migration can drop the ledger. Cheap to fix and independent of every other
slice.

**NFR-14 — Rate limiting on credential and money endpoints.** `POST /api/auth/login`
and `/refresh` are credential-stuffing targets; `POST /api/payments/transfers` is
an abuse target. Limits are per-identity where a token exists and per-IP before
it does. Enforced by: a token-bucket filter backed by Redis (already in
`docker-compose` under the `cache` profile), responding **429** with a
`Retry-After` header, and an E2E test asserting the 429.

Per-IP is weak on its own — an attacker rotates addresses — so the login limiter
also counts failures per *username*, which is what actually blunts credential
stuffing.

## Planned (added with the slice that introduces them)

- Caching (Redis), observability/metrics (per docs/00 goals)
- Field-level encryption, if a PII column ever appears (docs/07)
