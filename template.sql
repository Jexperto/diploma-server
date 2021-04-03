CREATE TABLE IF NOT EXISTS Admins
(
    id   TEXT primary key,
    name TEXT
);

CREATE TABLE IF NOT EXISTS Rooms
(
    id       TEXT primary key,
    admin_id TEXT,
    code     TEXT    not null,
    state    INTEGER not null,
    foreign key (admin_id)
        references Admins (id)
        on delete cascade
);

CREATE TABLE IF NOT EXISTS Teams
(
    id      TEXT primary key,
    room_id TEXT not null,
    name    TEXT not null,
    foreign key (room_id)
        references Rooms (id)
        on delete cascade

);

CREATE TABLE IF NOT EXISTS Players
(
    id      TEXT primary key,
    team_id TEXT,
    name    TEXT not null,
    foreign key (team_id)
        references Teams (id)
        on delete cascade
);

CREATE TABLE IF NOT EXISTS Questions
(
    id           TEXT primary key,
    room_id      TEXT not null,
    question     TEXT not null,
    foreign key (room_id)
        references Rooms (id)
        on delete cascade
);

CREATE TABLE IF NOT EXISTS Right_Answers
(
    question_id TEXT not null,
    answer      TEXT not null,
    primary key (question_id),
    foreign key (question_id)
        references Questions (id)
        on delete cascade
);

CREATE TABLE IF NOT EXISTS Wrong_Answers
(
    question_id TEXT not null,
    answer      TEXT not null,
    player_id   TEXT not null,
    primary key (question_id, answer),
    foreign key (player_id)
        references Players (id)
        on delete cascade
);

CREATE TABLE IF NOT EXISTS TeamsToQuestions
(
    team_id     TEXT not null,
    question_id TEXT not null,
    result      BOOLEAN,
    primary key (team_id, question_id),
    foreign key (team_id)
        references Teams (id)
        on delete cascade,
    foreign key (question_id)
        references Questions (id)
        on delete cascade

);


select question_id, answer, false as valid
from Wrong_Answers,
     Questions
where Questions.room_id = :1
union
select question_id, answer, true
from Right_Answers,
     Questions
where Questions.room_id = :1;


select id, question
from Questions
where room_id == :1


select room_id from Teams inner join (select (team_id) from Players where id == ?);

-- select Questions.id as question_id, question, answer, valid from Questions inner join  (
--     select id, right_answer as answer, true as valid from Questions where room_id == :1
--     union
--     select question_id, answer, false from Wrong_Answers, Questions where question_id == Questions.id and  Questions.room_id = :1) SEL on Questions.id == SEL.id where room_id = :1;
