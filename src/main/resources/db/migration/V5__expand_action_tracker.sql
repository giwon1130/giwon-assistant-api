alter table copilot_action
    add column priority varchar(16) not null default 'MEDIUM';

alter table copilot_action
    add column due_date timestamp with time zone;
