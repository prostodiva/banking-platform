# 04 Domain Model

## Accounts context (Slices 1-3)

**Account** — aggregate root.

- Identity: `id` (UUID, assigned at creation, not by the DB).
- State: `accountNumber` (unique, 10 digits), `ownerId` (UUID — reference to the
  auth context's user _by id only_, never by object), `type`
  (CHECKING | SAVINGS), `status` (ACTIVE | FROZEN | CLOSED), `balance` (Money),
  `version` (optimistic lock), `createdAt`.
- Invariants (enforced inside the aggregate):
  - created via `Account.open(...)` only → starts ACTIVE with zero balance
  - owner, type, account number are required and immutable
  - only ACTIVE accounts can be frozen;
  - only FROZEN accounts can be unfrozen
  - only ACTIVE or FROZEN accounts with zero balance can close
  - money moves only on ACTIVE accounts (deposit and withdraw)
  - the amount must be positive and in the account's currency
  - balance never goes negative — a withdrawal above the balance is refused
- Events: `AccountOpened`, `AccountFrozen`,`AccountUnfrozen`, `AccountClosed`, `MoneyDeposited`, `MoneyWithdrawn`.

**Money** — shared-kernel value object: `BigDecimal amount` + ISO `currencyCode`;
immutable; same-currency arithmetic only; max 4 decimal places.

## Users (owned by the future auth context)

`users` table exists (V1 migration) so accounts can hold a real FK, but the
model/endpoints belong to the auth slice when it's built. Other contexts refer
to users only by UUID.

## Status transitions (target)

```
        open()            freeze()
  (new) ──────► ACTIVE ◄──────────► FROZEN
                  │     unfreeze()       │
                  │                      │
                  │ close()              │ close()
                  ▼ [balance must be 0]  ▼ [balance must be 0]
               CLOSED ◄──────────────────┘
                     (terminal — banks never delete history)
```

## Payments context (Slice 4)

**Transfer** — aggregate root. A record of a movement that *already happened*,
not a workflow: no `status`, no PENDING/FAILED (ADR-003 decision 4).

- Identity: `id` (UUID, assigned at creation).
- State: `fromAccountId`, `toAccountId` (UUID — references into the accounts
  context _by id only_, no FK: the two tables would live in different databases
  after an extraction), `amount` (Money), `idempotencyKey` (String, client-chosen,
  unique), `createdAt` (Instant).
- Invariants (enforced inside the aggregate):
  - created via `Transfer.record(...)` only, and immutable afterwards — a
    committed transfer succeeded, a rolled-back one leaves no row
  - `fromAccountId` and `toAccountId` must differ
  - amount must be positive
  - `idempotencyKey` is required
- Events: `PaymentCompleted` (once per committed transfer; a replay publishes
  nothing).

`Transfer` deliberately does **not** restate the balance rules — ACTIVE-only,
matching currency, never negative. Those are `Account` invariants (above) and
stay in that one class; `payments` reaches them through the `AccountLedger` port
and holds no `Account` (ADR-003 decision 1). Safe retry is likewise not an
aggregate invariant: it is enforced by the unique index on `idempotencyKey` plus
the handler's replay path (NFR-09).
