create table if not exists daily_routine_check (
    id varchar(64) primary key,
    check_date date not null,
    item_key varchar(32) not null,
    completed boolean not null default false,
    note text,
    completed_at timestamp with time zone,
    updated_at timestamp with time zone not null default now()
);

create index if not exists idx_daily_routine_check_date
    on daily_routine_check (check_date);

create unique index if not exists uq_daily_routine_check_date_item
    on daily_routine_check (check_date, item_key);
