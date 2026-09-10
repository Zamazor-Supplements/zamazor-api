ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);