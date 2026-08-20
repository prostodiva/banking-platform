# 12 Open Questions

Undecided design questions, parked here until an ADR settles them. A question
leaves this file when [docs/adr/](adr/) answers it.

_Idempotency of payment submission — closed by [ADR-003](adr/03.md)
(decisions 6–7) and recorded as NFR-09._

## Visibility of failed transfers

**Slice 4 (payments). Does not block coding — accepted consequence for now.**

[ADR-003](adr/03.md) decision 4 makes `Transfer` a record of what happened: a
rolled-back transfer leaves no row, so "show me my declined transfers" is
unanswerable. Fine while the caller is a human who sees the error response; not
fine once a support agent has to explain a customer's missing money, or a fraud
model wants to score refusals as signal.

The shape is known — an append-only attempt log written in its own transaction
(`REQUIRES_NEW`) so it survives the rollback — but it is not obvious whether it
belongs to `payments`, to a future audit context, or to observability rather
than the domain at all. Decide when the first requirement asks for it.

## Idempotency of deposit and withdraw

**Slice 3 (accounts). Not blocking — deliberately deferred.**

Transfers are idempotent; deposit and withdraw are not. Today that is
defensible: they are called by a human looking at a balance, who owns the
ambiguity. It stops being defensible the moment either is machine-called, or
payments is extracted and `moveMoney` becomes a remote call that can time out
mid-flight.

Already fixed: the key would go on an append-only `account_entries` table — rows
that *are* requests — never as a column on `accounts`, whose rows are entities
(ADR-003 decision 6). What is undecided is whether `account_entries` should
exist from the start, since it is also what a double-entry ledger would need,
and retrofitting it after balances have moved means backfilling history that was
never recorded.
