# 12 Open Questions

Undecided design questions, parked here until an ADR settles them. A question
leaves this file when [docs/adr/](adr/) answers it.

_Idempotency of payment submission — closed by [ADR-003](adr/03.md)
(decisions 6–7) and recorded as NFR-09._

## Replaying a simultaneous duplicate request

**Slice 4 (payments). Does not block — the safe half already holds.**

[ADR-003](adr/03.md) decision 6 wants the loser of a same-key race to replay the
original 201. It returns **409** instead: a constraint violation at flush marks
the transaction rollback-only, so the re-read the decision describes cannot run
inside the handler. The ADR carries the amendment.

Nothing unsafe follows from it — one transfer, one movement, one event, pinned by
`ConcurrentTransferE2ETest`. Only the status is wrong, and only for two requests
genuinely in flight at once; a client retrying after a timeout takes the fast path
and gets its 201.

The fix is a retry outside the transaction boundary: catch
`DataIntegrityViolationException` in a non-transactional caller, re-invoke, let
the fast path find the committed row. What is undecided is **where that caller
lives** — a wrapper bean around the handler, a `@Retryable`, or the controller —
and whether the same wrapper should also absorb `OptimisticLockingFailureException`,
which is likewise a retry the caller is currently asked to perform itself.

## Money on the wire: JSON number or string

**All slices. Not blocking — but it gets more expensive per endpoint added.**

`AccountResponse.balance` and `TransferResponse.amount` serialize as JSON
numbers. `BigDecimal` exists in this codebase precisely because binary floating
point is banned for money (NFR-01), and a JSON number lands in a JavaScript
client as a double — reintroducing the hazard at the boundary the domain guards
everywhere else. Most payment APIs send strings for this reason.

Changing it is one Jackson setting, but it changes every response and the
frontend that reads them, so it is a project-wide decision rather than a payments
one. Decide before the React client starts parsing balances.

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
