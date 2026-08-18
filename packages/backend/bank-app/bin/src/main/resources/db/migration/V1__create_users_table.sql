create table users (
    id         uuid         primary key,
    email      varchar(320) not null unique,
    full_name  varchar(200) not null,
    created_at timestamptz  not null default now()
);
