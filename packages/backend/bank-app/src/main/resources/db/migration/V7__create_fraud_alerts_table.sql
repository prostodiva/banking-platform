create table fraud_alerts (
    id              uuid          primary key,
    transfer_id     uuid          not null unique,
    from_account_id uuid          not null,
    amount          numeric(19,4) not null,
    amount_currency varchar(3)    not null,
    reason          varchar(20)   not null,
    status          varchar(20)   not null,
    created_at      timestamptz   not null
);

create index idx_fraud_alerts_from_account_id on fraud_alerts (from_account_id);
