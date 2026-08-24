create table transfers (
    id              uuid          primary key,
    from_account_id uuid          not null,
    to_account_id   uuid          not null,
    amount          numeric(19,4) not null,
    amount_currency varchar(3)    not null,
    idempotency_key varchar(64)   not null unique,
    created_at      timestamptz   not null,

    constraint chk_transfers_distinct_accounts
        check (from_account_id <> to_account_id)
);
