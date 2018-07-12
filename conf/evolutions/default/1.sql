# todos schema

# --- !Ups

CREATE TABLE todos (
    id SERIAL,
    text text NOT NULL,
    checked boolean NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE comments (
    id SERIAL,
    text text NOT NULL,
    todoId integer NOT NULL REFERENCES todos ON DELETE CASCADE,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE todos;
DROP TABLE comments;
