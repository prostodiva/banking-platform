# 13 Slice 1 Walkthrough — Open Account (self-study)

A step-by-step guide to building the first complete vertical slice **yourself**:
`POST /api/accounts` → controller → handler → `Account` aggregate → repository →
PostgreSQL, plus tests. Type the code in, run each checkpoint, and debug against
the expected output before moving on. By the end you should be able to build
FreezeAccount (the exercise at the bottom) without help.

Every dependency name below was verified against Spring Boot **4.1.0** — several
tutorials online show Boot 3.x names that no longer work (noted where relevant).

---

## Story-first workflow — GitHub Issues (runs BEFORE the docs pass)

Stories are work-in-flight planning artifacts — they live in the issue tracker,
not in `docs/`. The durable residue of a finished story is the docs/02 FR entry
and the tests. Layering: **GitHub issue** (why, for whom, when it's done) →
**docs/02 FR** (durable contract, with ID) → **e2e test** (proof). Same fact,
three lifecycles, no duplication.

Per slice:

1. **Milestone per slice** (Issues → Milestones): `Slice 1 — Accounts: Open
   Account` (next one: `Slice 2 — Accounts: Freeze / Unfreeze / Close`). The
   milestone is the epic — no sprints, no story points; that would be cosplay
   for a solo project. Slice 1 started before this workflow existed — backfill
   it: create the milestone and issues now, tick acceptance criteria as the
   stage checkpoints pass.

   *Terminology:* "slice" here means a **work increment** — one thin
   end-to-end cut (story → docs → domain → api → tests). That's different
   from the architectural sense in docs/08, where a slice = a bounded-context
   package. Most work increments land *inside* an existing context: Slice 2
   adds use cases to `accounts`, it does not create a new package. The
   milestone name carries both: `Slice <n> — <Context>: <use cases>`.
2. **Story issue(s) first, before docs and code.** Slice 1's main story,
   written the way it should have been:

   > **Title:** Customer can open a bank account
   > **Labels:** `story`, `slice:accounts` · **Milestone:** Slice 1
   >
   > ```markdown
   > ## Story
   > As a customer, I want to open a checking or savings account
   > so that I can start keeping money at the bank.
   >
   > ## Acceptance criteria
   > - [ ] POST /api/accounts with owner, type, currency → 201 + Location
   > - [ ] New account starts ACTIVE with zero balance in requested currency
   > - [ ] Account number unique, 10 digits, not guessable
   > - [ ] Missing/invalid fields → 400 with problem details
   > - [ ] Unknown owner → 409
   > - [ ] AccountOpened event published
   >
   > ## Traceability
   > FR-ACC-01 (docs/02) · to be covered by OpenAccountE2ETest
   >
   > ## Tasks
   > - [ ] Flyway migrations
   > - [ ] Money VO + Account aggregate + domain tests
   > - [ ] Handler + ports + adapters
   > - [ ] Controller + DTOs + error handling
   > - [ ] E2E tests
   > ```

   The acceptance criteria are the same facts as the FR entry and the
   checkpoints — the story is where they get *invented*; docs/02 is where
   they become the contract; the test is where they become proof.

   Slice 1's second story (small ones don't need every section verbose):

   > **Title:** Customer can view an account
   > **Labels:** `story`, `slice:accounts` · **Milestone:** Slice 1
   >
   > ```markdown
   > ## Story
   > As a customer, I want to view an account's details
   > so that I can check its status and balance.
   >
   > ## Acceptance criteria
   > - [ ] GET /api/accounts/{id} → 200 with account details
   > - [ ] Unknown account id → 404
   >
   > ## Traceability
   > FR-ACC-02 (docs/02) · to be covered by OpenAccountE2ETest
   >
   > ## Tasks
   > - [ ] GetAccountHandler + AccountView
   > - [ ] GET endpoint on AccountController
   > - [ ] E2E test: happy path + 404
   > ```

   **Rule: every story produces exactly one FR** (occasionally a story splits
   into two if it bundles distinct behaviors — not the case here). The
   direction only goes one way: the story is written first, in business
   language; the FR is the same fact translated into the durable, testable
   contract in step 3. A story doesn't need to match an existing FR — it
   creates one.

   *(Slice 1 backfill note: both stories above describe code that was
   already built earlier in this tutorial. When creating these issues for a
   slice already in progress, tick the acceptance criteria as already-met
   rather than treating them as pending — the issue is closing a
   traceability gap, not queuing new work. Once you have the real issue
   number, update the draft PR body — `Closes #1, Closes #2` — and reference
   it in the relevant commit, same as `(#1)` below.)*
3. **Docs-first pass** (next section) — derive the FR entry from your own
   acceptance criteria: the story above became **FR-ACC-01** in docs/02, then
   docs/04 (Account invariants), docs/05 (the two endpoint rows), docs/06
   (AccountOpened).
4. **Branch per story.** Full command cycle (uncommitted work is safe: it
   travels onto the new branch with you):

   ```bash
   git switch -c feat/accounts-open-account   # create + switch in one step
   ```

   **Commit at green checkpoints** — every commit should be a state you'd
   happily check out later: it compiles, its tests pass. Run the stage's
   checkpoint first, then:

   ```bash
   git add packages/backend/bank-app
   git status                                  # always read what's staged
   git commit -m "feat(accounts): add Money VO, Account aggregate, domain tests (#1)"
   ```

   `(#1)` = issue number → GitHub links commit ↔ issue automatically.
   First push creates the remote branch and ties them together (`-u` = set
   upstream; after this, plain `git push` suffices):

   ```bash
   git push -u origin feat/accounts-open-account
   ```

   **Keeping a reference doc (like this file) current on `main` mid-slice.**
   Doc-only edits made while you're mid-branch (e.g. improving this very
   walkthrough) don't have to wait for the story's PR to merge — that's
   exactly *why* code and docs get committed separately (above): a docs-only
   commit touches no files the code commits touch, so it can be cherry-picked
   onto `main` immediately with zero conflict risk, independent of how far
   along the code is.

   ```bash
   git log --oneline -3                # find the docs commit's hash
   ```

   ```bash
   git switch main
   git pull
   git cherry-pick <docs-commit-hash>  # replays just that one commit onto main
   git push
   git switch feat/accounts-open-account   # back to where you left off
   ```

   Why it's safe: cherry-pick reapplies *only* that commit's diff — nothing
   else from the branch comes along. When the feature branch's PR eventually
   merges, `main` already has that exact content, so git treats it as a
   no-op for that file — no conflict, even though the same logical change
   now exists as two different commit SHAs (one on each branch). That's
   harmless; it's just slightly duplicated history if you go looking. If
   that bothers you across many stages, the alternative is to stop
   committing doc-only edits to the feature branch at all and make them
   directly on `main` in their own small sessions instead.

