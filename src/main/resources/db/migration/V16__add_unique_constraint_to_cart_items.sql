ALTER TABLE cart_items
    ADD CONSTRAINT uc_e020537f6d07b2305eddbe2b3 UNIQUE (cart_id, product_id);