# 23 Slice Roadmap

Build order for the backend slices. Each one follows the same procedure — docs
first, then domain, application, api, tests: checklist in
[docs/11](11-development-conventions.md), worked example in
[docs/13](13-slice-1-walkthrough.md).

| #   | Slice                     | FRs             | Endpoints                                                    | New concept it teaches                                                                                                       |
| --- | ------------------------- | --------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| 1   | Open + view account       | FR-ACC-01/02    | `POST /api/accounts`, `GET /{id}`                            | The slice shape end to end                                                                                                     |
| 2   | Freeze / unfreeze / close | FR-ACC-03/04/05 | `POST /{id}/freeze`, `/unfreeze`, `/close`                   | State machine in the aggregate; multi-precondition invariant (status **and** zero balance)                                     |
| 3   | Deposit / withdraw        | FR-ACC-06/07    | `POST /{id}/deposit`, `/withdraw`                            | Money arithmetic, overdraft refusal, `@Version` optimistic locking under concurrent writes                                     |
| 4   | Transfer between accounts | FR-PAY-01       | `POST /api/payments/transfers`                               | Cross-slice boundaries (payments must not import `accounts.domain`), two aggregates in one transaction, idempotency keys       |
| 5   | Auth                      | FR-AUTH-01…04   | `POST /api/auth/register`, `/login`, `/refresh`              | Spring Security filter chain, JWT + refresh, RBAC; retrofitting a cross-cutting concern onto ~8 existing endpoints              |
| 6   | Fraud detection           | FR-FRD-01       | none (event consumer)                                        | Kafka, async consumers, eventual consistency, `@TransactionalEventListener(AFTER_COMMIT)`                                      |
| 7   | Notifications             | FR-NOT-01       | none (event consumer)                                        | Strategy pattern: email / SMS / push behind one interface                                                                      |
| 8   | Reports                   | FR-REP-01/02    | `GET /api/reports/…`                                         | CQRS read side: thin domain, query-heavy projections                                                                           |

Slices 1–4 are built; 5 onward are the plan.

## Why this order

- **Dependencies point one way.** accounts → payments → fraud / notifications /
  reports. A slice whose upstream data doesn't exist yet forces you to fake it,
  and the fake becomes the design.
- **Payments unblocks three slices; auth unblocks none.** Fraud, notifications
  and reports all need real money movement to react to.
- **Auth sits at 5, not 8.** It's cross-cutting, so it gets retrofitted onto
  every endpoint that exists when you build it — cheaper at 8 endpoints than at
  20, but not worth paying before the domain is interesting.
- **Each slice must teach something new.** A slice that only repeats
  load → decide → save → publish is cheap to add later and proves nothing.

## Picking the next slice (when this list runs out or changes)

1. Finish the current slice — every FR entry, API row, event entry, domain test
   and E2E test present.
2. Respect the dependency arrows.
3. Prefer the slice that unblocks the most downstream slices.
4. Prefer the slice with a new risk in it.
5. Defer cross-cutting concerns (caching, rate limiting, metrics) until enough
   surface exists for them to matter — but before the retrofit gets expensive.

## Definition of done (per slice)

- [docs/02](02-functional-requirements.md) FR entries,
  [docs/05](05-api-spec.md) API rows, [docs/06](06-event-model.md) event entries
  written **before** the code.
- Business rules covered by plain-JUnit domain tests.
- Every documented error has an E2E test asserting that exact status.
- `ArchitectureTest` passes untouched. Relaxing an ArchUnit rule to make a slice
  compile means the design is wrong, not the rule.

## What slice 4 actually cost

Kept as a calibration note, since it was the first slice to span two contexts.

- **The ADR paid for itself, and was still wrong in one place.** Writing
  [ADR-003](adr/03.md) before coding settled the port, the transaction and the
  idempotency key cleanly. But decision 6's replay-on-unique-violation flow turned
  out to be unimplementable — Hibernate marks a constraint violation
  rollback-only — and only writing the code revealed it. Design docs are worth
  writing first and worth amending after.
- **The ArchUnit rule needed changing before any code compiled.** Expected and
  recorded in advance; the point is that it was a deliberate amendment with the
  net rule set stricter, not a rule relaxed to make a slice build.
- **A copied class name broke the whole application.** Two `@Component`s named
  `SpringDomainEventPublisher` in different contexts collide, because Spring
  derives bean names from the simple class name. Parallel slice structure makes
  this recur — name per-context adapters accordingly.
- **Compiling and unit-green said nothing about the app starting.** That bean
  collision was invisible until an E2E test loaded the context.
