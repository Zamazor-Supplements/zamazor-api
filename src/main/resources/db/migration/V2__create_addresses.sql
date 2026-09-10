CREATE TABLE addresses
(
    id      UUID NOT NULL,
    country VARCHAR(255),
    city    VARCHAR(255),
    street  VARCHAR(255),
    phone   VARCHAR(255),
    CONSTRAINT pk_addresses PRIMARY KEY (id)
);