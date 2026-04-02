alter table briefing_history
    add column source varchar(32) not null default 'MANUAL';
