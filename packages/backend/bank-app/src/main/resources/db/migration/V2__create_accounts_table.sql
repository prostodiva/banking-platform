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
