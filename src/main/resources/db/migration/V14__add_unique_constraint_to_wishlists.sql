ALTER TABLE wishlists
    ADD CONSTRAINT uc_05106068475b2a75b7eafd106 UNIQUE (user_id, product_id);