5. **Draft PR immediately, mark Ready at the end — even solo.** "Draft" is a
   PR state, not a commit type: visible, CI runs on it (once it exists), but
   unmergeable until you flip it to Ready.

   ```bash
   gh pr create --draft --title "Customer can open a bank account" --body "Closes #1"
   ```

   Then per stage: checkpoint green → commit → `git push` (the PR updates
   itself). When the final checkpoint passes:

   ```bash
   gh pr ready        # draft → ready for review
   ```

   Self-review the full diff in the PR view (you *will* find something), then:

   ```bash
   gh pr merge --squash --delete-branch
   ```

   `--squash` collapses the stage commits into one story-sized commit on main
   (use plain `gh pr merge` to keep every checkpoint commit instead — taste,
   but pick one style and stay consistent). Merge auto-closes the issue and
   ticks the milestone. Then sync up:

   ```bash
   git switch main && git pull
   ```

   *(Slice 1 backfill reality: the early stages were committed straight to
   main before this workflow existed — that history stays as it is. Branch
   from the current stage onward and PR the rest; slice 2 runs the full
   cycle from the start.)*
6. **Close the milestone when the slice ships** — for slice 1 that's after
   Checkpoint 5 (or 6 with ArchUnit) is green. Anything discovered mid-slice
   but out of scope (e.g. "AccountNumber should be a real VO",
   an idea already parked in this doc's exercises) becomes a new labeled
   issue — a parking lot, not scope creep.

Terminal equivalents: `gh issue create --milestone "Slice 1" --label story`,
`gh pr create`.

---

## Docs-first workflow — do this before coding ANY slice

Reference checklist for every slice (slice 2 onward: run it yourself, in order,
*before* opening the IDE). Each step is minutes, not hours — if an entry grows
past ~5 lines it's trying to be a design doc; the design belongs in 04/05/06.

1. **docs/02 functional requirements** — a few lines per use case:
   `ID / actor / trigger / preconditions / postconditions / error cases /
   covering tests`. This slice's entries are already there as the worked
   example — see **FR-ACC-01 Open account** and **FR-ACC-02 View account** in
   docs/02. The ID matters: e2e tests reference it — that closes the
   requirement → test traceability loop. (Slice 2 continues the numbering:
   FR-ACC-03 Freeze account.)
2. **docs/03 NFR** — written once, cross-cutting; NFR-01…08 are filled in
   (money exactness, optimistic locking, auditability, no auto-DDL, …), each
   naming the mechanism that enforces it. Touch per slice only when a new
   concern appears (payments will add idempotency; freeze adds nothing).
3. **docs/04 domain model** — new invariants, state transitions, events for the
   aggregate this slice touches.
4. **docs/05 api spec** — endpoint, request/response shape, status codes. One
   table row per endpoint is enough until OpenAPI generation exists.
5. **docs/06 event model** — new events: fields, publisher, expected consumers.
6. **ADR (docs/adr/)** — only if the slice forces a genuinely new architectural
   decision. Most slices don't; payments will (idempotency strategy).
7. **Code**, following the docs/11 checklists, docs open beside you.
8. **Loop back** — if implementation taught you the docs were wrong, fix the
   docs. A doc that disagrees with the code is worse than no doc.

(For slice 1, docs/02 and docs/03 were backfilled after the fact — the
acceptance criteria first lived implicitly in this tutorial's checkpoints:
"201 + Location", "400 on missing fields", "409 on unknown owner". Compare
FR-ACC-01 in docs/02 against Checkpoint 4 and you'll see they're the same
facts in two forms. For slice 2, write FR-ACC-03 *first* and derive your
checkpoints from it — that's the docs-first direction.)

---

## The map — read this first

The request flow you are about to build:

```
HTTP POST /api/accounts  {"ownerId":..., "type":"CHECKING", "currencyCode":"USD"}
   │
   ▼
api/AccountController          ← translates HTTP ↔ DTOs, nothing else
   │  builds OpenAccountCommand
   ▼
application/openaccount/OpenAccountHandler     ← the use case: orchestrates, owns the transaction
   │  Account.open(...)                        ← business rules live in the aggregate
   │  accounts.save(account)                   ← calls the PORT (interface in domain/)
   │  events.publish(AccountOpened)            ← calls the PORT (interface in application/port/)
   ▼
infrastructure/persistence/AccountRepositoryAdapter   ← implements the port
   │  delegates to Spring Data JPA
   ▼
PostgreSQL  (schema created by Flyway migrations, never by Hibernate)
```

Files you will create, in build order:

| Stage | Layer | Files |
|---|---|---|
| 1 | build/config | `pom.xml` (edit), `application.properties`, 3 SQL migrations |
| 2 | domain | `Money`, `Account`, `AccountType`, `AccountStatus`, `AccountNumber`, `AccountRepository`, `AccountOpened` + `AccountTest` |
| 3 | application + infrastructure | `OpenAccountCommand/Handler/Result`, `AccountView`, `GetAccountHandler`, `DomainEventPublisher` port, `AccountJpaRepository`, `AccountRepositoryAdapter`, `SpringDomainEventPublisher` |
| 4 | api | `OpenAccountRequest`, `AccountResponse`, `AccountController`, `GlobalExceptionHandler` |
| 5 | tests | `TestcontainersConfiguration`, `OpenAccountE2ETest`, update `BankAppApplicationTests` |

---

## Stage 0 — Prerequisites

Postgres must be running in Docker (apps run natively — see docs/10):

```bash
cp .env.example .env        # only if .env doesn't exist yet
docker compose up -d
docker compose ps           # wait until postgres shows (healthy)
```

**Checkpoint 0:** `docker exec -it bankapp-postgres psql -U bankapp -d bankapp -c '\conninfo'`
prints a connection info line. If auth fails, your `.env` password and the one
the volume was first created with differ — `docker compose down -v` resets it.

---

## Stage 1 — Dependencies, configuration, database schema

### 1a. `packages/backend/bank-app/pom.xml` — add dependencies

Spring Boot 4 renamed some starters (Boot 3 tutorials will mislead you here):

- `spring-boot-starter-web` → **`spring-boot-starter-webmvc`** (old name is deprecated — replace the one already in your pom)
- Flyway now needs **`spring-boot-starter-flyway`** (in Boot 3 you added `flyway-core` directly)
- Testcontainers is now **2.x**: the artifact is `testcontainers-postgresql` (1.x's `postgresql` artifact won't work with Boot 4)

Inside `<dependencies>`, replace the `spring-boot-starter-web` entry and add the rest:

```xml
<!-- replaces spring-boot-starter-web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- JPA/Hibernate: maps Java objects to tables -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Bean Validation: @NotNull etc. on request DTOs -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Flyway: versioned SQL migrations own the schema -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- JDBC driver, only needed at runtime -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- test: real Postgres in Docker during mvn test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>

<!-- test: RestTestClient (Boot 4's replacement for TestRestTemplate) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-resttestclient</artifactId>
    <scope>test</scope>
</dependency>
```

**Syntax notes**
- No `<version>` tags: the parent `spring-boot-starter-parent` is a BOM (bill of
  materials) that pins compatible versions for all of these. Only add a version
  when a dependency is *not* managed by the BOM.
- `<scope>runtime</scope>`: your code never imports the Postgres driver directly
  (you program against JDBC interfaces), so it's not needed at compile time.
- `<scope>test</scope>`: not packaged into the production jar.

### 1b. `src/main/resources/application.properties`

```properties
spring.application.name=bank-app

# --- datasource: reads env vars, falls back to local-dev defaults (.env.example) ---
spring.datasource.url=jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:bankapp}
spring.datasource.username=${POSTGRES_USER:bankapp}
spring.datasource.password=${POSTGRES_PASSWORD:local-dev-only-change-me}

# --- JPA: Flyway owns the schema; Hibernate only VERIFIES the mapping matches ---
spring.jpa.hibernate.ddl-auto=validate

# --- disable the anti-pattern where a DB session stays open during view rendering ---
spring.jpa.open-in-view=false
```

**Syntax notes**
- `${POSTGRES_PORT:5432}` = "use env var `POSTGRES_PORT`, default to `5432`".
  Dev defaults in properties are fine; production supplies real env vars.
- **Where does my `.env` password go? Nowhere in this file.** Spring Boot does
  NOT read `.env` — that file is read by *docker compose*, which sets the
  container's password on first initialization of the volume. Spring reads the
  *shell environment*. The two sides must simply agree:
  - If your `.env` keeps the example password, the fallback default here
    already matches — done.
  - If you changed it, load `.env` into your shell before starting the app:
    `set -a; source ../../../.env; set +a; ./mvnw spring-boot:run`
  - Never put a real password after the `:` in this file — it's committed to
    git. Only the throwaway fallback lives here; real values arrive via env.
  - Changed `.env` *after* Postgres first ran? The old password is baked into
    the volume — `docker compose down -v && docker compose up -d` resets it.
- `ddl-auto=validate` is the professional setting. `update`/`create` let Hibernate
  mutate your schema — never acceptable for a bank. If entity and table disagree,
  the app **fails at startup** with a clear error. That's a feature: you find out
  immediately, not at 3am.
- Flyway needs no properties: it auto-runs any `db/migration/V*.sql` on startup.

### 1c. Flyway migrations — `src/main/resources/db/migration/`

Naming is rigid: `V<number>__<description>.sql` — capital V, **two** underscores.
Flyway records each applied file in its `flyway_schema_history` table and never
runs it again; it also checksums applied files, so **never edit an applied
migration — add a new one**.

`V1__create_users_table.sql`:

```sql
create table users (
    id         uuid         primary key,
    email      varchar(320) not null unique,
    full_name  varchar(200) not null,
    created_at timestamptz  not null default now()
);
```

`V2__create_accounts_table.sql`:

```sql
create table accounts (
    id               uuid          primary key,
    account_number   varchar(12)   not null unique,
    owner_id         uuid          not null references users (id),
    type             varchar(20)   not null,
    status           varchar(20)   not null,
    balance_amount   numeric(19,4) not null,
    balance_currency varchar(3)    not null,
    version          bigint        not null,
    created_at       timestamptz   not null
);

create index idx_accounts_owner_id on accounts (owner_id);
```

`V3__seed_dev_user.sql`:

```sql
-- dev convenience: a known owner id for manual curl testing (see ADR-002)
insert into users (id, email, full_name)
values ('11111111-1111-1111-1111-111111111111', 'dev@bankapp.local', 'Dev User');
```

**Why these columns**
- `numeric(19,4)` for money — **never** `float`/`double` (binary floats can't
  represent 0.10 exactly; auditors will find the missing cents).
- `version bigint` — optimistic locking (Stage 2). Two concurrent updates to the
  same account: the second one fails instead of silently overwriting. Mandatory
  thinking for a bank from day one.
- `type`/`status` as `varchar` — matches `@Enumerated(EnumType.STRING)`. Storing
  enum *names* survives reordering the Java enum; storing ordinals doesn't.
- `balance_currency varchar(3)`, **not `char(3)`** — two reasons. Postgres
  `char(n)` blank-pads shorter values (`"US"` silently becomes `"US "`), and
  Hibernate maps a Java `String` to `VARCHAR`, so a `char(3)` column fails
  `ddl-auto=validate` with *"found [bpchar (Types#CHAR)], but expecting
  [varchar]"*. Length is guarded where it belongs: the `Money` constructor
  (stage 2a) rejects anything that isn't 3 characters. `length = 3` on the
  `@Column` documents the limit and caps DDL if it's ever generated.
- The FK `references users (id)` makes the DB itself reject accounts for
  nonexistent owners — defense in depth below the application layer.

### ✅ Checkpoint 1

```bash
cd packages/backend/bank-app && ./mvnw spring-boot:run
```

Expected in the log: Flyway lines like
`Migrating schema "public" to version "1 - create users table"` … `"3 - seed dev user"`,
then Tomcat on 8080. (Hibernate's `validate` passes trivially — no entities yet.)

```bash
docker exec -it bankapp-postgres psql -U bankapp -d bankapp -c '\dt'
```

Expected: `accounts`, `users`, `flyway_schema_history`. Stop the app (Ctrl-C).

---

## Stage 2 — Domain: where the business rules live

No Spring in this stage. Only JPA *mapping annotations* are allowed on the
aggregate (our ADR-002 tradeoff) — no framework *behavior*.

### 2a. `com/bankapp/shared/domain/Money.java` (shared kernel — every slice needs it)

```java
package com.bankapp.shared.domain;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public record Money(BigDecimal amount, String currencyCode) {

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (currencyCode == null || currencyCode.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code");
        }
        if (amount.scale() > 4) {
            throw new IllegalArgumentException("amount supports at most 4 decimal places");
        }
    }

    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, currencyCode);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currencyCode);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currencyCode);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                    "currency mismatch: " + currencyCode + " vs " + other.currencyCode);
        }
    }
}
```

**Syntax notes**
- `record` = immutable class: final fields, constructor, accessors (`amount()`,
  not `getAmount()`), `equals`/`hashCode` by value — exactly what a DDD *value
  object* needs. Two `Money(10, "USD")` are equal; two `Account`s never are
  (entities have identity, value objects have only values).
- `public Money { ... }` is a **compact constructor** — validation runs on every
  construction. An invalid `Money` cannot exist. That's the whole trick of DDD
  value objects: make illegal states unrepresentable.
- `@Embeddable`: JPA will inline these fields into the owning entity's table
  (no separate `money` table). Hibernate 7 (in Boot 4) supports records here.
- Operations return **new** instances (`add`, `subtract`) — immutability again.

### 2b. `com/bankapp/accounts/domain/AccountType.java` and `AccountStatus.java`

```java
package com.bankapp.accounts.domain;

public enum AccountType {
    CHECKING,
    SAVINGS
}
```

```java
package com.bankapp.accounts.domain;

public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
```

### 2c. `com/bankapp/accounts/domain/AccountNumber.java`

```java
package com.bankapp.accounts.domain;

import java.security.SecureRandom;

public final class AccountNumber {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LENGTH = 10;

    private AccountNumber() {
    }

    public static String generate() {
        StringBuilder digits = new StringBuilder(LENGTH);
        digits.append(1 + RANDOM.nextInt(9)); // no leading zero
        for (int i = 1; i < LENGTH; i++) {
            digits.append(RANDOM.nextInt(10));
        }
        return digits.toString();
    }
}
```

`SecureRandom`, not `Random` — account numbers must not be guessable from
previous ones. Uniqueness is enforced elsewhere (DB unique constraint + a check
in the handler); randomness alone is not a uniqueness guarantee.

### 2d. `com/bankapp/accounts/domain/Account.java` — the aggregate root

```java
package com.bankapp.accounts.domain;

import com.bankapp.shared.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, updatable = false)
    private String accountNumber;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "balance_amount", nullable = false))
    @AttributeOverride(name = "currencyCode", column = @Column(name = "balance_currency", nullable = false))
    private Money balance;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
        // required by JPA; protected so application code can't create blank accounts
    }

    private Account(UUID ownerId, AccountType type, String currencyCode, String accountNumber) {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber is required");
        }
        this.id = UUID.randomUUID();
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.type = type;
        this.status = AccountStatus.ACTIVE;
        this.balance = Money.zero(currencyCode);
        this.createdAt = Instant.now();
    }

    public static Account open(UUID ownerId, AccountType type, String currencyCode, String accountNumber) {
        return new Account(ownerId, type, currencyCode, accountNumber);
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public AccountType getType() {
        return type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Money getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
```

**Syntax notes**
- `@Entity` + `@Table(name = "accounts")`: Hibernate maps this class to the
  `accounts` table Flyway created. At startup, `ddl-auto=validate` cross-checks
  every `@Column` against the real table — your first checkpoint for this stage.
- `@Id` with **no** `@GeneratedValue`: the aggregate assigns its own UUID in the
  constructor. DDD style — identity exists the moment the object is born, no DB
  round-trip needed, and events can reference the id before the insert happens.
- `@Enumerated(EnumType.STRING)` — stores `"CHECKING"`, not `0`. The default
  (ordinal) breaks the moment someone reorders the enum.
- `@Embedded` + `@AttributeOverride`: pulls `Money`'s fields into this table and
  renames them to the `balance_*` columns. (Repeating `@AttributeOverride` twice
  is legal — it's a repeatable annotation.)
- `@Version`: Hibernate adds `WHERE version = ?` to every UPDATE and increments
  it. If another transaction updated the row in between, 0 rows match →
  `OptimisticLockingFailureException` instead of a silently lost update.
- **No setters.** State changes will be named business methods (`freeze()`,
  `deposit(...)`) — that's ubiquitous language. The private constructor + static
  factory `open(...)` means every `Account` in existence went through the rules.
- Why `protected Account()`: Hibernate instantiates entities reflectively and
  then sets fields; it needs a no-arg constructor but *your* code shouldn't call it.

### 2e. `com/bankapp/accounts/domain/AccountRepository.java` — the port

```java
package com.bankapp.accounts.domain;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    boolean existsByAccountNumber(String accountNumber);
}
```

The interface lives in **domain**, the implementation in **infrastructure**
(Stage 3). The domain declares what persistence it *needs*; it doesn't know
Postgres exists. This is the Repository pattern + Dependency Inversion (the D
in SOLID) in one file.

### 2f. `com/bankapp/accounts/domain/event/AccountOpened.java`

```java
package com.bankapp.accounts.domain.event;

import java.time.Instant;
import java.util.UUID;

public record AccountOpened(UUID accountId, UUID ownerId, Instant occurredAt) {
}
```

A domain event is a *fact*, named in past tense. Nothing consumes it yet — the
notifications slice will subscribe later without the accounts slice changing.

### 2g. First test — `src/test/java/com/bankapp/accounts/domain/AccountTest.java`

```java
package com.bankapp.accounts.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void openCreatesActiveAccountWithZeroBalance() {
        Account account = Account.open(OWNER, AccountType.CHECKING, "USD", "1234567890");

        assertThat(account.getId()).isNotNull();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getBalance().currencyCode()).isEqualTo("USD");
        assertThat(account.getOwnerId()).isEqualTo(OWNER);
    }

    @Test
    void openRequiresAnOwner() {
        assertThatThrownBy(() -> Account.open(null, AccountType.CHECKING, "USD", "1234567890"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openRejectsInvalidCurrency() {
        assertThatThrownBy(() -> Account.open(OWNER, AccountType.SAVINGS, "DOLLARS", "1234567890"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

**Syntax notes**
- No Spring annotations, no `@SpringBootTest` — this runs in milliseconds. Most
  of your tests should look like this; it's the payoff for keeping rules in the
  domain.
- AssertJ (`assertThat`) ships inside `spring-boot-starter-test`.
- `isEqualByComparingTo` for `BigDecimal`, not `isEqualTo`: `0` and `0.00` are
  `compareTo`-equal but not `equals`-equal. Classic BigDecimal trap.

### ✅ Checkpoint 2

```bash
./mvnw test -Dtest=AccountTest
```

Expected: `Tests run: 3, Failures: 0`. (`-Dtest=` runs only this class — the
full suite would also boot the Spring context, which we rewire in Stage 5.)

---

## Stage 3 — Application layer (use cases) + infrastructure (adapters)

### 3a. `com/bankapp/accounts/application/port/DomainEventPublisher.java`

```java
package com.bankapp.accounts.application.port;

public interface DomainEventPublisher {

    void publish(Object event);
}
```

### 3b. `com/bankapp/accounts/application/openaccount/` — one folder per use case

`OpenAccountCommand.java`:

```java
package com.bankapp.accounts.application.openaccount;

import com.bankapp.accounts.domain.AccountType;

import java.util.UUID;

public record OpenAccountCommand(UUID ownerId, AccountType type, String currencyCode) {
}
```

`OpenAccountResult.java`:

```java
package com.bankapp.accounts.application.openaccount;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountStatus;
import com.bankapp.accounts.domain.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenAccountResult(
        UUID id,
        String accountNumber,
        AccountType type,
        AccountStatus status,
        BigDecimal balance,
        String currencyCode) {

    public static OpenAccountResult from(Account account) {
        return new OpenAccountResult(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getStatus(),
                account.getBalance().amount(),
                account.getBalance().currencyCode());
    }
}
```

`OpenAccountHandler.java`:

```java
package com.bankapp.accounts.application.openaccount;

import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountNumber;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.event.AccountOpened;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OpenAccountHandler {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;

    public OpenAccountHandler(AccountRepository accounts, DomainEventPublisher events) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public OpenAccountResult handle(OpenAccountCommand command) {
        String accountNumber = uniqueAccountNumber();
        Account account = Account.open(
                command.ownerId(), command.type(), command.currencyCode(), accountNumber);

        accounts.save(account);
        events.publish(new AccountOpened(account.getId(), account.getOwnerId(), Instant.now()));

        return OpenAccountResult.from(account);
    }

    private String uniqueAccountNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = AccountNumber.generate();
            if (!accounts.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("could not generate a unique account number");
    }
}
```

**Syntax notes**
- `@Service` registers the class as a Spring bean (component scan finds it
  because it's under `com.bankapp`, the package of `BankAppApplication`).
- **Constructor injection with no `@Autowired`**: one constructor → Spring uses
  it automatically. Fields are `final` — the handler can't exist half-wired.
  This is the modern idiom; field-`@Autowired` is legacy style.
- Both dependencies are **interfaces you defined** (ports). The handler compiles
  without Spring Data, Postgres, or any event machinery — that's what makes it
  trivially unit-testable with Mockito later.
- `@Transactional` (the *Spring* one — `org.springframework.transaction.annotation`,
  not `jakarta.transaction`): the whole method is one DB transaction. Exception →
  rollback, including the save. The transaction boundary belongs on the use case,
  not on the controller (too wide) or the repository (too narrow).
- Note the handler contains **zero business rules** — it orchestrates. Rules are
  in `Account.open` and `Money`. If a handler starts growing `if`-logic about
  account state, that logic is trying to get into the aggregate.

### 3c. `com/bankapp/accounts/application/getaccount/` — the read side

`AccountView.java`:

```java
package com.bankapp.accounts.application.getaccount;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountStatus;
import com.bankapp.accounts.domain.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountView(
        UUID id,
        String accountNumber,
        UUID ownerId,
        AccountType type,
        AccountStatus status,
        BigDecimal balance,
        String currencyCode,
        Instant createdAt) {

    public static AccountView from(Account account) {
        return new AccountView(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerId(),
                account.getType(),
                account.getStatus(),
                account.getBalance().amount(),
                account.getBalance().currencyCode(),
                account.getCreatedAt());
    }
}
```

`GetAccountHandler.java`:

```java
package com.bankapp.accounts.application.getaccount;

import com.bankapp.accounts.domain.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class GetAccountHandler {

    private final AccountRepository accounts;

    public GetAccountHandler(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Optional<AccountView> handle(UUID accountId) {
        return accounts.findById(accountId).map(AccountView::from);
    }
}
```

Commands and queries as separate handlers = CQRS-lite. `readOnly = true` lets
Hibernate skip dirty-checking and tells the connection pool this transaction
won't write. Yes, `OpenAccountResult` and `AccountView` overlap — deliberate:
use cases evolve independently, so they don't share output types.

### 3d. `com/bankapp/accounts/infrastructure/persistence/`

`AccountJpaRepository.java`:

```java
package com.bankapp.accounts.infrastructure.persistence;

import com.bankapp.accounts.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AccountJpaRepository extends JpaRepository<Account, UUID> {

    boolean existsByAccountNumber(String accountNumber);
}
```

`AccountRepositoryAdapter.java`:

```java
package com.bankapp.accounts.infrastructure.persistence;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpa;

    AccountRepositoryAdapter(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Account save(Account account) {
        return jpa.save(account);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpa.existsByAccountNumber(accountNumber);
    }
}
```

**Syntax notes**
- `extends JpaRepository<Account, UUID>` — Spring Data generates the
  implementation at runtime: `save`, `findById`, paging, everything.
- `existsByAccountNumber` is a **derived query**: Spring Data parses the method
  name (`exists` + `By` + property `accountNumber`) and writes the SQL. Typo in
  the property name → startup error, not a runtime surprise.
- Both classes are **package-private** (no `public`) — nothing outside
  `infrastructure.persistence` can see Spring Data. Handlers can only reach
  persistence through the domain port. The compiler enforces your architecture.
- The adapter looks silly-thin today. It earns its keep the day you need a
  custom SQL query, caching, or to swap storage — signatures in domain stay put.

### 3e. `com/bankapp/accounts/infrastructure/events/SpringDomainEventPublisher.java`

```java
package com.bankapp.accounts.infrastructure.events;

import com.bankapp.accounts.application.port.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(Object event) {
        delegate.publishEvent(event);
    }
}
```

In-process events for now (any bean can `@EventListener` them later). When the
fraud-detection slice needs Kafka, you write a Kafka adapter for the same port —
`OpenAccountHandler` doesn't change. That is the entire point of ports.

### ✅ Checkpoint 3

```bash
./mvnw spring-boot:run
```

Expected: clean startup. This is a real test — Hibernate now **validates the
`Account` mapping against the `accounts` table**. A typo in a `@Column` name
fails right here with `Schema-validation: missing column [...]`. Fix and rerun.
Stop the app.

---

## Stage 4 — API layer: HTTP in, JSON out

### 4a. `com/bankapp/accounts/api/dto/OpenAccountRequest.java`

```java
package com.bankapp.accounts.api.dto;

import com.bankapp.accounts.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OpenAccountRequest(
        @NotNull UUID ownerId,
        @NotNull AccountType type,
        @NotBlank @Size(min = 3, max = 3) String currencyCode) {
}
```

### 4b. `com/bankapp/accounts/api/dto/AccountResponse.java`

```java
package com.bankapp.accounts.api.dto;

import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.openaccount.OpenAccountResult;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String type,
        String status,
        BigDecimal balance,
        String currencyCode) {

    public static AccountResponse from(OpenAccountResult result) {
        return new AccountResponse(
                result.id(),
                result.accountNumber(),
                result.type().name(),
                result.status().name(),
                result.balance(),
                result.currencyCode());
    }

    public static AccountResponse from(AccountView view) {
        return new AccountResponse(
                view.id(),
                view.accountNumber(),
                view.type().name(),
                view.status().name(),
                view.balance(),
                view.currencyCode());
    }
}
```

DTOs exist even when they look like the domain object: the JSON contract must be
able to stay stable while the domain refactors freely. Never serialize entities.

### 4c. `com/bankapp/accounts/api/AccountController.java`

```java
package com.bankapp.accounts.api;

import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.accounts.api.dto.OpenAccountRequest;
import com.bankapp.accounts.application.getaccount.GetAccountHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountCommand;
import com.bankapp.accounts.application.openaccount.OpenAccountHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final OpenAccountHandler openAccount;
    private final GetAccountHandler getAccount;

    public AccountController(OpenAccountHandler openAccount, GetAccountHandler getAccount) {
        this.openAccount = openAccount;
        this.getAccount = getAccount;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        OpenAccountResult result = openAccount.handle(
                new OpenAccountCommand(request.ownerId(), request.type(), request.currencyCode()));

        return ResponseEntity
                .created(URI.create("/api/accounts/" + result.id()))
                .body(AccountResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        return getAccount.handle(id)
                .map(view -> ResponseEntity.ok(AccountResponse.from(view)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

**Syntax notes**
- `@RestController` = `@Controller` + every return value serialized to JSON.
- `@Valid @RequestBody`: Jackson deserializes JSON → record, then Bean
  Validation runs the `@NotNull` rules. Failure → `MethodArgumentNotValidException`
  (handled globally in 4d) → the handler is never called.
- REST for creation: **201 Created** + `Location` header pointing at the new
  resource — that's `ResponseEntity.created(uri)`.
- The controller is ~15 lines of logic and knows nothing about persistence or
  rules. If a controller grows business logic, it's in the wrong layer.

### 4d. `com/bankapp/shared/web/GlobalExceptionHandler.java`

```java
package com.bankapp.shared.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onValidationFailure(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail(ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; ")));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail onIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Operation not allowed in current state");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onDataIntegrityViolation(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Data conflict");
        problem.setDetail("The request conflicts with existing data "
                + "(e.g. unknown owner or duplicate value).");
        return problem;
    }
}
```

**Syntax notes**
- `@RestControllerAdvice` = applies to **every** controller. Error handling is
  cross-cutting → lives in `shared/web/`, not in a slice.
- `ProblemDetail` is Spring's built-in RFC 9457 "problem+json" — the standard
  error shape (`{"type":..., "title":..., "status":..., "detail":...}`).
  No hand-rolled error DTO needed.
- Domain exceptions map naturally: `IllegalArgumentException` (bad input) → 400,
  `IllegalStateException` (valid request, wrong state — e.g. freezing a closed
  account later) → 409. The FK violation for an unknown owner surfaces as
  `DataIntegrityViolationException` → 409. Never let raw stack traces reach
  clients.

### ✅ Checkpoint 4 — the full round-trip

```bash
./mvnw spring-boot:run
```

```bash
curl -i -X POST localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerId":"11111111-1111-1111-1111-111111111111","type":"CHECKING","currencyCode":"USD"}'
```

Expected: `HTTP/1.1 201`, a `Location: /api/accounts/<uuid>` header, and a JSON
body with `"status":"ACTIVE"` and `"balance":0`. Then:

```bash
curl -i localhost:8080/api/accounts/<uuid-from-location>
```

Expected: `200` with the same account. Now the failure paths:

```bash
curl -i -X POST localhost:8080/api/accounts -H 'Content-Type: application/json' -d '{"type":"CHECKING"}'
```

Expected: `400` with a `ProblemDetail` JSON mentioning `ownerId` and `currencyCode`.

```bash
curl -i -X POST localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerId":"99999999-9999-9999-9999-999999999999","type":"CHECKING","currencyCode":"USD"}'
```

Expected: `409` (FK rejected the unknown owner). And in the database:

```bash
docker exec -it bankapp-postgres psql -U bankapp -d bankapp \
  -c 'select account_number, status, balance_amount, version from accounts;'
```

---

## Stage 5 — Tests against real Postgres (Testcontainers)

Manual curls prove it once; tests prove it forever. Testcontainers starts a
**throwaway Postgres container per test run** — tests never touch your dev
database and run identically in CI. Docker must be running.

### 5a. `src/test/java/com/bankapp/TestcontainersConfiguration.java`

```java
package com.bankapp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
```

**Syntax notes**
- `@ServiceConnection` is the magic: Boot inspects the container bean and
  **overrides `spring.datasource.*` automatically** — no URL/username/password
  wiring, no `@DynamicPropertySource` boilerplate from older tutorials.
- Import gotcha: Testcontainers 2.x moved the class to
  `org.testcontainers.postgresql.PostgreSQLContainer`. If your IDE offers
  `org.testcontainers.containers.PostgreSQLContainer`, that's the deprecated 1.x
  location.
- `postgres:17-alpine` matches docker-compose.yml — always test against the
  version you run.

### 5b. Update `src/test/java/com/bankapp/BankAppApplicationTests.java`

The context now needs a database, so point the existing smoke test at the
container:

```java
package com.bankapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BankAppApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

This little test now verifies a lot: context wires, Flyway migrations run
against a pristine Postgres, and Hibernate validates the schema.

### 5c. `src/test/java/com/bankapp/accounts/OpenAccountE2ETest.java`

Boot 4 note: `TestRestTemplate` is on its way out; the replacement is
**`RestTestClient`** (that's why `spring-boot-resttestclient` is in the pom).
It's not auto-configured — hence `@AutoConfigureRestTestClient`.

```java
package com.bankapp.accounts;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.api.dto.AccountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class OpenAccountE2ETest {

    private static final String DEV_USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private RestTestClient client;

    @Test
    void opensAccountAndReadsItBack() {
        AccountResponse created = client.post().uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ownerId", DEV_USER_ID, "type", "CHECKING", "currencyCode", "USD"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(AccountResponse.class)
                .returnResult().getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(created.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.accountNumber()).hasSize(10);

        client.get().uri("/api/accounts/" + created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponse.class)
                .isEqualTo(created);
    }

    @Test
    void rejectsRequestWithMissingFields() {
        client.post().uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "CHECKING"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsUnknownOwner() {
        client.post().uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ownerId", UUID.randomUUID().toString(),
                        "type", "CHECKING", "currencyCode", "USD"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void returns404ForUnknownAccount() {
        client.get().uri("/api/accounts/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }
}
```

**Syntax notes**
- `webEnvironment = RANDOM_PORT` boots real Tomcat on a free port — this is a
  genuine HTTP round-trip, JSON serialization and all, not a mock.
- Passing a `Map` as body → Jackson serializes it to JSON. (If your IDE doesn't
  offer `.body(...)`, look for `.bodyValue(...)` — the fluent API mirrors
  `RestClient`/`WebTestClient`.)
- The seeded dev user from `V3` exists here too — Flyway runs the same
  migrations in the test container. Tests and prod share one schema definition.
- First run downloads container images — slow once, fast after.

### ✅ Checkpoint 5 — the whole suite

```bash
./mvnw test
```

Expected: all tests green (`AccountTest`, `BankAppApplicationTests`,
`OpenAccountE2ETest`). Prove the isolation: stop the dev database with
`docker compose stop postgres` and run `./mvnw test` again — still green,
because tests bring their own Postgres. Then `docker compose start postgres`.

---

## Stage 6 (optional but recommended) — ArchUnit: the architecture as a test

ADR-002 keeps JPA annotations on the aggregate and promises in exchange that
the *real* rule — no framework behavior in domain, no cross-slice coupling —
is enforced mechanically. This stage delivers that promise: the layer rules
from docs/08 become a failing build instead of a code-review hope.

### 6a. `pom.xml` — one more test dependency

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.4.1</version>
    <scope>test</scope>
</dependency>
```

Note the explicit `<version>`: ArchUnit is **not** managed by the Spring Boot
BOM (compare with stage 1a, where the parent supplied every version). This is
what "unmanaged dependency" means in practice.

### 6b. `src/test/java/com/bankapp/ArchitectureTest.java`

```java
package com.bankapp;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.bankapp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_has_no_framework_behavior =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta.transaction..");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..api..", "..infrastructure..");

    @ArchTest
    static final ArchRule slices_do_not_depend_on_each_other =
            slices().matching("com.bankapp.(*)..")
                    .should().notDependOnEachOther()
                    .ignoreDependency(
                            DescribedPredicate.alwaysTrue(),
                            JavaClass.Predicates.resideInAnyPackage("com.bankapp.shared.."));
}
```

**Syntax notes**
- ArchUnit reads compiled **bytecode** — no Spring context, no database; these
  run as fast as `AccountTest`.
- `@ArchTest` fields replace `@Test` methods; snake_case names are the ArchUnit
  community idiom (they read like sentences in failure reports).
- Rule 1 is the ADR-002 boundary: `jakarta.persistence` annotations stay legal
  in `..domain..`, but any `org.springframework.*` dependency fails the build.
- Rule 2 is the Clean Architecture dependency rule — arrows point inward only.
- Rule 3: `(*)` captures each top-level package (accounts, payments, …) as a
  "slice" and forbids dependencies between them. The `ignoreDependency(...)`
  clause exempts the shared kernel — every slice may use `shared`, and that must
  not count as coupling.
- `DoNotIncludeTests` keeps test classes (like `TestcontainersConfiguration`)
  out of the analysis.

### ✅ Checkpoint 6 — and prove it actually guards

```bash
./mvnw test -Dtest=ArchitectureTest
```

Expected: 3 rules, all green. Now **make it fail on purpose** — temporarily add
to `Account.java`:

```java
import org.springframework.stereotype.Component;

@Component   // sabotage: framework behavior inside the domain
```

Re-run: rule 1 fails and the message names the class, the illegal dependency,
and the rule that was violated. Read that failure message carefully — it's what
a teammate would see in CI after an innocent-looking refactor. Revert the
sabotage, re-run green. From now on `./mvnw test` guards the architecture on
every build.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `FlywayValidateException: Migration checksum mismatch` | You edited an already-applied migration. Never do that — for a **dev** DB, `docker compose down -v && docker compose up -d` resets; from then on, add new `V<n>` files instead. |
| `Schema-validation: missing column` at startup | `@Column(name=...)` ↔ SQL column mismatch. The error names the column; fix whichever side is wrong (new migration if the SQL is wrong). |
| `Schema validation: wrong column type ... found [bpchar (Types#CHAR)], but expecting [varchar]` | SQL column is `char(n)` but Hibernate maps Java `String` → `VARCHAR`. Use `varchar(n)` in the migration. **`columnDefinition` does not help** — it only affects DDL *generation*, never `validate`, so it changes the error text without fixing anything. |
| `Connection refused` on startup | Postgres container not running (`docker compose ps`), or wrong port in `.env`. |
| `password authentication failed` | `.env` changed after the volume was created → `docker compose down -v`. |
| 400 on every POST | Missing `Content-Type: application/json` header in curl. |
| E2E tests: `Could not find a valid Docker environment` | Docker Desktop isn't running. |
| `No ConnectionDetailsFactory found for @ServiceConnection` | Wrong Testcontainers artifact — Boot 4 needs 2.x: `testcontainers-postgresql`, not 1.x `postgresql`. |

---

## Your turn — Slice exercise: FreezeAccount (no code provided, on purpose)

Everything you need already exists as a pattern above. Files to create:

1. `accounts/domain/Account.java` — add a `freeze()` method. Rules to enforce
   *inside the aggregate*: only an `ACTIVE` account can be frozen; freezing a
   `CLOSED` account is an `IllegalStateException` (→ your advice already maps it
   to 409). Add `AccountFrozen` to `domain/event/`.
2. `accounts/application/freezeaccount/FreezeAccountCommand.java` — record with
   the account id.
3. `accounts/application/freezeaccount/FreezeAccountHandler.java` — load via the
   repository port (`orElseThrow`), call `account.freeze()`, save, publish the
   event. `@Transactional`. Notice how thin it is.
4. `accounts/api/AccountController.java` — add
   `@PostMapping("/{id}/freeze")` → 200 with the updated account (or 404).
   A state *transition* is a POST to a sub-resource, not a PUT of the whole thing.
5. Tests: two plain-JUnit cases in `AccountTest` (freeze works on ACTIVE; freeze
   on CLOSED throws) and one e2e case (`POST /{id}/freeze` → 200 → GET shows
   `FROZEN`; freeze again → still fine or 409? — *you decide and document it*:
   idempotency is a real API design decision, see docs/00 goals).
6. No migration needed — ask yourself why. (Answer: the `status` column and its
   `FROZEN` value already exist; only behavior changes.)

Then UnfreezeAccount and CloseAccount are near-copies (CloseAccount rule: only
zero-balance accounts may close). After those three you have honestly earned the
"I build vertical slices without AI" line — and the interview story to match.

Quick-reference checklists for future slices live in
[docs/11-development-conventions.md](11-development-conventions.md).
