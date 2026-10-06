# banking-platform

A small banking backend built as a **modular monolith**: accounts, money
transfers and JWT auth, with the module boundaries enforced by tests rather
than by convention. Spring Boot + Postgres on the back, React on the front.

This is a learning project. The design docs in [`docs/`](docs/) were written
before the code, and each feature was built as one vertical slice, in the order
laid out in the [slice roadmap](docs/23-slice-roadmap.md).

## Stack

| Layer    | Tech                                                              |
| -------- | ----------------------------------------------------------------- |
| Backend  | Java 21 · Spring Boot 4.1 · Spring Security (OAuth2 resource server) · JPA |
| Database | Postgres 17 · Flyway migrations                                   |
| Tests    | JUnit 5 · Testcontainers (real Postgres, never H2) · ArchUnit     |
| Frontend | React 19 · Vite 8 · TypeScript                                    |
| Infra    | Docker Compose — Postgres, plus Redis and Kafka behind profiles   |

## Status

| # | Slice | State |
| - | ----- | ----- |
| 1 | Open + view account | built |
| 2 | Freeze / unfreeze / close | built |
| 3 | Deposit / withdraw | built |
| 4 | Transfer between accounts | built |
| 5 | Auth — register, login, refresh, logout | built; not yet enforced on the account and payment endpoints |
| 6 | Fraud detection (Kafka consumer) | planned |
| 7 | Notifications | planned |
| 8 | Reports (CQRS read side) | planned |
| 9 | Hardening — rate limiting, least-privilege DB roles, headers | planned |
| 10 | Extraction to services | planned |

The frontend is still the Vite scaffold.

## Getting started

Needs Java 21, Docker and Node 22+.

```bash
# 1. Environment
cp .env.example .env
```

Fill in `POSTGRES_PASSWORD`, then generate a JWT signing key and paste the
output into `BANKAPP_JWT_PRIVATE_KEY`. There is no default for the key — the
app will not start without it.

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  | openssl pkcs8 -topk8 -nocrypt -outform DER \
  | base64 | tr -d '\n'
```

```bash
# 2. Postgres
docker compose up -d

# 3. Backend on :8080 — Flyway applies the migrations on startup
cd packages/backend/bank-app && ./mvnw spring-boot:run

# 4. Frontend on :5173, proxies /api to :8080
cd packages/frontend && npm install && npm run dev
```

Only infrastructure runs in Docker; both apps run natively for hot reload.
The backend reads the repo-root `.env` itself, so there is nothing to export.

Redis and Kafka are not used yet. When they are:

```bash
docker compose --profile cache up -d     # + Redis
docker compose --profile events up -d    # + Kafka
```

## Tests

```bash
cd packages/backend/bank-app
./mvnw test                                                        # needs Docker running
./mvnw test -Dtest=OpenAccountE2ETest#opensAccountAndReadsItBack   # a single test
```

Three kinds of test, per slice:

- **Domain** — plain JUnit against the aggregates, no Spring context.
- **Handler** — one use case with its ports faked.
- **E2E** — HTTP in, real Postgres underneath via Testcontainers. Every
  documented error status has a test asserting it.

Plus concurrency tests for transfers (optimistic locking, deadlock ordering)
and `ArchitectureTest`, which enforces the rules below.

## API

Errors are RFC 7807 `ProblemDetail` responses. Full contract:
[docs/05](docs/05-api-spec.md).

| Method | Path | |
| ------ | ---- | - |
| `POST` | `/api/auth/register` | Create a user |
| `POST` | `/api/auth/login` | Access token (15 min) + refresh token (14 days) |
| `POST` | `/api/auth/refresh` | Exchange a refresh token |
| `POST` | `/api/auth/logout` | Requires a valid access token |
| `POST` | `/api/accounts` | Open an account |
| `GET`  | `/api/accounts/{id}` | View an account |
| `POST` | `/api/accounts/{id}/freeze` · `/unfreeze` · `/close` | Account lifecycle |
| `POST` | `/api/accounts/{id}/deposit` · `/withdraw` | Move money in or out |
| `POST` | `/api/payments/transfers` | Transfer between two accounts (idempotent) |
| `GET`  | `/api/payments/transfers/{id}` | View a transfer |

## Architecture

One Maven module. Each bounded context is a top-level package with the same
four layers inside:

```
com.bankapp.<context>.{api, application, domain, infrastructure}

  accounts   auth   payments   frauddetection   notifications   reports   shared
```

`application/` holds one package per use case, verb first —
`openaccount/OpenAccountHandler`, `transfermoney/TransferMoneyHandler`.

Three rules fail the build if broken:

1. `domain` imports nothing from Spring or `jakarta.transaction`. JPA mapping
   annotations are the one allowed exception ([ADR-002](docs/adr/02.md)).
2. `domain` depends on no outer layer.
3. Contexts do not depend on each other, only on `shared`. Cross-context
   references are by id, and communication goes through published ports and
   events — `payments` moves money without importing `accounts.domain`.

The point of rule 3 is slice 10: if the seams are real, extracting a context
into its own service is a deployment change, not a rewrite.

## Docs

| Topic | Where |
| ----- | ----- |
| Overview · scope | [00](docs/00-overview.md) · [01](docs/01-scope.md) |
| Functional requirements · NFRs | [02](docs/02-functional-requirements.md) · [03](docs/03-nfr.md) |
| Domain model · API spec · event model | [04](docs/04-domain-model.md) · [05](docs/05-api-spec.md) · [06](docs/06-event-model.md) |
| Auth and the security boundary | [07](docs/07-authetication.md) |
| System architecture | [08](docs/08-system-architecture.md) |
| Development conventions | [11](docs/11-development-conventions.md) |
| A slice built end to end | [13](docs/13-slice-1-walkthrough.md) |
| Request flow diagrams | [19](docs/19-request-flow.md) |
| Slice order and why | [23](docs/23-slice-roadmap.md) |

Decisions:

- [ADR-001](docs/adr/01.md) — Modular monolith with vertical slices
- [ADR-002](docs/adr/02.md) — Flyway migrations, JPA on the aggregate, Testcontainers
- [ADR-003](docs/adr/03.md) — Transfers: cross-context calls, atomicity, idempotency
- [ADR-004](docs/adr/04.md) — Auth: token strategy, key custody, privilege separation
