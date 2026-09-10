CREATE TABLE wishlists
(
    id         UUID NOT NULL,
    user_id    UUID,
    product_id UUID,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_wishlists PRIMARY KEY (id)
);