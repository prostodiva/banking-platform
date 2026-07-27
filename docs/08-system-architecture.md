# 08 System Architecture

## MVC (Model-View-Controller)

Model - Data
View - UI
Controller - Business logic

## Layered Architecture and Vertical Slices

UI - User Interface
Controller - handles HTTP requests
Service - business logic
Repository - data access
Model/Entity - Data

I will organize development by vertical slices(features) and will keep the horizontal layers inside of each feature slice.
DDD + Hexagonal Architecture (Ports and Adapters) inside each vertical slice

## DDD - Domain Driven Design

Let the business domain — not the database, framework, or UI — drive how code is
structured and named. Core ideas:

- **Ubiquitous language** — code uses the bank's vocabulary: if the bank says
  "freeze", the method is `freeze()`, not `setStatusInactive()`.
- **Bounded contexts** — the domain splits into contexts (Accounts, Payments,
  Fraud Detection...), each with its own model and vocabulary. One bounded
  context = one vertical slice in this project.
- **Aggregates** — consistency boundaries; `Account` owns its rules (refuses to
  withdraw below zero itself, instead of a service checking externally).
- **Value objects** — immutable concepts like `Money`, `AccountNumber`.
- **Repositories** — interfaces in the domain that fetch/store aggregates;
  the JPA implementation is an infrastructure detail.
- **Domain events** — facts like `PaymentCompleted` that other contexts react to.

Payoff: business logic lives in one testable place, isolated from infrastructure —
`domain/` packages have zero Spring/JPA imports.

## Backend structure (modular monolith, see ADR-001)

One Spring Boot app under base package `com.bankapp`. Each bounded context is a
top-level package with the horizontal layers inside it:

```
com/bankapp/
├── BankAppApplication.java
├── accounts/                        # one vertical slice = one bounded context
│   ├── api/                         # inbound adapters: controllers + request/response DTOs
│   ├── application/                 # use cases: one service class per use case
│   │   └── port/                    # interfaces the slice needs from outside
│   ├── domain/                      # aggregates, value objects, domain events,
│   │                                #   repository INTERFACES — no framework imports
│   └── infrastructure/              # outbound adapters: JPA entities + repository
│                                    #   implementations, Kafka producers
├── payments/                        # same 4-layer shape inside
├── auth/
├── frauddetection/                  # mostly event consumers
├── notifications/                   # Strategy pattern: email/SMS/push behind one interface
├── reports/                         # query-heavy: thin domain, rich queries (CQRS read side)
└── shared/                          # shared kernel — keep SMALL
    ├── domain/                      # Money, AccountNumber, DomainEvent base
    ├── web/                         # global exception handler, PingController
    └── config/                      # cross-cutting Spring config
```

Tests mirror the slices: `src/test/java/com/bankapp/accounts/...`

### Slice rules

1. Slices talk via events or application-service interfaces — never via each
   other's repositories or entities. Payments never imports `accounts.domain.Account`.
2. Dependency direction inside a slice: `api → application → domain ← infrastructure`.
   Domain has no framework *behavior*: no Spring imports, no Spring Data types.
   JPA mapping annotations on aggregates are tolerated as inert metadata (ADR-002).
3. Repository pattern: interface in `domain/`, JPA implementation in
   `infrastructure/persistence/`.
4. `shared/` is a shared kernel, not a dumping ground — value objects and
   cross-cutting config only.
5. Microservice extraction later = lift one top-level package into its own app.
   ArchUnit tests can enforce these boundaries.

## Frontend structure (mirrors the slices)

State/API layer: Redux Toolkit + RTK Query. RTK's `createSlice` maps 1:1 to a
vertical slice; RTK Query handles server calls (generated hooks, caching,
loading states).

```
src/
├── app/                    # composition root: App.tsx, store.ts, router.tsx, providers.tsx
├── features/               # vertical slices, mirror backend bounded contexts
│   ├── accounts/
│   │   ├── api.ts          # RTK Query endpoints (baseApi.injectEndpoints)
│   │   ├── slice.ts        # createSlice for feature-local client state
│   │   ├── types.ts        # request/response types for this feature
│   │   ├── components/     # feature-private components
│   │   └── index.ts        # public surface of the slice
│   ├── payments/
│   ├── auth/               # holds token/session state
│   ├── notifications/
│   └── reports/
├── shared/
│   ├── api/                # baseApi.ts: single createApi with fetchBaseQuery("/api")
│   ├── components/         # cross-feature UI kit
│   └── lib/
└── main.tsx
```

Same import rule as the backend: features import from `shared/` and from other
features' `index.ts` only.
