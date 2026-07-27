# 11 Development Conventions

Worked example with full explanations: [docs/13-slice-1-walkthrough.md](13-slice-1-walkthrough.md).

## Checklist: adding a use case to an existing slice

1. **Domain first** — add/extend the aggregate method that owns the rules
   (e.g. `Account.freeze()`); add a domain event to `domain/event/` if other
   contexts might care. Write the plain-JUnit test for the rules.
2. **Application** — new folder `application/<usecase>/` with
   `<UseCase>Command` (or a query), `<UseCase>Handler` (`@Service`,
   `@Transactional`, constructor injection, thin), and a result/view record if
   it returns data.
3. **API** — endpoint on the slice controller + request/response DTOs in
   `api/dto/` if the shapes are new. Map new domain exceptions in
   `shared/web/GlobalExceptionHandler` if needed.
4. **Migration** — only if the schema changes: next `V<n>__<description>.sql`.
   Never edit an applied migration.
5. **Tests** — domain rules in plain JUnit; one e2e happy path + the interesting
   failure paths in the slice's `*E2ETest`.

## Checklist: adding a brand-new slice (bounded context)

1. `V<n>__create_<context>_table.sql` migration.
2. `domain/` — aggregate (JPA-annotated, no setters, static factory, `@Version`),
   value objects, enums (`@Enumerated(STRING)`), repository **interface**,
   `event/` records.
3. `application/` — `port/` interfaces + one folder per use case.
4. `infrastructure/` — package-private Spring Data repository + adapter
   implementing the domain port; other adapters (events, messaging) as needed.
5. `api/` — one controller per aggregate, DTOs in `dto/`.
6. Tests mirroring the packages; reuse `TestcontainersConfiguration`.
7. `package-info.java` describing the bounded context.

## Naming

- Use case folders/classes: verb-first — `openaccount/OpenAccountHandler`,
  `freezeaccount/FreezeAccountCommand`.
- Domain events: past tense facts — `AccountOpened`, `AccountFrozen`.
- Aggregate methods: ubiquitous language — `freeze()`, not `setStatus(...)`.
- Migrations: `V<n>__<snake_case_description>.sql`.
- REST: plural resources (`/api/accounts`), state transitions as POST on a
  sub-resource (`POST /api/accounts/{id}/freeze`), 201 + Location on create.

## Layer rules (enforced by convention, later by ArchUnit)

- `api → application → domain ← infrastructure`; nothing imports `api`.
- Domain: JPA mapping annotations allowed, no framework *behavior*
  (no Spring imports, no Spring Data types) — see ADR-002.
- Cross-slice: reference other contexts **by id only**; communicate via events
  or application-service interfaces.
- DTOs never leave `api/`; entities never appear in controller signatures.

## Testing conventions

- Business rules: plain JUnit + AssertJ, no Spring context.
- End-to-end: `@SpringBootTest(RANDOM_PORT)` + `@Import(TestcontainersConfiguration.class)`
  + `@AutoConfigureRestTestClient` + `RestTestClient`. Real Postgres, never H2.
- `BigDecimal` assertions: `isEqualByComparingTo`, never `isEqualTo`.
