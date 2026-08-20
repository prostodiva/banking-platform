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
