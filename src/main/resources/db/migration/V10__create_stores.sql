CREATE TABLE stores
(
    id          UUID NOT NULL,
    name        VARCHAR(20),
    description TEXT,
    logo_url    TEXT,
    CONSTRAINT pk_stores PRIMARY KEY (id)
);