CREATE TABLE users
(
    id         UUID         NOT NULL,
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    is_admin   BOOLEAN      NOT NULL,
    password   VARCHAR(255) NOT NULL,
    address_id UUID,
    cart_id    UUID,
    CONSTRAINT pk_users PRIMARY KEY (id)
);