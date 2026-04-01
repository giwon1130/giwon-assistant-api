create table idea (
    id varchar(64) primary key,
    title varchar(255) not null,
    raw_text text not null,
    summary text not null,
    key_points text not null,
    suggested_actions text not null,
    tags text not null,
    status varchar(64) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table briefing_history (
    id varchar(64) primary key,
    generated_at timestamp with time zone not null,
    summary text not null,
    weather_location varchar(255) not null,
    weather_condition varchar(255) not null,
    weather_temperature_celsius integer not null,
    calendar_items text not null,
    headlines text not null,
    tasks text not null,
    focus_suggestion text not null
);
