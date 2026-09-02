# 12 Open Questions

Undecided design questions, parked here until an ADR settles them. A question
leaves this file when [docs/adr/](adr/) answers it.

_Idempotency of payment submission — closed by [ADR-003](adr/03.md)
(decisions 6–7) and recorded as NFR-09._

_JWT signing algorithm, key custody, refresh-token storage — closed by
[ADR-004](adr/04.md) (decisions 1–4): RS256 with a published JWKS, private key
from the environment with no default, `kid` from the first commit, opaque
refresh tokens hashed at rest and rotated on use._

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

## Admin ownership checks on customer resources

**Slice 5 (auth). Does not block — the endpoints don't exist yet.**

ADR-004 decision 6 separates the surfaces: `/api/admin/**` is a different path
tree, not a bypass branch inside the customer handlers. What it does not settle
is **what an admin may actually read**. "Any account" is the easy answer and the
one that makes an insider breach unbounded; a support agent looking up a
customer they have no ticket for is the canonical abuse.

Options when the first admin endpoint is written: unrestricted plus an
append-only access log (cheap, detects after the fact), or a scoped grant — an
admin may read an account only while an open case references it (real
prevention, needs a case entity this project has no requirement for).

Deferred until an admin endpoint has a use case behind it. Recording it here so
that the surface separation isn't mistaken for having answered it.


## Refresh tokens accumulate without bound

**Slice 5 (auth). Does not block — a correctness question, not a safety one.**

Login mints a refresh token and revokes nothing, so a user who signs in daily for
a year holds 365 valid 14-day tokens, of which 351 are expired rows nobody
deletes. Options: a scheduled purge of `expires_at < now()`, a cap of N active
tokens per user evicting oldest-first, or nothing until the table is measurably a
problem. Deferred; it is cheap to add and expensive to guess at.

## Should a failed login publish an event?

**Slice 5 (auth). Deferred to the fraud slice.**

`UserLoggedIn` is the weaker signal — credential stuffing shows up as failures.
The obstacle is that a failure against an unknown address has no `userId` to put
in the event, and putting the attempted email in one writes an unregistered
address onto the bus. Options when fraud detection is real: a `LoginFailed`
carrying only a hash of the attempted address, or move the whole concern to a
structured audit log that is not an event at all.


## Rotation makes the refresh window slide forever

**Slice 5 (auth). Does not block — accepted for now.**

Each refresh issues a token with a fresh 14-day expiry, so a client that refreshes
daily is never logged out. That is the intended UX and it is also an unbounded
session: a token stolen from a device that keeps refreshing stays alive
indefinitely, because reuse detection only fires if the *victim* also refreshes.

The fix is an absolute session lifetime — a `session_started_at` carried across
rotations, past which no refresh is honoured regardless of the token's own expiry.
Deferred because it needs a column and a re-login story, and because 14 days of
sliding is a defensible product decision on its own.
