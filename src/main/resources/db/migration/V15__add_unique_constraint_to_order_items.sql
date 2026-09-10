ALTER TABLE order_items
    ADD CONSTRAINT uc_a24b333cbfe56aa534c0ecee5 UNIQUE (order_id, product_id);