CREATE TABLE order_items
(
    id                UUID           NOT NULL,
    product_id        UUID,
    product_name      VARCHAR(255)   NOT NULL,
    product_image_url TEXT,
    unit_price        DECIMAL(10, 2) NOT NULL,
    quantity          INTEGER        NOT NULL,
    order_id          UUID           NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id)
);