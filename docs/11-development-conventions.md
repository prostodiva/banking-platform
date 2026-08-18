# 11 Development Conventions

Worked example with full explanations: [docs/13-slice-1-walkthrough.md](13-slice-1-walkthrough.md).

## Checklist: adding a use case to an existing slice

1. **Domain first**
   - add/extend the aggregate method that owns the rules
     (e.g. `Account.freeze()`);
   - add a domain event to `domain/events/` if other
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
   failure paths in the slice's `*E2ETest`. Steps: _Writing a domain unit test_
   below.

## Checklist: adding a brand-new slice (bounded context)

1. `V<n>__create_<context>_table.sql` migration.
2. `domain/` — aggregate (JPA-annotated, no setters, static factory, `@Version`),
   value objects, enums (`@Enumerated(STRING)`), repository **interface**,
   `events/` records.
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

## Layer rules (enforced by ArchitectureTest — docs/13 stage 6)

- `api → application → domain ← infrastructure`; nothing imports `api`.
- Domain: JPA mapping annotations allowed, no framework _behavior_
  (no Spring imports, no Spring Data types) — see ADR-002.
- Cross-slice: reference other contexts **by id only**; communicate via events
  or application-service interfaces.
- DTOs never leave `api/`; entities never appear in controller signatures.

## Testing conventions

- Business rules: plain JUnit + AssertJ, no Spring context.
- End-to-end: `@SpringBootTest(RANDOM_PORT)` + `@Import(TestcontainersConfiguration.class)`
  - `@AutoConfigureRestTestClient` + `RestTestClient`. Real Postgres, never H2.
- `BigDecimal` assertions: `isEqualByComparingTo`, never `isEqualTo`.

## Writing a domain unit test — 5 steps

Plain JUnit + AssertJ. No Spring, no database, no mocks — an aggregate is just
an object. Reference: `AccountTest`.

1. **List the rules before writing code.** The aggregate's invariants in
   [docs/04](04-domain-model.md) _are_ the test list: one test for the happy
   path, one per way the rule can refuse. `freeze()` → "only ACTIVE accounts
   can be frozen" = 2 tests (ACTIVE → FROZEN, already-FROZEN → rejected).
2. **Name the test after the rule** — verb-first, no `test` prefix:
   `freezeMakesActiveAccountFrozen`, `freezeRejectsAnAlreadyFrozenAccount`.
   The name alone should say which invariant broke when it goes red.
3. **Arrange — reach the state through the aggregate.** There are no setters,
   so the only route to FROZEN is `open()` then `freeze()`. If a state is
   unreachable, its test waits for the method that reaches it (no CLOSED test
   until `close()` exists). Unreachable-in-a-test = the encapsulation working.
4. **Act — one line.** The call under test, nothing else.
5. **Assert the state that changed, or the exception:**

```java
// success — assert the field the method is supposed to change
account.freeze();
assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);

// failure — the lambda/method reference is required: it defers the call so
// AssertJ can catch it. assertThatThrownBy(account.freeze()) throws first.
assertThatThrownBy(account::freeze)
    .isInstanceOf(IllegalStateException.class);
```

### Which exception to throw (and assert)

The type is not cosmetic: `GlobalExceptionHandler` maps it straight to an HTTP
status, so the wrong type in the domain ships the wrong status code. Pick the
one matching the response [docs/05](05-api-spec.md) promises.

| The problem is…                                    | Throw                                                              | Client gets |
| -------------------------------------------------- | ------------------------------------------------------------------ | ----------- |
| A bad **argument** — null, malformed, out of range | `IllegalArgumentException`                                         | 400         |
| Arguments fine, the **current state** forbids it   | `IllegalStateException`                                            | 409         |
| No aggregate with that **id**                      | `<Aggregate>NotFoundException` (extends `EntityNotFoundException`) | 404         |

Deciding between the first two — _did the caller send something wrong, or did
they ask for something wrong?_ Junk input is 400; a legal request the aggregate
won't perform right now is 409.

Two more the handler maps, thrown outside the domain so never asserted in a
domain test: `MethodArgumentNotValidException` (`@Valid` on a request DTO) →
400, and `DataIntegrityViolationException` (FK/unique violation from the DB) → 409. Those belong in the slice's `*E2ETest`.

## Writing a slice E2E test — 5 steps

Real HTTP against a real Postgres (Testcontainers). It proves the **wiring** —
routing, JSON shape, status codes, transaction, exception mapping — not the
business rules; those are already covered by the domain test, so don't repeat
them here. One file per use case: `accounts/e2e/<UseCase>E2ETest`. Reference:
`OpenAccountE2ETest`.

