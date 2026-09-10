CREATE TABLE cart_items
(
    id         UUID NOT NULL,
    quantity   INTEGER,
    cart_id    UUID NOT NULL,
    product_id UUID NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (id)
);