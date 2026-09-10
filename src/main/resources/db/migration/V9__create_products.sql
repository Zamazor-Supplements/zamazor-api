CREATE TABLE products
(
    id                UUID           NOT NULL,
    name              VARCHAR(255),
    description       TEXT,
    price             DECIMAL(10, 2) NOT NULL,
    image_url         TEXT           NOT NULL,
    image_public_id   VARCHAR(255)   NOT NULL,
    stock_quantity    INTEGER        NOT NULL,
    reserved_quantity INTEGER        NOT NULL,
    store_id          UUID           NOT NULL,
    category_id       UUID           NOT NULL,
    version           BIGINT,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    modified_at       TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_products PRIMARY KEY (id)
);