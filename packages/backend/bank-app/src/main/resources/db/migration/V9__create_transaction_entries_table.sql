create table transaction_entries (
    id                      uuid          primary key,
    account_id              uuid          not null,
    type                    varchar(20)   not null,
    amount                  numeric(19,4) not null,
    amount_currency         varchar(3)    not null,
    counterparty_account_id uuid,
    source_id               uuid          not null,
    occurred_at             timestamptz   not null,

    constraint uq_transaction_entries_account_source
        unique (account_id, source_id)
);

create index idx_transaction_entries_account_occurred
    on transaction_entries (account_id, occurred_at);
