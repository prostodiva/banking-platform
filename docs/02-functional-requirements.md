# 02 Functional Requirements

Format per use case (keep each entry under ~5 lines — design details belong in
docs/04/05/06): **ID — name**, actor, trigger, preconditions, postconditions,
error cases, covering tests. IDs are referenced by e2e tests: that closes the
requirement → test traceability loop. Write the entry _before_ coding the slice
(see the docs-first workflow in docs/13).

## Accounts context

**FR-ACC-01 — Open account**

- Actor: API client (authenticated customer once the auth slice exists)
- Trigger: `POST /api/accounts`
- Preconditions: owner (user) exists; type is CHECKING or SAVINGS; currency is a 3-letter ISO code
- Postconditions: account persisted with unique 10-digit account number, status ACTIVE,
  zero balance in the requested currency; `AccountOpened` published; response 201 + Location header
- Errors: 400 missing/invalid field (ownerId, type, currencyCode); 409 unknown owner
- Covered by: `OpenAccountE2ETest`, `AccountTest`

**FR-ACC-02 — View account**

- Actor: API client
- Trigger: `GET /api/accounts/{id}`
- Preconditions: none
- Postconditions: 200 with account representation (id, accountNumber, type, status, balance, currency)
- Errors: 404 unknown account id
- Covered by: `OpenAccountE2ETest`

**FR-ACC-03 - Freeze account**

- Actor: API client
- Trigger: `POST /api/accounts/{id}/freeze`
- Preconditions: AccountStatus.ACTIVE
- Postconditions: 200 with AccountStatus.FROZEN
- Errors: 404 unknown account id; 409 - freezing a non-active account
- Covered by: `FreezeAccountE2ETest`

**FR-ACC-04 - Unfreeze account**

- Actor: API client
- Trigger: `POST /api/accounts/{id}/unfreeze`
- Preconditions: AccountStatus.FROZEN
- Postconditions: 200 with AccountStatus.ACTIVE; AccountUnfrozen pushlished
- Errors: 404 unknown account id; 409 - unfreezing a non-frozen account
- Covered by: `UnfreezeAccountE2ETest`

**FR-ACC-05 - Close account**

- Actor: API client
- Trigger: `POST /api/accounts/{id}/close`
- Preconditions: AccountStatus.ACTIVE or FROZEN; balance is zero
- Postconditions: 200 with AccountStatus.CLOSED
- Errors: 404 unknown account id; 409 non-zero balance; 409 account already CLOSED
- Covered by: `CloseAccountE2ETest`


**FR-ACC-06 - Deposit**

- Actor: API client
- Trigger: `POST /api/accounts/{id}/deposit`
- Preconditions: AccountStatus.ACTIVE; amount is positive; the currency matches the account
- Postconditions: 200 with updated balance; MoneyDeposited published
- Errors: 404 unknown account id; 400 non-positive amount; 409 account is not active; 400 currency mismatch
- Covered by `DepositMoneyE2ETest`


**FR-ACC-07 - Withdraw**

- Actor: API client
- Trigger: `POST /api/accounts/{id}/withdraw`
- Preconditions: AccountStatus.ACTIVE; amount is positive; the currency matches the account
- Postconditions: 200 with updated balance; MoneyWithdrawn published
- Errors: 404 unknown account id; 400 non-positive amount; 409 account is not active; 409 insufficient funds;
400 currency mismatch
- Covered by `WithdrawMoneyE2ETest`


## Payments context

**FR-PAY-01 - Transfer money**

- Actor: API client
- Trigger: `POST /api/payments/transfers` with an `Idempotency-Key` header
- Preconditions: both accounts exist and are ACTIVE; they are different accounts; amount is positive and in both accounts' currency; `Idempotency-Key` header present
- Postconditions: 201 + Location, TransferResponse; both balances updated atomically; transfer persisted; `PaymentCompleted` published once. A retry with the same key replays the original 201 response, moves no money and publishes nothing
- Errors: 400 currency mismatch; 400 same account transfer; 400 missing an Idempotency-Key header; 400 non-positive amount; 404 unknown account; 409 concurrent modification; 409 insufficient funds; 409 non-active account; 422 same key, diff. body
- Covered by: `TransferMoneyE2ETest`, `TransferTest`

**FR-PAY-02 - View transfer**

- Actor: API client
- Trigger: `GET /api/payments/transfers/{id}`
- Preconditions: none
- Postconditions: 200 with TransferResponse (id, fromAccountId, toAccountId, amount, currencyCode, createdAt)
- Errors: 404 unknown transfer id
- Covered by: `TransferMoneyE2ETest`


## Auth Context

Token strategy, key custody and privilege separation: [ADR-004](adr/04.md).
`role` is never a request field — registration can only produce a CUSTOMER, and
the first ADMIN is bootstrapped from the environment (ADR-004 decision 5).

**FR-AUTH-01 — Register**

- Actor: anonymous API client
- Trigger: `POST /api/auth/register`
- Preconditions: email is well-formed and not already registered; password is 8–72 bytes
- Postconditions: user persisted with a BCrypt hash and role CUSTOMER; 201 + `AuthResponse`
  (access token, refresh token, expiry); refresh token persisted hashed; `UserRegistered` published
