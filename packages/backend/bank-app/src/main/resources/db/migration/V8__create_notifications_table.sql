create table notifications (
    id uuid primary key,
    alert_id uuid not null unique,
    recipient_id uuid not null,
    account_id uuid not null,
    type varchar(20) not null,
    amount numeric(19,4) not null,
    amount_currency varchar(3) not null,
    status varchar(20) not null,
    created_at timestamptz not null
);

create index idx_notifications_recipient_id on notifications (recipient_id);
