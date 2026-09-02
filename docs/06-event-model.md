# 06 Events

Domain events are **past-tense facts** (`AccountOpened`, not `OpenAccount`),
defined in the owning slice's `domain/events/` package and published by
application handlers through the `DomainEventPublisher` port — never directly
from controllers or entities.

Transport today: **in-process** (Spring `ApplicationEventPublisher` behind the
port). Handlers publish inside their transaction, but **the adapter holds the
event until that transaction commits** — a rolled-back change announces nothing,
so a plain `@EventListener` is safe and `@TransactionalEventListener(AFTER_COMMIT)`
is not required to get the guarantee.

That belongs in the adapter rather than in each consumer for two reasons: "one
committed change, one event" is a promise this file makes on the publisher's
behalf, and a consumer that forgets the annotation would otherwise silently break
it. It is also what a Kafka adapter needs (docs/09) — writing to a broker before
the database commits is the dual-write problem, and doing it here means Kafka
replaces the adapter without touching a handler.

Trade-off accepted: a consumer cannot join the publisher's transaction. Nothing
needs to, and for money the safer default is to say nothing until it is durable.

## Published events

**AccountOpened** — accounts context

- Fields: `accountId: UUID`, `ownerId: UUID`, `occurredAt: Instant`
- Published by: `OpenAccountHandler`, after `save`, inside the transaction
- Consumers: none yet (notifications slice planned: welcome message)
- Related: FR-ACC-01

**AccountFrozen** - accounts context

- Fields: `accountId: UUID`, `ownerId: UUID`, `occurredAt: Instant`
- Published by: `FreezeAccountHandler`
- Consumers: none yet (notifications slice planned)
- Related: FR-ACC-03

**AccountUnfrozen** - accounts context

- Fields: `accountId: UUID`, `ownerId: UUID`, `occurredAt: Instant`
- Published by: `UnfreezeAccountHandler`
- Consumers: none yet (notifications slice planned)
- Related: FR-ACC-04

**AccountClosed** - accounts context

- Fields: `accountId: UUID`, `ownerId: UUID`, `occurredAt: Instant`
- Published by: `CloseAccountHandler`
- Consumers: none yet (notifications slice planned)
- Related: FR-ACC-05

**MoneyDeposited** - accounts context

- Fields: `accountId: UUID`, `ownerId: UUID`, `amount: BigDecimal`, `currencyCode: String`, `occurredAt: Instant`
- Published by: `DepositMoneyHandler`
- Consumers: none yet (notifications slice planned)
- Related: FR-ACC-06

**MoneyWithdrawn** - accounts context

- Fields: `accountId: UUID`, `ownerId: UUID`, `amount: BigDecimal`, `currencyCode: String`, `occurredAt: Instant`
- Published by: `WithdrawMoneyHandler`
- Consumers: none yet (notifications slice planned)
- Related: FR-ACC-07

**PaymentCompleted** - payments context

- Fields: `transferId: UUID`, `fromAccountId: UUID`, `toAccountId: UUID`, `amount: BigDecimal`, `currencyCode: String`, `idempotencyKey: String`, `occurredAt: Instant`
- Published by: `TransferMoneyHandler`, after `save`, inside the transaction
- Published **once per committed transfer**: a replayed request returns the
  stored response and publishes nothing, or one client retry would become five
  payments downstream (ADR-003 decisions 5–6)
- The only event a transfer emits — `moveMoney` fires no `MoneyWithdrawn` /
  `MoneyDeposited`, so one transfer is one fact, not three
- Consumers: none yet — fraud detection planned (slice 6), which is when the
  in-process adapter is replaced by Kafka: payment → Kafka → fraud detection →
  notification (docs/00)
- Related: FR-PAY-01

## Auth

**UserRegistered** — auth context

- Fields: `userId: UUID`, `occurredAt: Instant`
- Published by: `RegisterUserHandler`, after `save`, inside the transaction
- Consumers: none yet (notifications slice planned: welcome message)
- Related: FR-AUTH-01

**UserLoggedIn** — auth context

- Fields: `userId: UUID`, `occurredAt: Instant`
- Published by: `LoginHandler`, after the tokens are issued, inside the transaction
- Consumers: none yet (fraud detection planned: login velocity;
  notifications planned: "new sign-in to your account")
- Related: FR-AUTH-02


**RefreshTokenReuseDetected** — auth context

- Fields: `userId: UUID`, `occurredAt: Instant`
- Published by: `RefreshSessionHandler`, only when an already-revoked token is
  presented. The happy path publishes nothing — a refresh is not a new fact.
- Consumers: none yet (fraud detection planned: this is the strongest signal the
  auth context emits)
- Related: FR-AUTH-03
