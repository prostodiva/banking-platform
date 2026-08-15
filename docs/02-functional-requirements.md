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
