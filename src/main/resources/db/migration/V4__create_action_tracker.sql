create table copilot_action (
    id varchar(64) primary key,
    title text not null,
    source_question text not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    completed_at timestamp with time zone
);
