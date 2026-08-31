alter table users add column password_hash varchar(60);
alter table users add column role          varchar(20);
alter table users add column version       bigint;

update users
set password_hash = '$2y$12$b6/uozYcl26WBsx9.gDGYOYxqCcbf7CJxgbrHiX6s5TSc/QELR0EG',
    role          = 'CUSTOMER',
    version       = 0
where password_hash is null;

alter table users alter column password_hash set not null;
alter table users alter column role          set not null;
alter table users alter column version       set not null;

alter table users add constraint chk_users_role check (role in ('CUSTOMER', 'ADMIN'));
