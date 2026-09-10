CREATE TABLE carts
(
    id      UUID NOT NULL,
    version BIGINT,
    CONSTRAINT pk_carts PRIMARY KEY (id)
);