1. **Boilerplate** — same three annotations every time, plus the client:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class FreezeAccountE2ETest {

    @Autowired
    private RestTestClient client;
```

2. **Take the test list from [docs/05](05-api-spec.md).** That row already
   enumerates them: the success response, then one test per documented error.
   `POST /{id}/freeze` → 200, 404 unknown id, 409 non-active = 3 tests.
3. **Arrange over HTTP, never through a repository.** Need a frozen account?
   `POST /api/accounts/` then `POST /{id}/freeze`. Injecting the repository
   would bypass the very wiring under test. The only pre-existing state you may
   assume is the `V3` seed user (`11111111-…`), because a migration guarantees
   it.
4. **Act** — one exchange, the request under test.
5. **Assert status first, then body.** Status is the contract; a green body
   assertion on the wrong status proves nothing:

```java
client.post().uri("/api/accounts/" + id + "/freeze")
    .exchange()
    .expectStatus().isOk()
    .expectBody(AccountResponse.class)
    .returnResult();
```

- `.expectStatus().isNotFound()` / `.isBadRequest()` / `.isEqualTo(409)` for
  errors — no named method for 409.
- Deserialize to the real DTO (`AccountResponse.class`), not `String` — it
  catches field renames.
- `BigDecimal`: `isEqualByComparingTo`.

**Isolation:** all tests share one container and one schema, and nothing rolls
back — an E2E test writes rows that outlive it. So create the data each test
needs, use `UUID.randomUUID()` for "not found" cases, and never assert on row
counts or "the only account".

### Endpoints that take a request body

Money endpoints (`/deposit`, `/withdraw`, later `/transfers`) add three things
to the five steps above. Reference: `DepositMoneyE2ETest`.

**1. A request helper that stops at `.exchange()`.** Most tests on these
endpoints assert only a status, and an error response has a different JSON shape
than the success DTO — a helper that deserialized `AccountResponse` would fail
on every error case:

```java
private RestTestClient.ResponseSpec deposit(
    UUID accountId,
    String amount,
    String currencyCode
) {
    return client
        .post()
        .uri("/api/accounts/" + accountId + "/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of("amount", new BigDecimal(amount),
                   "currencyCode", currencyCode)
        )
        .exchange();
}
```

Each test picks up from there: `deposit(id, "100.00", "USD").expectStatus()…`.
Amount as a `String` parameter, `new BigDecimal(...)` inside — never
`BigDecimal.valueOf(100.00)`, which launders the value through a `double`.

**2. Two different 400s. Test one of each.** Same status, opposite ends of the
request:

| Body                                      | Rejected by                                     | Exception                        |
| ----------------------------------------- | ----------------------------------------------- | -------------------------------- |
| `amount: -50` or `amount: 0`              | `@Positive` on the request DTO, before the handler runs | `MethodArgumentNotValidException` |
| `currencyCode: "RUB"` into a USD account  | `Money.requireSameCurrency`, inside the aggregate | `IllegalArgumentException`       |

The second passes validation — `RUB` is three letters and a real ISO code — and
only fails once the domain does arithmetic with it. Delete `@Valid` from the
controller and the first case turns into a 500 while the second stays green;
that asymmetry is the whole reason to write both.

**3. After a refused money move, GET the account and assert the balance.** A 409
proves the request was rejected. It does not prove nothing was written — a
handler that debits and then throws also returns 409. Reading the balance back
is what makes it a money test:

```java
deposit(id, "100.00", "USD").expectStatus().isEqualTo(409);

AccountResponse after = client.get().uri("/api/accounts/" + id)
    .exchange()
    .expectStatus().isOk()
    .expectBody(AccountResponse.class)
    .returnResult().getResponseBody();

assertThat(after.balance()).isEqualByComparingTo(BigDecimal.ZERO);
```

#### The test list

Take it from the [docs/05](05-api-spec.md) row, one test per documented
response, plus one for the happy path:

| Test                        | Arrange                        | Expect            |
| --------------------------- | ------------------------------ | ----------------- |
| happy path                  | open                           | 200 + new balance |
| repeated call accumulates   | open, deposit                  | 200 + sum         |
| frozen account              | open, freeze                   | 409 + unchanged   |
| closed account              | open, close                    | 409               |
| non-positive amount         | open                           | 400               |
| mismatched currency         | open                           | 400               |
| unknown id                  | —                              | 404               |

Withdraw is this list minus "accumulates", plus the case only it has:
deposit 50, withdraw 100 → **409**, balance still 50.
