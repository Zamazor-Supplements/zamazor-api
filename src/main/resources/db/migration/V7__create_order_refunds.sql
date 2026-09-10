CREATE TABLE order_refunds
(
    id                UUID                           NOT NULL,
    stripe_refund_id  VARCHAR(128)                   NOT NULL,
    order_id          UUID                           NOT NULL,
    amount            BIGINT                         NOT NULL,
    status            VARCHAR(32)                    NOT NULL,
    failure_reason    VARCHAR(512),
    stripe_created_at TIMESTAMP(6) WITHOUT TIME ZONE,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    version           BIGINT,
    CONSTRAINT pk_order_refunds PRIMARY KEY (id)
);