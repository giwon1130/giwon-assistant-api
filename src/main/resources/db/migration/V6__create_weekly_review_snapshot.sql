create table weekly_review_snapshot (
    id varchar(64) primary key,
    period_start timestamp with time zone not null,
    period_end timestamp with time zone not null,
    summary text not null,
    metrics text not null,
    wins text not null,
    risks text not null,
    next_focus text not null,
    generated_at timestamp with time zone not null
);
