# 04 Domain Model

## Accounts context (slice 1)

**Account** — aggregate root.
- Identity: `id` (UUID, assigned at creation, not by the DB).
- State: `accountNumber` (unique, 10 digits), `ownerId` (UUID — reference to the
  auth context's user *by id only*, never by object), `type`
  (CHECKING | SAVINGS), `status` (ACTIVE | FROZEN | CLOSED), `balance` (Money),
  `version` (optimistic lock), `createdAt`.
- Invariants (enforced inside the aggregate):
  - created via `Account.open(...)` only → starts ACTIVE with zero balance
  - owner, type, account number are required and immutable
  - (next: only ACTIVE accounts can be frozen; only zero-balance accounts can close)
- Events: `AccountOpened` (→ later: `AccountFrozen`, `AccountClosed`).

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
                  │     unfreeze()
                  │ close()  [balance must be zero]
                  ▼
               CLOSED     (terminal — banks never delete history)
```
