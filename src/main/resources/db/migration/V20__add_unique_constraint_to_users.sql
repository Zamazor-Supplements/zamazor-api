ALTER TABLE users
    ADD CONSTRAINT uc_users_cart UNIQUE (cart_id);