# 06 Events

Domain events are **past-tense facts** (`AccountOpened`, not `OpenAccount`),
defined in the owning slice's `domain/event/` package and published by
application handlers through the `DomainEventPublisher` port — never directly
from controllers or entities.

Transport today: **in-process** (Spring `ApplicationEventPublisher` behind the
port, published inside the handler's transaction). When a consumer must only
react to _committed_ state, it should use
`@TransactionalEventListener(phase = AFTER_COMMIT)` rather than a plain
`@EventListener`. Kafka replaces the adapter later (docs/09) without touching
any handler — that is the point of the port.

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

## Planned events

- `PaymentCompleted` (payments) — consumers: fraud detection (via Kafka,
  the docs/00 flow: payment → Kafka → fraud detection → notification)
