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
