# 07 Authentication

Design notes for the auth slice (slice 5, docs/23) — token strategy, password
storage, and the **security boundary**: which protections this application
implements, which belong to the deployment, and which are deliberately out of
scope. Requirements go in docs/02 as FR-AUTH-01…04; this is the reasoning.

## Slice 5 scope

`POST /api/auth/register`, `/login`, `/refresh`. Spring Security filter chain,
JWT access token + refresh token, roles (CUSTOMER, ADMIN). Then the retrofit:
every endpoint built in slices 1–4 becomes authenticated, and `ownerId` stops
being a request field the client can claim and starts coming from the token.

## Password storage

Passwords are **hashed, never encrypted** — encryption implies recovering the
plaintext, which is exactly the property that must not exist. `BCrypt` via
`BCryptPasswordEncoder` (Argon2id is stronger but BCrypt ships with
`spring-security-crypto` and is uncontroversial at this scale).

- Per-password salt, stored inside the hash string — no separate column.
- Cost factor 10–12: deliberately slow, so an offline attacker gets few guesses
  per second.
- BCrypt truncates input past 72 bytes. Validate max length rather than letting
  it happen silently.
- Login must not reveal which half failed — "bad credentials", never "no such
  user", or the endpoint becomes a user-enumeration oracle.

## Tokens

Access token: JWT, short-lived (~15 min), stateless — no server-side session,
which is what lets a future extraction verify tokens without a shared store.
Refresh token: long-lived, stored server-side so it can be **revoked**; that
revocability is the entire reason it exists as a separate token.

A JWT is **signed, not encrypted** (JWS). The payload is base64 and readable by
anyone holding the token — so claims carry `sub`, roles and expiry, never
anything secret. No account numbers, no PII.

**Settled — signing algorithm: RS256**, verified against a public JWKS
(`/.well-known/jwks.json`). HS256 is symmetric, so anyone who can verify can
also mint, and docs/00 targets 5–8 services that all need to *validate* tokens —
each would hold forging power. Reasoning, key custody and refresh-token storage:
[ADR-004](adr/04.md) decisions 1–4.

## The security boundary

| Concern | Where it lives | Status |
|---|---|---|
| Password hashing | this app | slice 5 |
| Token issuing / verifying | this app | slice 5 |
| Authorization (roles, ownership checks) | this app | slice 5 |
| Security headers (HSTS, CSP), cookie flags | this app | slice 5 |
| **TLS termination** | the edge — LB / ingress / gateway | not this app |
| mTLS between services | service mesh | after extraction |
| Encryption at rest (TDE) | Postgres / infra | out of scope |
| Field-level encryption | this app, if PII appears | deferred, trigger below |
| Key custody (HSM/KMS) | external | out of scope |

**TLS is not a slice.** In production a Spring app almost never terminates TLS —
the load balancer holds the certificate, completes the handshake, and forwards
over a private network. The app's only involvement is trusting
`X-Forwarded-Proto` and emitting HSTS. There is no deployment here yet, so there
is no edge to configure; local dev is plain HTTP on `:8080` and that is a
deliberate choice, not an oversight.

What would be in scope the moment a deployment exists: `Secure` + `HttpOnly` +
`SameSite` on the refresh cookie (if it becomes a cookie rather than a response
field), HSTS, and `server.forward-headers-strategy=framework`.

**Encryption at rest is deferred**, because nothing stored is sensitive enough
to need it: no card numbers, no national IDs (docs/01 puts cards and
international transfers out of scope). Balances and account numbers are
protected by authorization, not by ciphertext. Switch trigger: the first PII
column. The mechanism then is a JPA `AttributeConverter` doing AES-GCM with an
envelope scheme — random data key per record, wrapped by a key-encryption key
from a KMS — never a hand-rolled cipher, and never `ECB` mode.

## Configuration failures — the other half

Everything above is about protecting a request that *reaches* the application.
The other half is **what is reachable at all, and how it is configured** — and
that is where real breaches overwhelmingly happen. Broken crypto is rare;
an open admin endpoint, a database with a public IP, or a service account that
can drop tables is common. A boundary table that discusses mTLS but not
`/actuator/env` is calibrated for the interview, not for the system.

| Failure mode | Status here | Lands in |
|---|---|---|
| Backend trusts client-supplied identity | **open** — `ownerId` is a request field, no ownership check on `{id}` | slice 5, NFR-11 |
| API with no authentication | **partly closed** — tokens are minted and verified, and `/api/auth/logout` is authenticated; slices 1–4 are still whitelisted in `SecurityConfig` | slice 5, NFR-11 |
| Admin surface exposed (`/actuator/**`) | not exposed — actuator isn't a dependency yet | slice 9, NFR-12 |
| Secrets in source or config | covered | NFR-08 |
| No rate limiting on login / payments | not built; Redis already in compose | slice 9, NFR-14 |
| Over-privileged service account | **open** — the app connects as the DB owner | slice 9, NFR-13 |
| Database reachable from the internet | no deployment exists yet | deployment, not this app |
| Internal services reachable from outside | one process today | slice 10 (extraction) |
| Misconfigured object storage | **not applicable** — no blobs, none planned (docs/01) | — |
| Input validation | `@Valid` on every request body; `Money` enforces scale | done, NFR-01/05 |

Two of those are open in the code right now, and they are worth naming precisely
because they sound like the same problem and are not.

**Client-supplied identity is authorization, not validation.** `@Valid` proves
`ownerId` is a well-formed UUID. It cannot prove it is *your* UUID. This is
IDOR — insecure direct object reference — and it is the most common serious API
flaw in the wild, because validation frameworks give a false sense of coverage.
The fix is not a stricter annotation; it is that after slice 5 the field stops
existing in the request and comes from the token instead.

**Over-privileged credentials are a blast-radius problem.** Splitting the Flyway
role from the runtime role does not stop an attack; it bounds one. If the
application can only `SELECT/INSERT/UPDATE`, then a SQL injection reads and
corrupts rows but cannot `DROP TABLE transfers`. Financial records are never
deleted (NFR-03) — that guarantee is currently enforced only by the code's good
manners, not by the database refusing.

**What is genuinely not this application's problem:** where the database listens,
what the security groups allow, and whether internal services have public
routes. Those are deployment topology. The correct engineering answer is to state
the assumption the app is built under — a private network, an authenticating
edge — and not to simulate it in `docker-compose`.

## Out of scope, and why

HSMs, PIN blocks, DUKPT, EMV cryptograms and key ceremonies are real banking
infrastructure and none of them are simulable in a portfolio project. A mock HSM
demonstrates less than an honest boundary statement does: it shows you didn't
know where the line was. The concepts are worth knowing cold for interviews —
deck docs/24 — but they stay out of the code.

Also out of **slice 5** specifically: OAuth2 as a provider (docs/00 lists it
aspirationally; slice 5 is first-party auth only) and MFA. Rate limiting is not
out of scope — it moved to slice 9 (NFR-14), because it needs Redis and belongs
with the other hardening work rather than inflating the auth slice.

## Related

docs/02 (FR-AUTH-01…04) · docs/03 NFR-08 (secrets), NFR-11…14 (boundary,
admin surface, least privilege, rate limiting) · [ADR-004](adr/04.md) (tokens,
key custody, privilege separation) · docs/12 (admin ownership checks) ·
docs/23 slices 5, 9, 10 · docs/18 section F (Spring Security cards) · docs/24
(cryptography cards)
