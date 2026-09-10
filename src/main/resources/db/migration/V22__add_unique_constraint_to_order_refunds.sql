ALTER TABLE order_refunds
    ADD CONSTRAINT uk_order_refund_stripe_id UNIQUE (stripe_refund_id);