ALTER TABLE orders
    ADD CONSTRAINT uc_orders_stripe_checkout_session UNIQUE (stripe_checkout_session_id);