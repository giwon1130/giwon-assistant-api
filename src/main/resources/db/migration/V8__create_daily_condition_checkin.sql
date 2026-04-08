create table if not exists daily_condition_checkin (
    check_date date primary key,
    energy integer not null,
    focus integer not null,
    mood integer not null,
    stress integer not null,
    sleep_quality integer not null,
    note text,
    updated_at timestamp with time zone not null default now()
);
