# 05 API Specification

One table row per endpoint (enough until OpenAPI/springdoc generation is added).
Conventions, applying to every endpoint:

- Base path `/api`; JSON in/out.
- Errors are RFC 9457 `ProblemDetail` (`{"type","title","status","detail"}`) —
  produced by `GlobalExceptionHandler`, never raw stack traces.
- Creation returns **201 + Location header**; state transitions are `POST` on a
  sub-resource (`POST /api/accounts/{id}/freeze`), not PUT of the whole resource.
- IDs are UUIDs; unknown UUID → 404.

## Accounts

| Method | Path                          | Request body                                                           | Success                           | Errors                                                                   | FR        |
| ------ | ----------------------------- | ---------------------------------------------------------------------- | --------------------------------- | ------------------------------------------------------------------------ | --------- |
| POST   | `/api/accounts`               | `{ownerId: UUID, type: "CHECKING"\|"SAVINGS", currencyCode: ISO-4217}` | 201 + `Location`, AccountResponse | 400 invalid field; 409 unknown owner                                     | FR-ACC-01 |
| GET    | `/api/accounts/{id}`          | -                                                                      | 200, AccountResponse              | 404 unknown account                                                      | FR-ACC-02 |
| POST   | `/api/accounts/{id}/freeze`   | -                                                                      | 200, AccountResponse              | 404 unknown account id; 409 freezing a non-active account                | FR-ACC-03 |
| POST   | `/api/accounts/{id}/unfreeze` | -                                                                      | 200 AccountResponse               | 404 unknown account id; 409 - unfreezing a non-frozen account            | FR-ACC-04 |
| POST   | `/api/accounts/{id}/close`    | -                                                                      | 200, AccountResponse              | 404 unknown account id; 409 non-zero balance; 409 account already CLOSED | FR-ACC-05 |
| POST | `/api/accounts/{id}/deposit` | AmountRequest | 200, AccountResponse | 404 unknown account id; 400 non-positive amount; 409 account is not active; 400 currency mismatch | FR-ACC-06 |
| POST | `/api/accounts/{id}/withdraw` | AmountRequest | 200, AccountResponse | 404 unknown account id; 400 non-positive amount; 409 account is not active; 409 insufficient funds; 400 currency mismatch | FR-ACC-07 |

**AccountResponse** (shared by all account endpoints):

```json
{
  "id": "uuid",
  "accountNumber": "10 digits",
  "type": "CHECKING",
  "status": "ACTIVE",
  "balance": 0,
  "currencyCode": "USD"
}
```

**AmountRequest** (deposit and withdraw):

| Field          | Type                | Notes                                  |
| -------------- | ------------------- | -------------------------------------- |
| `amount`       | string (BigDecimal) | required, > 0, max 4 decimal places    |
| `currencyCode` | string              | required, ISO 4217, must match account |


## Payments

Both accounts travel in the **body**, not the path — a transfer is a resource of
its own, not a sub-resource of either account.

| Method | Path | Request body | Success | Errors | FR |
| ------ | ---- | ------------ | ------- | ------ | -- |
| POST | `/api/payments/transfers` | TransferRequest | 201 + `Location`, TransferResponse | 400 currency mismatch; 400 same account transfer; 400 missing an Idempotency-Key header; 400 non-positive amount; 404 unknown account; 409 concurrent modification; 409 insufficient funds; 409 non-active account; 422 same key, diff. body | FR-PAY-01 |
| GET | `/api/payments/transfers/{id}` | - | 200, TransferResponse | 404 unknown transfer | FR-PAY-02 |

**Required header** on `POST /api/payments/transfers`:

| Header            | Notes                                                                                                                                          |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `Idempotency-Key` | required, client-chosen, ≤ 64 chars; missing → 400. Same key replays the original 201 response; same key + different body → 422 (ADR-003) |

**TransferRequest**:

| Field           | Type                | Notes                                        |
| --------------- | ------------------- | -------------------------------------------- |
| `fromAccountId` | UUID                | required, must differ from `toAccountId`     |
| `toAccountId`   | UUID                | required                                     |
| `amount`        | string (BigDecimal) | required, > 0, max 4 decimal places          |
| `currencyCode`  | string              | required, ISO 4217, must match both accounts |

**TransferResponse** — `amount` serializes as a JSON **number**, matching
`AccountResponse.balance`. Money as a number is a precision hazard in a JS client
(it becomes a double); switching to strings is a project-wide call, not a
per-endpoint one — see [docs/12](12-open-questions.md).

```json
{
  "id": "uuid",
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 25.0000,
  "currencyCode": "USD",
  "createdAt": "2026-08-20T10:15:30Z"
}
```
