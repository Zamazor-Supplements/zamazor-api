CREATE TABLE categories
(
    id    UUID NOT NULL,
    label VARCHAR(255),
    CONSTRAINT pk_categories PRIMARY KEY (id)
);