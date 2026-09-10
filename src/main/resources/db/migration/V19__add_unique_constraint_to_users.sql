ALTER TABLE users
    ADD CONSTRAINT uc_users_address UNIQUE (address_id);