- Errors: 400 missing/invalid field (email, fullName, password); 409 email already registered
- Covered by: `RegisterUserE2ETest`, `UserTest`

**FR-AUTH-02 — Login**

- Actor: anonymous API client
- Trigger: `POST /api/auth/login`
- Preconditions: none — the credentials are the precondition
- Postconditions: 200 + `AuthResponse`; a new refresh token persisted hashed, existing ones
  left valid (one session per device); `UserLoggedIn` published
- Errors: 400 missing/malformed field; 401 bad credentials — **one message for both unknown
  email and wrong password**, and the same elapsed time, or the endpoint becomes a
  user-enumeration oracle (docs/07)
- Covered by: `LoginE2ETest`, `LoginHandlerTest`

**FR-AUTH-03 — Refresh**

- Actor: API client holding a refresh token
- Trigger: `POST /api/auth/refresh`
- Preconditions: refresh token exists, is unexpired and unrevoked
- Postconditions: 200 + `AuthResponse` with a **new** access *and* refresh token; the presented
  token is revoked (single-use rotation, ADR-004 decision 3); the access token's role is re-read
  from the user row, never carried over — a refresh token is opaque and has no claims to carry
- Errors: 400 missing token; 401 unknown, expired or already-revoked token — a revoked token
  presented again revokes every refresh token for that user (replay is indistinguishable from
  theft) and publishes `RefreshTokenReuseDetected`
- Covered by: `RefreshSessionE2ETest`, `RefreshSessionHandlerTest`

**FR-AUTH-04 — Logout**

- Actor: authenticated customer
- Trigger: `POST /api/auth/logout`
- Preconditions: valid access token; refresh token in the body
- Postconditions: 204; that refresh token revoked. The access token stays valid until it expires
  (≤15 min) — that is inherent to stateless JWTs, not a gap
- Errors: 401 missing/invalid access token; 400 missing `refreshToken`. An unknown or
  already-revoked refresh token is **204**, not 404 — logout is idempotent and must not report on
  tokens the caller may not own. A refresh token belonging to **another user** is also 204 and is
  left untouched: any other status would confirm that the token is real and owned by a real
  account, a distinction the caller had no way to make before asking
- Covered by: `LogoutE2ETest`

Admin user management (`POST /api/admin/users`, ADR-004 decision 5) has no FR
yet — the bootstrap admin covers slice 5. It gets FR-AUTH-05 when a second admin
is actually needed.

**Retrofit — not an FR.** Every endpoint in slices 1–4 becomes authenticated and
`ownerId` moves from the request body to the token `sub`. That is NFR-11, and it
changes FR-ACC-01: the 409 unknown-owner error becomes unreachable.

### FR-FRAUD-01 — Flag large payments and raise a fraud alert

- **Actor:** Customer (account owner)
- **Trigger:** A payment completes (PaymentCompleted event) whose amount
  exceeds the large-payment threshold.
- **Preconditions:** The account exists; the payment completed successfully;
  the large-payment threshold is configured.
- **Postconditions:** A fraud alert is recorded for the account owner,
  referencing the flagged payment (payment id, amount, timestamp). The
  payment itself is not blocked or reversed — this is detection, not
  prevention.
- **Errors:** If the event payload is missing required fields, the event is
  logged and skipped — no alert is created and processing of subsequent
  events continues.
- **Covered by:** FraudDetectionE2ETest

### FR-NOT-01 — Notify the sender when a fraud alert is raised

- **Actor:** System (fraud detection context, via event)
- **Trigger:** `FraudAlertRaised` consumed from the `fraud-alert-raised` topic (alertId, transferId, fromAccountId, amount, currencyCode, occurredAt)
- **Preconditions:** The alert was raised for a committed payment; the notifications consumer is subscribed to the topic
- **Postconditions:** Exactly one notification is recorded for this alert, addressed to the owner of `fromAccountId`; delivery is attempted through the configured channel strategy (logging adapter ships first — "sent" means the log entry was written without an exception). The payment and the alert are never affected by the outcome — notification is a side effect, not a participant
- **Error cases:** Duplicate delivery of the same event → no second notification (deduplication on `alertId`). Malformed event (missing required fields) → logged and skipped, no notification, subsequent events still processed. Channel failure → logged, delivery marked failed, never propagated — the listener still acks. Owner of `fromAccountId` cannot be resolved → logged and skipped
- **Covering tests:** NotificationsE2ETest

### FR-NOT-02 — List my notifications

- **Actor:** Authenticated customer
- **Trigger:** `GET /api/notifications` with a valid Bearer token
- **Preconditions:** The caller is authenticated; the JWT subject is the user's id
- **Postconditions:** The caller receives only their own notifications, newest first
- **Error cases:** No token or invalid token → 401. (Owner-only by construction: the list is keyed by the token's subject, so there is no way to request another user's notifications.) A user with no notifications → 200 with an empty list
- **Covering tests:** NotificationsE2ETest
