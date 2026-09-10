CREATE TABLE stripe_webhook_events
(
    id             UUID         NOT NULL,
    event_id       VARCHAR(255),
    event_type     VARCHAR(255),
    order_id       UUID,
    outcome        VARCHAR(255) NOT NULL,
    failure_reason VARCHAR(255),
    received_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    version        BIGINT,
    processed_at   TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_stripe_webhook_events PRIMARY KEY (id)
);