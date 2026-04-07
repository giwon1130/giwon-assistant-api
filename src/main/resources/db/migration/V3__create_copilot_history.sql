create table copilot_history (
    id varchar(64) primary key,
    generated_at timestamp with time zone not null,
    question text not null,
    answer text not null,
    reasoning text not null,
    suggested_actions text not null,
    source varchar(32) not null
);
